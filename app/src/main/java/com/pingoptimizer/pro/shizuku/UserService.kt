package com.pingoptimizer.pro.shizuku

/**
 * This class is instantiated by Shizuku in a *separate process* that runs with
 * `adb shell`-level privilege (granted by the user through the Shizuku app,
 * no root required). That privilege level includes android.permission.
 * FORCE_STOP_PACKAGES, which normal apps are never granted.
 *
 * We reach it via reflection on the hidden IActivityManager AIDL interface -
 * the same interface `am force-stop` itself calls into - so the result is
 * identical to running that shell command, but triggered instantly and
 * silently from inside the app.
 */
class UserService : IUserService.Stub() {

    override fun forceStopPackage(packageName: String) {
        try {
            val activityManagerClass = Class.forName("android.app.ActivityManager")
            val getServiceMethod = activityManagerClass.getMethod("getService")
            val iActivityManager = getServiceMethod.invoke(null)

            val forceStopMethod = iActivityManager.javaClass.getMethod(
                "forceStopPackage",
                String::class.java,
                Int::class.javaPrimitiveType
            )
            // userId 0 = the primary/current user profile
            forceStopMethod.invoke(iActivityManager, packageName, 0)
        } catch (e: Exception) {
            // Swallow - if this specific device's hidden API shape differs,
            // the caller will simply see no effect and can fall back.
        }
    }

    override fun destroy() {
        System.exit(0)
    }
}
