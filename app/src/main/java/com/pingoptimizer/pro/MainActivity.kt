package com.pingoptimizer.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pingoptimizer.pro.ui.screens.*
import com.pingoptimizer.pro.ui.theme.*

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Network : Screen("network", "Network\nOptimizer", Icons.Filled.BarChart)
    object Dashboard : Screen("dashboard", "Game\nBooster", Icons.Filled.RocketLaunch)
    object Tools : Screen("tools", "Tools", Icons.Filled.Build)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

private val bottomNavItems = listOf(
    Screen.Home, Screen.Network, Screen.Dashboard, Screen.Tools, Screen.Settings
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PingOptimizerTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = BgDeep) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                bottomNavItems.forEach { screen ->
                    val isStandout = currentRoute == Screen.Home.route && screen == Screen.Dashboard
                    val scale by animateFloatAsState(targetValue = if (isStandout) 1.3f else 1f)

                    NavigationBarItem(
                        icon = { 
                            Box(
                                modifier = Modifier.scale(scale),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    screen.icon, 
                                    contentDescription = screen.label.replace("\n", " "),
                                    tint = LocalContentColor.current
                                ) 
                            }
                        },
                        label = { Text(screen.label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            unselectedIconColor = TextSecondary,
                            selectedTextColor = TextPrimary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = BgCard
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding).background(BgDeep)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Network.route) { NetworkOptimizerScreen() }
            composable(Screen.Dashboard.route) { GameBoosterScreen() }
            composable(Screen.Tools.route) { ToolsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
