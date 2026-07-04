package com.pingoptimizer.pro.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class PingResult(
    val success: Boolean,
    val latencyMs: Long,
    val error: String? = null
)

/**
 * Measures round-trip latency using a TCP connect handshake instead of raw ICMP.
 *
 * Why: Android does not allow unprivileged (non-root) apps to open raw ICMP sockets.
 * A TCP-connect timing test is the standard, honest, non-root-friendly substitute -
 * it measures the same network path and gives a very close real-world approximation
 * of what your game's connection setup would experience.
 */
object PingUtils {

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

    /** Runs [samples] pings and returns (min, avg, max, jitter, packetLossPercent). */
    suspend fun runSamples(
        host: String,
        port: Int,
        samples: Int = 5
    ): PingStats {
        val results = mutableListOf<Long>()
        var failures = 0
        repeat(samples) {
            val r = tcpPing(host, port)
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
}

data class PingStats(
    val minMs: Long,
    val avgMs: Long,
    val maxMs: Long,
    val jitterMs: Long,
    val lossPercent: Int
)
