package com.pingoptimizer.pro.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.pingoptimizer.pro.data.AppPrefs
import com.pingoptimizer.pro.service.GameBoosterVpnService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import com.pingoptimizer.pro.ui.theme.*

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val scope = rememberCoroutineScope()

    // Real Metrics State
    var pingMs by remember { mutableIntStateOf(0) }
    var pingHistory by remember { mutableStateOf(List(10) { 0 }) }
    var ramUsagePercent by remember { mutableIntStateOf(0) }
    var batteryTemp by remember { mutableFloatStateOf(0f) }

    // Fetch real Temp
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                batteryTemp = temp / 10f
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
        }
    }

    // Fetch real Ping and RAM periodically
    LaunchedEffect(Unit) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        while (true) {
            // Update RAM
            am.getMemoryInfo(memoryInfo)
            val total = memoryInfo.totalMem.toDouble()
            val avail = memoryInfo.availMem.toDouble()
            ramUsagePercent = (((total - avail) / total) * 100).toInt()

            // Update Ping - centralized, real measurement (ICMP with TCP-connect fallback)
            pingMs = withContext(Dispatchers.IO) {
                val r = com.pingoptimizer.pro.network.PingUtils.smartPing("8.8.8.8")
                if (r.success) r.latencyMs.toInt() else -1
            }
            if (pingMs > 0) {
                pingHistory = (pingHistory.drop(1) + pingMs)
            }
            delay(2000)
        }
    }

    val boosterActive by prefs.boosterActive.collectAsState(initial = false)
    val priorityPackage by prefs.priorityPackage.collectAsState(initial = null)
    val dnsPrimary by prefs.dnsPrimary.collectAsState(initial = "1.1.1.1")
    val dnsSecondary by prefs.dnsSecondary.collectAsState(initial = "1.0.0.1")
    val throttleEnabled by prefs.throttleEnabled.collectAsState(initial = true)

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GameBoosterVpnService.start(context, priorityPackage, dnsPrimary, dnsSecondary, throttleEnabled)
            scope.launch { prefs.setBoosterActive(true) }
        }
    }

    fun toggleBooster() {
        if (boosterActive) {
            GameBoosterVpnService.stop(context)
            scope.launch { prefs.setBoosterActive(false) }
        } else {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent != null) {
                vpnPermissionLauncher.launch(vpnIntent)
            } else {
                GameBoosterVpnService.start(context, priorityPackage, dnsPrimary, dnsSecondary, throttleEnabled)
                scope.launch { prefs.setBoosterActive(true) }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NetUP",
                    color = NeonCyan,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(color = NeonCyan.copy(alpha = 0.5f), blurRadius = 15f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.Send, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "PING:",
                    value = if (pingMs > 0) "${pingMs}ms" else "--",
                    valueColor = if (pingMs in 1..80) AccentGreen else if (pingMs > 80) Color.Yellow else NeonCyan,
                    content = {
                        Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                            val maxPing = (pingHistory.maxOrNull() ?: 100).coerceAtLeast(100)
                            val path = Path()
                            val stepX = size.width / (pingHistory.size - 1).coerceAtLeast(1)
                            
                            pingHistory.forEachIndexed { index, value ->
                                val x = index * stepX
                                // map ping value to height (higher ping = lower y value in canvas implies higher graph peak, but standard graph is 0 at bottom)
                                val normalizedValue = 1f - (value.toFloat() / maxPing.toFloat()).coerceIn(0f, 1f)
                                val y = normalizedValue * size.height
                                
                                if (index == 0) path.moveTo(x, y)
                                else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = if (pingMs in 1..80) AccentGreen else if (pingMs > 80) Color.Yellow else NeonCyan,
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "RAM:",
                    value = "${ramUsagePercent}% Used",
                    valueColor = Color.White,
                    content = {
                        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.1f))) {
                            Box(modifier = Modifier.fillMaxWidth(ramUsagePercent / 100f).height(4.dp).background(NeonCyan))
                        }
                    }
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "TEMP:",
                    value = "${batteryTemp}°C",
                    valueColor = if (batteryTemp > 40f) Color.Red else Color.White,
                    content = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = if (batteryTemp > 40f) Color.Red else AccentGreen, modifier = Modifier.size(28.dp))
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // MEGA BOOST BUTTON
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Pulse Ring 1
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .border(2.dp, NeonCyan.copy(alpha = 0.2f), CircleShape)
                )
                
                // Outer Pulse Ring 2
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale * 0.9f)
                        .clip(CircleShape)
                        .border(4.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
                )
                
                // Main Button
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(BgDeep)
                        .border(8.dp, if (boosterActive) AccentGreen else NeonCyan, CircleShape)
                        .clickable { toggleBooster() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "MEGA",
                            color = if (boosterActive) AccentGreen else NeonCyan,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(color = if (boosterActive) AccentGreen else NeonCyan, blurRadius = 20f)
                            )
                        )
                        Text(
                            text = "BOOST",
                            color = if (boosterActive) AccentGreen else NeonCyan,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(color = if (boosterActive) AccentGreen else NeonCyan, blurRadius = 20f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, title: String, value: String, valueColor: Color, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(12.dp)
    ) {
        Text(title, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        content()
    }
}
