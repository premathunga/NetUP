package com.pingoptimizer.pro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingoptimizer.pro.data.AppPrefs
import com.pingoptimizer.pro.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun NetworkOptimizerScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val scope = rememberCoroutineScope()

    val currentDnsPrimary by prefs.dnsPrimary.collectAsState(initial = "1.1.1.1")

    // Ping test simulation state
    var isTesting by remember { mutableStateOf(false) }
    var pingLog by remember { mutableStateOf(listOf<String>()) }
    val scrollState = rememberScrollState()

    // Live DNS Pings
    var pingCloudflare by remember { mutableIntStateOf(-1) }
    var pingGoogle by remember { mutableIntStateOf(-1) }
    var pingQuad9 by remember { mutableIntStateOf(-1) }

    // Live signal snapshot backing the radar stat boxes below (real data, not hardcoded)
    var wifiSnapshot by remember { mutableStateOf<com.pingoptimizer.pro.utils.WifiInfoSnapshot?>(null) }
    var cellularSnapshot by remember { mutableStateOf<com.pingoptimizer.pro.utils.CellularInfoSnapshot?>(null) }
    var lastJitterMs by remember { mutableIntStateOf(0) }
    var lastLossPercent by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        wifiSnapshot = com.pingoptimizer.pro.utils.NetworkSignalUtils.readWifi(context)
        cellularSnapshot = com.pingoptimizer.pro.utils.NetworkSignalUtils.readCellular(context)
        withContext(Dispatchers.IO) {
            launch {
                val r = com.pingoptimizer.pro.network.PingUtils.smartPing("1.1.1.1")
                pingCloudflare = if (r.success) r.latencyMs.toInt() else -1
            }
            launch {
                val r = com.pingoptimizer.pro.network.PingUtils.smartPing("8.8.8.8")
                pingGoogle = if (r.success) r.latencyMs.toInt() else -1
            }
            launch {
                val stats = com.pingoptimizer.pro.network.PingUtils.runSamples("9.9.9.9", samples = 4)
                pingQuad9 = if (stats.lossPercent < 100) stats.avgMs.toInt() else -1
                lastJitterMs = stats.jitterMs.toInt()
                lastLossPercent = stats.lossPercent
            }
        }
    }

    // Radar Animation
    val infiniteTransition = rememberInfiniteTransition()
    val radarAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Network Optimizer",
                        color = NeonCyan,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(color = NeonCyan.copy(alpha = 0.5f), blurRadius = 15f)
                        )
                    )
                }
                Icon(Icons.Filled.Radar, contentDescription = "Radar", tint = TextPrimary)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Advanced Network Radar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(20.dp)
            ) {
                val connectionOk = lastLossPercent < 50
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background((if (connectionOk) AccentGreen else Color(0xFFFF4D6D)).copy(alpha = radarAlpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (connectionOk) "CONNECTION SECURE" else "CONNECTION UNSTABLE",
                            color = if (connectionOk) AccentGreen else Color(0xFFFF4D6D),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                    }
                    Text(
                        cellularSnapshot?.networkType?.takeIf { wifiSnapshot?.connected != true }
                            ?: if (wifiSnapshot?.connected == true) "Wi-Fi" else "...",
                        color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RadarStatBox(
                        title = "PACKET LOSS",
                        value = "$lastLossPercent%",
                        color = if (lastLossPercent == 0) AccentGreen else Color(0xFFFFB020)
                    )
                    RadarStatBox(title = "JITTER", value = "${lastJitterMs}ms", color = NeonCyan)
                    RadarStatBox(
                        title = "SIGNAL",
                        value = wifiSnapshot?.takeIf { it.connected }?.qualityLabel?.substringBefore(" (")
                            ?: cellularSnapshot?.qualityLabel?.substringBefore(" (")
                            ?: "...",
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Gaming DNS Switcher
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dns, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Gaming DNS Switcher", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select a DNS server for lower latency and better route optimization.", color = TextSecondary, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DnsCard(
                    title = "Cloudflare",
                    subtitle = "Ultra Low Latency",
                    primaryIp = "1.1.1.1",
                    secondaryIp = "1.0.0.1",
                    icon = Icons.Filled.Bolt,
                    isSelected = currentDnsPrimary == "1.1.1.1",
                    pingMs = pingCloudflare,
                    onClick = { scope.launch { prefs.setDns("Cloudflare", "1.1.1.1", "1.0.0.1") } }
                )
                DnsCard(
                    title = "Google DNS",
                    subtitle = "Maximum Stability",
                    primaryIp = "8.8.8.8",
                    secondaryIp = "8.8.4.4",
                    icon = Icons.Filled.SportsEsports,
                    isSelected = currentDnsPrimary == "8.8.8.8",
                    pingMs = pingGoogle,
                    onClick = { scope.launch { prefs.setDns("Google", "8.8.8.8", "8.8.4.4") } }
                )
                DnsCard(
                    title = "Quad9",
                    subtitle = "Secure Routing",
                    primaryIp = "9.9.9.9",
                    secondaryIp = "149.112.112.112",
                    icon = Icons.Filled.Security,
                    isSelected = currentDnsPrimary == "9.9.9.9",
                    pingMs = pingQuad9,
                    onClick = { scope.launch { prefs.setDns("Quad9", "9.9.9.9", "149.112.112.112") } }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Advanced Diagnostics
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terminal, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Advanced Diagnostics", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Terminal Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF050810))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (pingLog.isEmpty()) {
                        Text("> Ready to run diagnostics...", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    } else {
                        pingLog.forEach { log ->
                            Text(log, color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Run Ping Test Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isTesting) Color.White.copy(alpha = 0.1f) else NeonCyan)
                    .clickable(enabled = !isTesting) {
                        isTesting = true
                        pingLog = emptyList()
                        scope.launch {
                            // Real multi-target diagnostic - every line below reflects an
                            // actual ping result (ICMP via shell, TCP-connect fallback).
                            // Nothing here is scripted; if your connection is bad, this
                            // will honestly show loss/timeouts instead of faking success.
                            val targets = listOf(
                                "Cloudflare" to "1.1.1.1",
                                "Google" to "8.8.8.8",
                                "Quad9" to "9.9.9.9"
                            )
                            val finalStats = com.pingoptimizer.pro.network.PingUtils.runDiagnostic(targets) { line ->
                                pingLog = pingLog + line
                            }
                            pingLog = pingLog + if (finalStats.lossPercent < 100)
                                "> Result: ${if (finalStats.avgMs < 80) "ROUTE HEALTHY" else "ROUTE DEGRADED"}"
                            else "> Result: CONNECTION FAILED - check network"
                            isTesting = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("DIAGNOSTICS RUNNING...", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = BgDeep, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUN PING TEST", color = BgDeep, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun RadarStatBox(title: String, value: String, color: Color) {
    Column {
        Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DnsCard(
    title: String,
    subtitle: String,
    primaryIp: String,
    secondaryIp: String,
    icon: ImageVector,
    isSelected: Boolean,
    pingMs: Int = -1,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) NeonCyan else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.05f) else BgCard)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (isSelected) NeonCyan else Color.White, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (pingMs > 0) {
                    val pingColor = if (pingMs <= 80) AccentGreen else Color.Yellow
                    Text("${pingMs}ms", color = pingColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(primaryIp, color = if (isSelected) NeonCyan else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text(secondaryIp, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
