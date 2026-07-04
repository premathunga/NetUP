package com.pingoptimizer.pro.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * The "No-Shizuku Method" from the user's request: with the user's Accessibility
 * permission, automatically navigates each app's "App info" screen and taps the
 * real system "Force stop" button (+ the confirmation dialog's "OK") - the same
 * button a human would tap manually. This is the technique Greenify historically
 * used for genuine, non-reversible app freezing without root.
 *
 * IMPORTANT - by design this service does nothing at all unless [armed] is true.
 * It is only armed for the few seconds it takes to process the boost queue that
 * [startBoostSequence] hands it, then immediately disarms itself. This keeps it
 * inert (no battery/privacy cost, no unwanted interaction with any other app)
 * the rest of the time, and avoids scanning every screen you use the phone for.
 *
 * CAVEATS (please read before shipping this to real users):
 * - The exact button/label layout of the system Settings app differs across
 *   Android versions and OEM skins (Samsung/Xiaomi/Oppo etc). The text matching
 *   below covers the common English AOSP wording; it may need tuning per-device.
 * - Google Play policy restricts Accessibility Service usage to genuine
 *   accessibility purposes. This pattern is intended for sideloaded/open-source
 *   distribution (F-Droid style), not necessarily guaranteed to pass Play Store
 *   review - see README for details.
 */
class BoostAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<String>()
    private var onFinished: (() -> Unit)? = null
    private var stepInFlight = false

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!armed) return
        if (event == null) return
        if (event.packageName?.toString() != "com.android.settings") return
        if (stepInFlight) return

        val root = rootInActiveWindow ?: return

        // Step 1: find and tap the "Force stop" button on the App info screen.
        val forceStopNode = findNodeByText(root, FORCE_STOP_LABELS)
        if (forceStopNode != null && forceStopNode.isEnabled) {
            stepInFlight = true
            forceStopNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler.postDelayed({ tapConfirmDialog() }, 350)
            return
        }

        // Step 2: the confirmation dialog appeared as its own content-changed event.
        tapConfirmDialog()
    }

    private fun tapConfirmDialog() {
        val root = rootInActiveWindow
        val okNode = root?.let { findNodeByText(it, CONFIRM_LABELS) }
        if (okNode != null && okNode.isEnabled) {
            okNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            handler.postDelayed({ processNext() }, 400)
        } else if (stepInFlight) {
            // No dialog appeared (some ROMs force-stop without confirmation) - move on.
            handler.postDelayed({ processNext() }, 300)
        }
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, labels: List<String>): AccessibilityNodeInfo? {
        for (label in labels) {
            val matches = root.findAccessibilityNodeInfosByText(label)
            val clickable = matches.firstOrNull { it.isClickable } ?: matches.firstOrNull()
            if (clickable != null) return clickable
        }
        return null
    }

    private fun processNext() {
        stepInFlight = false
        val next = queue.poll()
        if (next == null) {
            armed = false
            onFinished?.invoke()
            onFinished = null
            // Return the user to our app instead of leaving them in Settings.
            val returnIntent = packageManager.getLaunchIntentForPackage(packageName)
            returnIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            returnIntent?.let { startActivity(it) }
            return
        }
        openAppInfo(next)
    }

    private fun openAppInfo(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    companion object {
        private var instance: BoostAccessibilityService? = null
        private var armed = false

        private val FORCE_STOP_LABELS = listOf("Force stop", "FORCE STOP", "Force Stop")
        private val CONFIRM_LABELS = listOf("OK", "Ok", "FORCE STOP", "Force stop")

        fun isEnabled(context: Context): Boolean = instance != null

        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }

        /**
         * Kicks off the force-stop sequence for [packages] one at a time.
         * [onFinished] is called after the last package has been processed
         * (or immediately, with 0 processed, if the service isn't enabled).
         */
        fun startBoostSequence(packages: List<String>, onFinished: () -> Unit) {
            val svc = instance
            if (svc == null || packages.isEmpty()) {
                onFinished()
                return
            }
            svc.queue.clear()
            svc.queue.addAll(packages)
            svc.onFinished = onFinished
            armed = true
            svc.processNext()
        }
    }
}
