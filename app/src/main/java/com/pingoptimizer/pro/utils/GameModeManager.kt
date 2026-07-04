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
     * Attempts to free RAM/bandwidth by killing background processes of the given
     * packages. This uses the standard, non-root ActivityManager API - Android will
     * only actually kill apps it considers safe to kill (cached/background state);
     * it will not force-close apps with active foreground services.
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

    fun openAppDataSettings(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
