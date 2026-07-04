package com.pingoptimizer.pro.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isLikelyGame: Boolean
)

object AppListUtils {

    /**
     * Lists installed, launchable, non-system apps. Apps declared with
     * ApplicationInfo.CATEGORY_GAME (or whose package commonly indicates a game
     * publisher) are flagged and sorted first, since that's what users picking a
     * "priority app" for gaming almost always want to find quickly.
     */
    fun listLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val launcherIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(launcherIntent, 0)

        return resolved
            .mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val pkg = appInfo.packageName
                    if (pkg == context.packageName) return@mapNotNull null
                    InstalledAppInfo(
                        packageName = pkg,
                        label = pm.getApplicationLabel(appInfo).toString(),
                        icon = try { pm.getApplicationIcon(appInfo) } catch (e: Exception) { null },
                        isLikelyGame = isLikelyGame(appInfo)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<InstalledAppInfo> { it.isLikelyGame }.thenBy { it.label.lowercase() })
    }

    private fun isLikelyGame(info: ApplicationInfo): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            info.category == ApplicationInfo.CATEGORY_GAME
        } else {
            (info.flags and ApplicationInfo.FLAG_IS_GAME) != 0
        }
    }
}
