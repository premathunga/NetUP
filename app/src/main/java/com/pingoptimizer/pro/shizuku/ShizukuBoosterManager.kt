package com.pingoptimizer.pro.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import kotlinx.coroutines.suspendCancellableCoroutine
import rikka.shizuku.Shizuku
import kotlin.coroutines.resume

/**
 * The "Ultimate Method" from the user's request: uses Shizuku (open-source,
 * no-root, ADB-privilege broker - https://shizuku.rikka.app) to genuinely
 * force-stop other apps the same way `adb shell am force-stop` would, instead
 * of the weak/undo-able ActivityManager.killBackgroundProcesses().
 *
 * Requires the user to separately install the Shizuku app and start its
 * service once (via wireless debugging pairing or root) - this is standard
 * for any Shizuku-integrated app and is documented in the README.
 */
object ShizukuBoosterManager {

    private const val REQUEST_CODE = 9001
    private var service: IUserService? = null
    private var isBound = false

    /** True if the Shizuku service is installed, running, and reachable. */
    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        isAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) {
        false
    }

    fun requestPermission(listener: (granted: Boolean) -> Unit) {
        if (!isAvailable()) {
            listener(false)
            return
        }
        val resultListener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                if (requestCode == REQUEST_CODE) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    listener(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
        Shizuku.addRequestPermissionResultListener(resultListener)
        Shizuku.requestPermission(REQUEST_CODE)
    }

    private var activeConnection: ServiceConnection? = null

    private fun userServiceArgs(context: Context) = Shizuku.UserServiceArgs(
        ComponentName(context.packageName, UserService::class.java.name)
    )
        .daemon(false)
        .processNameSuffix("shizuku_booster")
        .debuggable(false)
        .version(1)

    private suspend fun ensureBound(context: Context): IUserService? {
        if (isBound && service != null) return service
        return suspendCancellableCoroutine { cont ->
            val args = userServiceArgs(context)
            val tempConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    service = IUserService.Stub.asInterface(binder)
                    isBound = true
                    if (cont.isActive) cont.resume(service)
                }
                override fun onServiceDisconnected(name: ComponentName?) {
                    service = null
                    isBound = false
                }
            }
            activeConnection = tempConnection
            try {
                Shizuku.bindUserService(args, tempConnection)
            } catch (e: Throwable) {
                if (cont.isActive) cont.resume(null)
            }
            cont.invokeOnCancellation {
                try {
                    Shizuku.unbindUserService(args, tempConnection, true)
                } catch (e: Throwable) { /* ignore */ }
            }
        }
    }

    /**
     * Force-stops every package in [packages] via the Shizuku-privileged service.
     * Returns the number of packages successfully sent a force-stop call.
     */
    suspend fun forceStopAll(context: Context, packages: List<String>): Int {
        if (!hasPermission()) return 0
        val svc = ensureBound(context) ?: return 0
        var count = 0
        for (pkg in packages) {
            try {
                svc.forceStopPackage(pkg)
                count++
            } catch (e: Exception) {
                // continue with the rest
            }
        }
        return count
    }

    fun unbind(context: Context) {
        val conn = activeConnection
        if (conn != null) {
            try {
                Shizuku.unbindUserService(userServiceArgs(context), conn, true)
            } catch (e: Throwable) {
                // ignore
            }
        }
        activeConnection = null
        isBound = false
        service = null
    }
}
