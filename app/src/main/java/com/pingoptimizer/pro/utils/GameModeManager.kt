package com.pingoptimizer.pro.utils

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object GameModeManager {

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun isGameDndEnabled(context: Context): Boolean {
        if (!hasDndAccess(context)) return false
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }

    fun requestDndAccess(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    fun enableGameDnd(context: Context) {
        if (!hasDndAccess(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun disableGameDnd(context: Context) {
        if (!hasDndAccess(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    /** Usage-access is required to enumerate recently-used background apps. */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageAccess(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    /**
     * Returns non-system apps that have been active recently (candidates for
     * background cleanup) excluding launchers, this app itself, and the selected
     * game package.
     */
    fun listBackgroundCandidates(context: Context, excludePackage: String?): List<String> {
        if (!hasUsageAccess(context)) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - 1000 * 60 * 60 * 6 // last 6 hours
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
        val pm = context.packageManager

        return stats
            .filter { it.totalTimeInForeground > 0 }
            .map { it.packageName }
            .distinct()
            .filter { pkg ->
                pkg != context.packageName &&
                    pkg != excludePackage &&
                    isUserApp(context, pkg)
            }
            .also { /* sorted by most recently used first via original stats order */ }
    }

    private fun isUserApp(context: Context, packageName: String): Boolean = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        (info.flags and ApplicationInfo.FLAG_SYSTEM) == 0
    } catch (e: Exception) {
        false
    }

    /**
     * Weakest method: asks the OS to kill background processes. Android treats this
     * as a mere *suggestion* - apps in a cached state are free to (and commonly do)
     * restart within seconds. Kept only as the last-resort fallback below.
     */
    fun killBackgroundApps(context: Context, packages: List<String>) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        packages.forEach { pkg ->
            try {
                am.killBackgroundProcesses(pkg)
            } catch (e: Exception) {
                // ignore individual failures
            }
        }
    }

    enum class CleanMethod { SHIZUKU, ACCESSIBILITY, BASIC }

    /** Which real cleanup method is currently usable, best first. */
    fun bestAvailableCleanMethod(context: Context): CleanMethod = when {
        com.pingoptimizer.pro.shizuku.ShizukuBoosterManager.hasPermission() -> CleanMethod.SHIZUKU
        com.pingoptimizer.pro.accessibility.BoostAccessibilityService.isEnabled(context) -> CleanMethod.ACCESSIBILITY
        else -> CleanMethod.BASIC
    }

    /**
     * The real fix for "apps come back after 5 seconds": tries a genuine,
     * non-reversible force-stop via Shizuku first (instant, no UI needed).
     * If Shizuku isn't set up, falls back to driving the real system
     * "Force stop" button via Accessibility (a few seconds, visible to the
     * user). Only if neither is set up does it fall back to the weak
     * killBackgroundProcesses() suggestion.
     *
     * Returns the [CleanMethod] that was actually used, so the UI can show
     * the user an honest result instead of a fixed "success" message.
     */
    suspend fun ultimateClean(
        context: Context,
        packages: List<String>,
        onAccessibilityDone: (() -> Unit)? = null
    ): CleanMethod {
        if (packages.isEmpty()) return CleanMethod.BASIC

        if (com.pingoptimizer.pro.shizuku.ShizukuBoosterManager.hasPermission()) {
            val n = com.pingoptimizer.pro.shizuku.ShizukuBoosterManager.forceStopAll(context, packages)
            if (n > 0) return CleanMethod.SHIZUKU
        }

        if (com.pingoptimizer.pro.accessibility.BoostAccessibilityService.isEnabled(context)) {
            com.pingoptimizer.pro.accessibility.BoostAccessibilityService.startBoostSequence(packages) {
                onAccessibilityDone?.invoke()
            }
            return CleanMethod.ACCESSIBILITY
        }

        killBackgroundApps(context, packages)
        return CleanMethod.BASIC
    }

    fun openAppDataSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
