// AIDL interface exposed by our Shizuku UserService. Runs with the same
// privilege level as `adb shell` (granted by the user via the Shizuku app),
// which includes the FORCE_STOP_PACKAGES permission normal apps don't have.
package com.pingoptimizer.pro.shizuku;

interface IUserService {

    /**
     * Force-stops a package the same way `adb shell am force-stop <pkg>` would.
     * Unlike ActivityManager.killBackgroundProcesses() (which the OS is free to
     * ignore/undo within seconds), this genuinely tears the process down - it
     * will not silently restart the way "fake booster" apps' cleanup does.
     */
    void forceStopPackage(String packageName) = 1;

    /** Required by Shizuku so it can cleanly tear down the remote service. */
    void destroy() = 16777114;
}
