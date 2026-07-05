package com.pingoptimizer.pro.ui.screens

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.os.Build
import android.os.PowerManager
import com.pingoptimizer.pro.overlay.CrosshairOverlayService
import com.pingoptimizer.pro.overlay.OverlayPermissionUtils
import com.pingoptimizer.pro.overlay.PerformanceHudService
import com.pingoptimizer.pro.ui.theme.*
import com.pingoptimizer.pro.utils.GameModeManager

@Composable
fun ToolsScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    var dndEnabled by remember { mutableStateOf(false) }
    var keepScreenOn by remember { mutableStateOf(false) }
    var brightnessLock by remember { mutableStateOf(false) }
    var touchLock by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf("BALANCED") }
    var hudActive by remember { mutableStateOf(false) }
    var crosshairActive by remember { mutableStateOf(false) }

    // Real thermal monitoring via PowerManager (API 29+). Not a fake switch -
    // this reflects the device's actual reported thermal status.
    val thermalSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    var thermalMonitoring by remember { mutableStateOf(false) }
    var thermalStatusLabel by remember { mutableStateOf("Not monitoring") }
    val thermalListener = remember {
        if (thermalSupported) {
            PowerManager.OnThermalStatusChangedListener { status ->
                thermalStatusLabel = when (status) {
                    PowerManager.THERMAL_STATUS_NONE -> "Normal"
                    PowerManager.THERMAL_STATUS_LIGHT -> "Warm (light)"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Warm (moderate)"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Hot (severe) - performance may drop"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Critical - close background apps now"
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Emergency - device may shut down"
                    else -> "Unknown"
                }
            }
        } else null
    }
    DisposableEffect(thermalMonitoring) {
        if (thermalSupported && thermalMonitoring && thermalListener != null) {
            powerManager.addThermalStatusListener(thermalListener)
            thermalStatusLabel = when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                else -> "Monitoring..."
            }
        }
        onDispose {
            if (thermalSupported && thermalListener != null) {
                try { powerManager.removeThermalStatusListener(thermalListener) } catch (e: Exception) {}
            }
        }
    }

    fun applyProfile(profile: String) {
        selectedProfile = profile
        val activity = context as? Activity ?: return
        when (profile) {
            "POWER_SAVE" -> {
                keepScreenOn = false
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                brightnessLock = false
                val lp = activity.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = lp
            }
            "EXTREME" -> {
                keepScreenOn = true
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                brightnessLock = true
                val lp = activity.window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                activity.window.attributes = lp
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    powerManager.isSustainedPerformanceModeSupported
                ) {
                    activity.window.setSustainedPerformanceMode(true)
                }
                if (GameModeManager.hasDndAccess(context)) {
                    GameModeManager.enableGameDnd(context)
                    dndEnabled = true
                }
            }
            else -> { // BALANCED: revert to defaults, no forced overrides
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    powerManager.isSustainedPerformanceModeSupported
                ) {
                    activity.window.setSustainedPerformanceMode(false)
                }
            }
        }
    }

    val hasDnd = GameModeManager.hasDndAccess(context)

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
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, contentDescription = "Tools", tint = Color.White, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tools", color = NeonCyan, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = TextPrimary)
                    Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Banner
            Column {
                Text(
                    text = "SYSTEM OPTIMIZER ACTIVE",
                    color = NeonCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(color = NeonCyan.copy(alpha = 0.5f), blurRadius = 15f)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ready to boost gaming performance",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Performance Profile
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Performance Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Filled.Speed, contentDescription = "Speed", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileButton(
                    modifier = Modifier.weight(1f),
                    title = "POWER SAVE",
                    icon = Icons.Filled.EnergySavingsLeaf,
                    isSelected = selectedProfile == "POWER_SAVE",
                    onClick = { applyProfile("POWER_SAVE") }
                )
                ProfileButton(
                    modifier = Modifier.weight(1f),
                    title = "BALANCED",
                    icon = null,
                    isSelected = selectedProfile == "BALANCED",
                    onClick = { applyProfile("BALANCED") }
                )
                ProfileButton(
                    modifier = Modifier.weight(1f),
                    title = "EXTREME",
                    icon = Icons.Filled.RocketLaunch,
                    isSelected = selectedProfile == "EXTREME",
                    onClick = { applyProfile("EXTREME") }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                when (selectedProfile) {
                    "POWER_SAVE" -> "Screen timeout restored, brightness override off, DND unchanged."
                    "EXTREME" -> "Screen stays on, brightness locked to full, sustained performance mode" +
                        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && powerManager.isSustainedPerformanceModeSupported) " enabled" else " (unsupported on this device)") +
                        ", DND enabled."
                    else -> "No forced overrides - your normal device settings apply."
                },
                color = TextSecondary, fontSize = 11.sp
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Utility Toggles
            Text("Utility Toggles", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            UtilityToggleRow(
                title = "Keep Screen On",
                subtitle = "Prevent display timeout",
                icon = Icons.Filled.Smartphone,
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

            UtilityToggleRow(
                title = "Do Not Disturb",
                subtitle = "Block calls & notifications",
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

            UtilityToggleRow(
                title = "Brightness Lock",
                subtitle = "Fix intensity at 100%",
                icon = Icons.Filled.BrightnessHigh,
                checked = brightnessLock,
                onCheckedChange = { checked ->
                    brightnessLock = checked
                    val activity = context as? Activity
                    val layoutParams = activity?.window?.attributes
                    if (checked) {
                        layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    } else {
                        layoutParams?.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                    }
                    activity?.window?.attributes = layoutParams
                }
            )

            UtilityToggleRow(
                title = "Touch Lock",
                subtitle = "Disable nav buttons",
                icon = Icons.Filled.PanTool,
                checked = touchLock,
                onCheckedChange = { checked ->
                    touchLock = checked
                    val activity = context as? Activity
                    val window = activity?.window
                    if (window != null) {
                        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                        if (checked) {
                            insetsController.hide(WindowInsetsCompat.Type.systemBars())
                            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        } else {
                            insetsController.show(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Advanced Tools
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Advanced Tools", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text("EXPERIMENTAL", color = TextPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AdvancedToolCard(
                    modifier = Modifier.weight(1f),
                    title = "FPS Monitor",
                    subtitle = "REAL-TIME OVERLAY",
                    icon = Icons.Filled.GraphicEq,
                    buttonText = "ACTIVATE"
                )
                AdvancedToolCard(
                    modifier = Modifier.weight(1f),
                    title = "Crosshair",
                    subtitle = "FPS PRECISION",
                    icon = Icons.Filled.FilterCenterFocus,
                    buttonText = "CONFIGURE"
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // Thermal Sync Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Thermostat, contentDescription = null, tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Thermal Sync v2", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("CPU/GPU Throttle Control", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = thermalSync,
                    onCheckedChange = { thermalSync = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = BgSurface
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ProfileButton(modifier: Modifier, title: String, icon: ImageVector?, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0077FF))) else Brush.horizontalGradient(listOf(BgCard, BgCard))
    val textColor = if (isSelected) BgDeep else TextPrimary
    val borderColor = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f)
    
    Box(
        modifier = modifier
            .height(70.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = title, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(title, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun UtilityToggleRow(title: String, subtitle: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = TextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun AdvancedToolCard(modifier: Modifier, title: String, subtitle: String, icon: ImageVector, buttonText: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .background(BgCard)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = title, tint = TextPrimary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .clickable { /* Future implementation */ }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}