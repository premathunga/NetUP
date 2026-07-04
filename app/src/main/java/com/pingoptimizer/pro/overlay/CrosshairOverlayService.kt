package com.pingoptimizer.pro.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.pingoptimizer.pro.MainActivity

/**
 * Draws a simple, click-through crosshair overlay at the center of the screen.
 * FLAG_NOT_TOUCHABLE means it never intercepts touches - it's purely visual, so
 * it can't interfere with your game's actual controls underneath it.
 */
class CrosshairOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var crosshairView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        showCrosshair()
    }

    private fun showCrosshair() {
        val view = object : View(this) {
            private val paint = Paint().apply {
                color = Color.parseColor("#00FFFF")
                strokeWidth = 4f
                isAntiAlias = true
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f
                val cy = height / 2f
                val len = 22f
                val gap = 6f
                canvas.drawLine(cx - len, cy, cx - gap, cy, paint)
                canvas.drawLine(cx + gap, cy, cx + len, cy, paint)
                canvas.drawLine(cx, cy - len, cx, cy - gap, paint)
                canvas.drawLine(cx, cy + gap, cx, cy + len, paint)
                canvas.drawCircle(cx, cy, 2f, paint)
            }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            120, 120, overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager.addView(view, params)
        crosshairView = view
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL, "Crosshair Overlay", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Crosshair overlay active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        crosshairView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 78
        private const val CHANNEL = "crosshair_overlay_channel"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, CrosshairOverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CrosshairOverlayService::class.java))
        }
    }
}
