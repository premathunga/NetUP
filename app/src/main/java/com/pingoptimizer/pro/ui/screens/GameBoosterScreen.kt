package com.pingoptimizer.pro.ui.screens

import android.app.Activity
import android.net.VpnService
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pingoptimizer.pro.data.AppPrefs
import com.pingoptimizer.pro.service.GameBoosterVpnService
import com.pingoptimizer.pro.ui.theme.*
import com.pingoptimizer.pro.utils.AppListUtils
import com.pingoptimizer.pro.utils.GameModeManager
import com.pingoptimizer.pro.utils.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun GameBoosterScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { AppPrefs(context) }
    val scope = rememberCoroutineScope()

    val boosterActive by prefs.boosterActive.collectAsState(initial = false)
    val priorityPackage by prefs.priorityPackage.collectAsState(initial = null)
    val priorityLabel by prefs.priorityLabel.collectAsState(initial = null)
    val dnsPrimary by prefs.dnsPrimary.collectAsState(initial = "1.1.1.1")
    val dnsSecondary by prefs.dnsSecondary.collectAsState(initial = "1.0.0.1")
    val throttleEnabled by prefs.throttleEnabled.collectAsState(initial = true)
    
    // Default to PUBG Mobile if nothing is selected
    val displayGameName = priorityLabel ?: "PUBG Mobile"

    val pm = context.packageManager
    val targetIconBmp = remember(priorityPackage) {
        if (priorityPackage != null) {
            try {
                val appInfo = pm.getApplicationInfo(priorityPackage!!, 0)
                pm.getApplicationIcon(appInfo).toBitmap(width = 150, height = 150).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    var showAppPicker by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf(listOf<InstalledAppInfo>()) }

    var dndEnabled by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var cleanedCount by remember { mutableStateOf(0) }
    var ramFreedMb by remember { mutableIntStateOf(0) }
    var currentLatencyMs by remember { mutableIntStateOf(0) }

    val hasDnd = GameModeManager.hasDndAccess(context)
    val hasUsageAccess = GameModeManager.hasUsageAccess(context)

    // Live Latency Polling
    LaunchedEffect(dnsPrimary) {
        withContext(Dispatchers.IO) {
            while (true) {
                try {
                    val process = Runtime.getRuntime().exec("ping -c 1 -W 1 $dnsPrimary")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    val output = reader.readText()
                    process.waitFor()
                    val match = "time=([0-9.]+)".toRegex().find(output)
                    currentLatencyMs = match?.groupValues?.get(1)?.toDouble()?.toInt() ?: -1
                } catch (e: Exception) {
                    currentLatencyMs = -1
                }
                delay(3000)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                dndEnabled = GameModeManager.isGameDndEnabled(context)
                val activity = context as? Activity
                val flags = activity?.window?.attributes?.flags ?: 0
                keepScreenOn = (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            GameBoosterVpnService.start(context, priorityPackage, dnsPrimary, dnsSecondary, throttleEnabled)
            scope.launch { prefs.setBoosterActive(true) }
            
            // Clean RAM automatically
            if (hasUsageAccess) {
                val candidates = GameModeManager.listBackgroundCandidates(context, priorityPackage)
                GameModeManager.killBackgroundApps(context, candidates)
            }
            
            // Launch the selected game
            val pkgToLaunch = priorityPackage ?: "com.tencent.ig"
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkgToLaunch)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
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
                
                // Clean RAM automatically
                if (hasUsageAccess) {
                    val candidates = GameModeManager.listBackgroundCandidates(context, priorityPackage)
                    GameModeManager.killBackgroundApps(context, candidates)
                }
                
                // Launch the selected game
                val pkgToLaunch = priorityPackage ?: "com.tencent.ig" // Default to PUBG Mobile if null
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkgToLaunch)
                if (launchIntent != null) {
                    launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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
                        text = "Game Booster", 
                        color = NeonCyan, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(color = NeonCyan.copy(alpha = 0.5f), blurRadius = 15f)
                        )
                    )
                }
                Icon(Icons.Filled.BarChart, contentDescription = "Stats", tint = TextPrimary)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Current Target Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("CURRENT TARGET", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(displayGameName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetIconBmp != null) {
                            androidx.compose.foundation.Image(
                                bitmap = targetIconBmp,
                                contentDescription = "Game Icon",
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Icon(Icons.Filled.VideogameAsset, contentDescription = "Game Icon", tint = NeonCyan, modifier = Modifier.size(32.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Optimize & Launch Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (boosterActive) androidx.compose.ui.graphics.SolidColor(DangerRed)
                            else Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0077FF)))
                        )
                        .clickable { toggleBooster() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (boosterActive) Icons.Filled.Stop else Icons.Filled.RocketLaunch, contentDescription = null, tint = if (boosterActive) Color.White else BgDeep, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (boosterActive) "STOP OPTIMIZATION" else "OPTIMIZE & LAUNCH", 
                            color = if (boosterActive) Color.White else BgDeep, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Select Different Game
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .background(Color.Transparent)
                        .clickable {
                            apps = AppListUtils.listLaunchableApps(context)
                            showAppPicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELECT DIFFERENT GAME", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Latency
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("LATENCY", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (currentLatencyMs > 0) "${currentLatencyMs}ms" else "--", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.1f))) {
                        val fraction = if (currentLatencyMs > 0) (1f - (currentLatencyMs / 200f)).coerceIn(0f, 1f) else 0f
                        Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (currentLatencyMs in 1..80) AccentGreen else if (currentLatencyMs > 80) Color.Yellow else NeonCyan))
                    }
                }

                // Freed RAM
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .background(BgCard)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("RAM FREED", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (ramFreedMb > 0) "+${ramFreedMb} MB" else "0 MB", color = AccentGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.1f))) {
                        val ramFraction = (ramFreedMb / 1024f).coerceIn(0f, 1f)
                        Box(modifier = Modifier.fillMaxWidth(if (ramFreedMb > 0) ramFraction.coerceAtLeast(0.1f) else 0f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Device Protocol
            Text("DEVICE PROTOCOL", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            GameBoosterToggleRow(
                title = "Do Not Disturb",
                subtitle = "Request Permission ↗",
                icon = Icons.Filled.DoNotDisturbOn,
                checked = dndEnabled,
                onCheckedChange = { checked ->
                    if (!hasDnd) {
                        GameModeManager.requestDndAccess(context)
                    } else {
                        dndEnabled = checked
                        if (checked) GameModeManager.enableGameDnd(context)
                        else GameModeManager.disableGameDnd(context)
                    }
                }
            )
            
            GameBoosterToggleRow(
                title = "Keep Screen On",
                subtitle = "Overrides display timeout",
                icon = Icons.Filled.LightMode,
                checked = keepScreenOn,
                onCheckedChange = { checked ->
                    keepScreenOn = checked
                    val activity = context as? Activity
                    if (checked) {
                        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // RAM Overclock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Memory, contentDescription = "RAM", tint = AccentGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("RAM Overclock", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Aggressively terminate non-essential background processes to allocate maximum memory to your gaming experience.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Resource Log
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RESOURCE LOG", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Apps Cleaned: $cleanedCount", color = AccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clean Now Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable {
                            if (!hasUsageAccess) {
                                GameModeManager.requestUsageAccess(context)
                            } else {
                                scope.launch(Dispatchers.IO) {
                                    val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                                    val memBefore = android.app.ActivityManager.MemoryInfo()
                                    am.getMemoryInfo(memBefore)
                                    val availBefore = memBefore.availMem
                                    
                                    val candidates = GameModeManager.listBackgroundCandidates(context, priorityPackage)
                                    GameModeManager.killBackgroundApps(context, candidates)
                                    
                                    delay(500) // wait for apps to actually die
                                    
                                    val memAfter = android.app.ActivityManager.MemoryInfo()
                                    am.getMemoryInfo(memAfter)
                                    val availAfter = memAfter.availMem
                                    
                                    val freedBytes = (availAfter - availBefore).coerceAtLeast(0)
                                    
                                    withContext(Dispatchers.Main) {
                                        cleanedCount = candidates.size
                                        ramFreedMb = (freedBytes / (1024 * 1024)).toInt()
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CleaningServices, contentDescription = null, tint = BgDeep, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "CLEAN NOW", 
                            color = BgDeep, 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (!hasUsageAccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { GameModeManager.requestUsageAccess(context) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("REQUEST USAGE ACCESS", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showAppPicker) {
        AlertDialog(
            onDismissRequest = { showAppPicker = false },
            confirmButton = {},
            containerColor = BgCard,
            titleContentColor = Color.White,
            title = { Text("Select Priority Game", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.height(450.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(apps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .background(BgDeep)
                                .clickable {
                                    scope.launch { prefs.setPriorityApp(app.packageName, app.label) }
                                    showAppPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            val bmp = remember(app.icon) { 
                                try { 
                                    app.icon?.toBitmap(width = 100, height = 100)?.asImageBitmap() 
                                } catch (e: Exception) { null } 
                            }
                            if (bmp != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bmp,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Android, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // Texts
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (app.isLikelyGame) {
                                        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        app.label,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    app.packageName,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun GameBoosterToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = BgSurface
            )
        )
    }
}
