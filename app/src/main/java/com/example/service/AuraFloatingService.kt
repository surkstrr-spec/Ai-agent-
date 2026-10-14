package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.model.ThemeMode
import com.example.ui.components.FloatingAgentOverlayContent
import com.example.ui.theme.AuraAgentTheme

/**
 * خدمة النظام العائمة (Floating Overlay Service)
 * تعرض الأيقونة العائمة فوق جميع التطبيقات، قابلة للسحب، وتنكشف منها نافذة المحادثة.
 */
class AuraFloatingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_FLOATING"
        const val ACTION_STOP = "ACTION_STOP_FLOATING"
        private const val CHANNEL_ID = "aura_floating_channel"
        private const val NOTIFICATION_ID = 1001

        var isRunning: Boolean = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var floatingContainer: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                showFloatingView()
            }
        }
        return START_STICKY
    }

    private fun showFloatingView() {
        if (floatingContainer != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }
        layoutParams = params

        floatingContainer = FrameLayout(this).apply {
            val composeView = ComposeView(this@AuraFloatingService).apply {
                setContent {
                    AuraAgentTheme(themeMode = ThemeMode.SYSTEM) {
                        FloatingAgentOverlayContent(
                            onCloseService = { stopSelf() },
                            onUpdatePosition = { deltaX, deltaY ->
                                updateWindowPosition(deltaX, deltaY)
                            }
                        )
                    }
                }
            }
            addView(composeView)
        }

        try {
            windowManager?.addView(floatingContainer, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWindowPosition(deltaX: Int, deltaY: Int) {
        val params = layoutParams ?: return
        params.x += deltaX
        params.y += deltaY
        try {
            windowManager?.updateViewLayout(floatingContainer, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aura Agent نشط")
            .setContentText("المساعد العائم جاهز لتنفيذ المهام والأتمتة")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Aura Floating Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "قناة إشعار المساعد العائم للتحكم بالجوال"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingContainer?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        floatingContainer = null
    }
}
