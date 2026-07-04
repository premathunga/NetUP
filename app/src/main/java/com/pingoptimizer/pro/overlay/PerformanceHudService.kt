package com.pingoptimizer.pro.overlay

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.pingoptimizer.pro.MainActivity
import com.pingoptimizer.pro.network.PingUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A real, honest floating overlay: shows live ping, RAM usage %, and battery
 * temperature while you're in-game, on top of any app. Deliberately NOT called
 * "FPS Monitor" - a third-party app cannot legitimately read another app's real
 * render frame rate on non-rooted Android without instrumenting that app, so
 * claiming to show "FPS" would just be another fake-booster-style lie. Every
 * number this overlay shows is something we can actually, honestly measure.
 */
class PerformanceHudService : Service() {

    private lateinit var windowManager: WindowManager
    private var hudView: View? = null
    private var pingText: TextView? = null
    private var ramText: TextView? = null
    private var tempText: TextView? = null

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        showHud()
        running = true
        pollLoop()
    }

    private fun pollLoop() {
        scope.launch {
            while (running) {
                updateRam()
                updateBatteryTemp()
                val r = PingUtils.smartPing("1.1.1.1")
                pingText?.text = if (r.success) "${r.latencyMs} ms" else "timeout"
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun updateRam() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        val usedPercent = (((info.totalMem - info.availMem).toDouble() / info.totalMem) * 100).toInt()
        ramText?.text = "$usedPercent%"
    }

    private fun updateBatteryTemp() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, filter)
        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        tempText?.text = if (tempTenths >= 0) "${tempTenths / 10.0}°C" else "--"
    }

    private fun showHud() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(24, 14, 24, 14)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#DD0A0E17"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#3300FFFF"))
            }
        }

        fun statColumn(label: String): Pair<LinearLayout, TextView> {
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 16, 0)
            }
            val labelView = TextView(this).apply {
                text = label
                setTextColor(Color.parseColor("#8A93A6"))
                textSize = 9f
            }
            val valueView = TextView(this).apply {
                text = "--"
                setTextColor(Color.parseColor("#00FFFF"))
                textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            col.addView(labelView)
            col.addView(valueView)
            return col to valueView
        }

        val (pingCol, pingVal) = statColumn("PING")
        val (ramCol, ramVal) = statColumn("RAM")
        val (tempCol, tempVal) = statColumn("TEMP")
        pingText = pingVal
        ramText = ramVal
        tempText = tempVal

        container.addView(pingCol)
        container.addView(ramCol)
        container.addView(tempCol)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 100
        }

        // Draggable: standard touch-based overlay dragging.
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchX).toInt()
                    params.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(container, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(container, params)
        hudView = container
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Performance HUD", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Performance HUD active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        running = false
        job.cancel()
        hudView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 77
        private const val CHANNEL = "performance_hud_channel"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, PerformanceHudService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PerformanceHudService::class.java))
        }
    }
}
