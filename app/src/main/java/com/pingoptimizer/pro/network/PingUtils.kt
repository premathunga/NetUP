package com.pingoptimizer.pro.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

data class PingResult(
    val success: Boolean,
    val latencyMs: Long,
    val error: String? = null
)

/**
 * All latency measurement lives here - ONE implementation, reused by every screen.
 * (Previously this logic was copy-pasted with `Runtime.exec` in three different
 * screens, and one screen even had a fully scripted/fake result. Both problems
 * are fixed by centralizing real logic here.)
 *
 * Two real strategies, tried in order:
 *
 * 1. [shellPing] - shells out to the OS's own `/system/bin/ping` (toybox/busybox).
 *    On stock Android this binary carries a file-level `CAP_NET_RAW` capability,
 *    so it can send genuine ICMP echo requests WITHOUT root. This is the same
 *    trick used by most reputable non-root network-diagnostic apps. It can fail
 *    on some locked-down OEM ROMs, so...
 * 2. [tcpPing] - a TCP-connect-timing fallback that always works (no special
 *    capability required) and gives a very close real-world proxy for latency.
 *
 * [smartPing] tries #1 first and transparently falls back to #2. Nothing here
 * ever fabricates a result - if both methods fail, that failure is reported
 * honestly instead of being replaced with a scripted "success" message.
 */
object PingUtils {

    /** Real ICMP ping via the OS ping binary. Returns null if the binary/output is unusable. */
    private suspend fun shellPing(host: String, timeoutSec: Int = 1): PingResult? =
        withContext(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("ping", "-c", "1", "-W", timeoutSec.toString(), host)
                    .redirectErrorStream(true)
                    .start()
                val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                val finished = process.waitFor(timeoutSec + 2L, TimeUnit.SECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    return@withContext null
                }
                val match = Regex("time[=<]([0-9.]+)").find(output)
                if (match != null) {
                    val ms = match.groupValues[1].toDouble().toLong()
                    PingResult(success = true, latencyMs = ms)
                } else if (output.contains("0 received") || output.contains("100% packet loss")) {
                    PingResult(success = false, latencyMs = -1, error = "No reply")
                } else {
                    null // unparseable output - let caller fall back to TCP method
                }
            } catch (e: Exception) {
                null // ping binary missing/blocked on this ROM - fall back
            }
        }

    /** TCP-connect timing fallback - always available, no special permission needed. */
    suspend fun tcpPing(host: String, port: Int = 443, timeoutMs: Int = 1500): PingResult =
        withContext(Dispatchers.IO) {
            val start = System.nanoTime()
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
                val elapsedMs = (System.nanoTime() - start) / 1_000_000
                PingResult(success = true, latencyMs = elapsedMs)
            } catch (e: Exception) {
                PingResult(success = false, latencyMs = -1, error = e.message ?: "Timeout")
            }
        }

    /** Tries real ICMP first, falls back to TCP-connect timing. Always a real measurement. */
    suspend fun smartPing(host: String, tcpPort: Int = 443): PingResult =
        shellPing(host) ?: tcpPing(host, tcpPort)

    /** Runs [samples] real pings and returns (min, avg, max, jitter, packetLossPercent). */
    suspend fun runSamples(host: String, port: Int = 443, samples: Int = 5): PingStats {
        val results = mutableListOf<Long>()
        var failures = 0
        repeat(samples) {
            val r = smartPing(host, port)
            if (r.success) results.add(r.latencyMs) else failures++
        }
        if (results.isEmpty()) {
            return PingStats(0, 0, 0, 0, 100)
        }
        val min = results.min()
        val max = results.max()
        val avg = results.average().toLong()
        val jitter = if (results.size > 1) {
            results.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().toLong()
        } else 0L
        val lossPercent = (failures * 100) / samples
        return PingStats(min, avg, max, jitter, lossPercent)
    }

    /**
     * Runs a real multi-target diagnostic and emits each line via [onLine] as it
     * completes - used to drive the "Advanced Diagnostics" terminal log honestly,
     * with genuine per-target results instead of a scripted animation.
     */
    suspend fun runDiagnostic(
        targets: List<Pair<String, String>>, // label to host
        onLine: suspend (String) -> Unit
    ): PingStats {
        onLine("> Starting real diagnostic (ICMP ping, TCP fallback)...")
        val allLatencies = mutableListOf<Long>()
        var totalFailures = 0
        var totalSent = 0
        for ((label, host) in targets) {
            onLine("> Pinging $label ($host)...")
            val stats = runSamples(host, 443, samples = 3)
            totalSent += 3
            if (stats.lossPercent == 100) {
                totalFailures += 3
                onLine(">   unreachable (timeout)")
            } else {
                allLatencies.add(stats.avgMs)
                onLine(">   avg ${stats.avgMs}ms . jitter ${stats.jitterMs}ms . loss ${stats.lossPercent}%")
            }
        }
        onLine("> Diagnostic complete.")
        if (allLatencies.isEmpty()) {
            onLine("> All targets unreachable - check your connection.")
            return PingStats(0, 0, 0, 0, 100)
        }
        val min = allLatencies.min()
        val max = allLatencies.max()
        val avg = allLatencies.average().toLong()
        val jitter = if (allLatencies.size > 1) {
            allLatencies.zipWithNext { a, b -> kotlin.math.abs(a - b) }.average().toLong()
        } else 0L
        val lossPercent = (totalFailures * 100) / totalSent
        onLine("> Overall: avg ${avg}ms, best ${min}ms, worst ${max}ms, loss $lossPercent%")
        return PingStats(min, avg, max, jitter, lossPercent)
    }
}

data class PingStats(
    val minMs: Long,
    val avgMs: Long,
    val maxMs: Long,
    val jitterMs: Long,
    val lossPercent: Int
)