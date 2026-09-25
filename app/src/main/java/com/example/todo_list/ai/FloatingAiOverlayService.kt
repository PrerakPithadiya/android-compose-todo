package com.example.todo_list.ai

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.todo_list.MainActivity
import com.example.todo_list.R
import kotlin.math.abs

/**
 * System-wide floating overlay service providing an always-accessible Apple HIG
 * purple floating AI assistant shortcut button that floats over any application (e.g. WhatsApp).
 *
 * Physical Snapping Specification:
 * - Always hugs the left or right phone border/edge.
 * - When dragged and released towards the center, smoothly springs back to the nearest border.
 * - Never remains in the center of the display.
 * - Tapping immediately triggers TaskFlow's Ask AI chatbot with natural language task creation.
 */
class FloatingAiOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "taskflow_ai_overlay_channel"
        private const val NOTIFICATION_ID = 2048

        fun start(context: Context) {
            val intent = Intent(context, FloatingAiOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingAiOverlayService::class.java)
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var currentAnimator: AnimatorSet? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()
        setupFloatingView()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FloatingAiButtonManager.isFloatingEnabled || !FloatingAiButtonManager.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        floatingView?.post {
            redockToEdgeAfterConfigurationChange()
        }
    }

    private fun startForegroundServiceWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TaskFlow AI Assistant Shortcut",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the floating AI shortcut accessible across all applications."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_AI_CHAT, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_check)
            .setContentTitle("TaskFlow AI Assistant Active")
            .setContentText("Tap floating button anytime to chat or add tasks with natural language.")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification, 0)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingView() {
        if (!FloatingAiButtonManager.canDrawOverlays(this)) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        val buttonSize = (52 * density).toInt()
        val padding = (4 * density).toInt()
        val totalSize = buttonSize + (padding * 2)

        val screenBounds = getScreenDimensions()
        val screenWidth = screenBounds.x
        val screenHeight = screenBounds.y

        val (savedIsRight, savedYRatio) = FloatingAiButtonManager.getDockedPosition()

        val topMargin = (60 * density).toInt()
        val bottomMargin = (80 * density).toInt()

        val initialY = FloatingAiButtonManager.clampYPosition(
            currentY = (screenHeight * savedYRatio).toInt(),
            buttonHeight = totalSize,
            screenHeight = screenHeight,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )

        val initialX = if (savedIsRight) {
            screenWidth - totalSize
        } else {
            0
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            totalSize,
            totalSize,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        layoutParams = params

        // Container View
        val container = FrameLayout(this).apply {
            setPadding(padding, padding, padding, padding)
            clipChildren = false
            clipToPadding = false
        }

        // Floating Purple Gradient Pill / Circle
        val bubble = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(buttonSize, buttonSize).apply {
                gravity = Gravity.CENTER
            }

            // Apple iOS HIG Purple Gradient
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#7C3AED"), // Deep vibrant purple
                    Color.parseColor("#A855F7")  // Electric purple
                )
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke((1.5f * density).toInt(), Color.parseColor("#55FFFFFF")) // Subtle glowing rim
            }
            background = gradient
            elevation = 10f * density

            // Center Sparkle / AI Star Icon
            val icon = ImageView(this@FloatingAiOverlayService).apply {
                val iconSize = (26 * density).toInt()
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                setImageDrawable(ContextCompat.getDrawable(this@FloatingAiOverlayService, R.drawable.ic_floating_ai_sparkle))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(icon)
        }
        container.addView(bubble)

        // Dragging & Edge Snapping Touch Listener
        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentAnimator?.cancel()
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false

                    // Apple HIG press spring scale
                    bubble.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120).start()
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()

                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                        bubble.animate().scaleX(1.06f).scaleY(1.06f).setDuration(150).start()
                    }

                    if (isDragging) {
                        params.x = startX + dx
                        params.y = startY + dy
                        try {
                            windowManager?.updateViewLayout(container, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    bubble.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()

                    if (!isDragging) {
                        // Single tap detected -> Launch Chatbot directly
                        openChatbot()
                    } else {
                        // Drag completed -> Physically snap strictly to nearest edge
                        snapToBorder(container, params)
                    }
                    true
                }
                else -> false
            }
        }

        floatingView = container
        try {
            windowManager?.addView(container, params)
        } catch (_: Exception) {}
    }

    /**
     * Physically snaps the floating button to the left or right phone border.
     * Guarantees that the button NEVER remains in the center of the display.
     */
    private fun snapToBorder(view: View, params: WindowManager.LayoutParams) {
        val screenBounds = getScreenDimensions()
        val screenWidth = screenBounds.x
        val screenHeight = screenBounds.y
        val density = resources.displayMetrics.density

        val viewWidth = view.width.takeIf { it > 0 } ?: (60 * density).toInt()
        val viewHeight = view.height.takeIf { it > 0 } ?: (60 * density).toInt()

        val topMargin = (60 * density).toInt()
        val bottomMargin = (80 * density).toInt()

        val targetX = FloatingAiButtonManager.calculateDockedTargetX(
            currentX = params.x,
            buttonWidth = viewWidth,
            screenWidth = screenWidth
        )

        val targetY = FloatingAiButtonManager.clampYPosition(
            currentY = params.y,
            buttonHeight = viewHeight,
            screenHeight = screenHeight,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )

        currentAnimator?.cancel()

        val animX = ValueAnimator.ofInt(params.x, targetX).apply {
            interpolator = OvershootInterpolator(0.7f)
            addUpdateListener {
                params.x = it.animatedValue as Int
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }

        val animY = ValueAnimator.ofInt(params.y, targetY).apply {
            addUpdateListener {
                params.y = it.animatedValue as Int
                try {
                    windowManager?.updateViewLayout(view, params)
                } catch (_: Exception) {}
            }
        }

        currentAnimator = AnimatorSet().apply {
            playTogether(animX, animY)
            duration = 320
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val isRightSide = (targetX > 0)
                    val yRatio = targetY.toFloat() / screenHeight.toFloat()
                    FloatingAiButtonManager.savePosition(isRightSide, yRatio)
                }
            })
            start()
        }
    }

    private fun redockToEdgeAfterConfigurationChange() {
        val container = floatingView ?: return
        val params = layoutParams ?: return
        val screenBounds = getScreenDimensions()
        val screenWidth = screenBounds.x
        val screenHeight = screenBounds.y
        val density = resources.displayMetrics.density

        val (isRight, yRatio) = FloatingAiButtonManager.getDockedPosition()
        val viewWidth = container.width.takeIf { it > 0 } ?: (60 * density).toInt()
        val viewHeight = container.height.takeIf { it > 0 } ?: (60 * density).toInt()

        val topMargin = (60 * density).toInt()
        val bottomMargin = (80 * density).toInt()

        params.x = if (isRight) screenWidth - viewWidth else 0
        params.y = FloatingAiButtonManager.clampYPosition(
            currentY = (screenHeight * yRatio).toInt(),
            buttonHeight = viewHeight,
            screenHeight = screenHeight,
            topMargin = topMargin,
            bottomMargin = bottomMargin
        )

        try {
            windowManager?.updateViewLayout(container, params)
        } catch (_: Exception) {}
    }

    private fun openChatbot() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_AI_CHAT, true)
        }
        startActivity(intent)
    }

    private fun getScreenDimensions(): Point {
        val wm = windowManager ?: getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            point.x = bounds.width()
            point.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getSize(point)
        }
        return point
    }

    override fun onDestroy() {
        currentAnimator?.cancel()
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null
        super.onDestroy()
    }
}
