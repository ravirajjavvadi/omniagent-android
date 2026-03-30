package com.omniagent.app.service

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.app.NotificationCompat
import com.chaquo.python.Python
import com.omniagent.app.MainActivity
import com.omniagent.app.R
import com.omniagent.app.engine.LlamaEngine
import kotlinx.coroutines.*

class FloatingAIService : Service() {

    companion object {
        private const val TAG = "FloatingAI"
        private const val NOTIF_CHANNEL_ID = "floating_ai"
        private const val NOTIF_ID = 42
        var isRunning = false
    }

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var pillView: View
    private lateinit var expandedView: View
    private var isExpanded = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createForegroundNotification()
        inflateFloatingUI()
    }

    private fun createForegroundNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(NOTIF_CHANNEL_ID, "Floating AI", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("OmniAgent AI")
            .setContentText("Floating AI Orb is active")
            .setContentIntent(intent)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun inflateFloatingUI() {
        val inflater = LayoutInflater.from(this)

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).also {
            it.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            it.x = 0
            it.y = 48
        }

        floatingView = inflater.inflate(R.layout.layout_floating_ai, null)
        pillView = floatingView.findViewById(R.id.pillView)
        expandedView = floatingView.findViewById(R.id.expandedView)

        windowManager.addView(floatingView, params)
        setupInteractions()
    }

    private fun setupInteractions() {
        val inputField = floatingView.findViewById<EditText>(R.id.etAiInput)
        val sendBtn = floatingView.findViewById<ImageButton>(R.id.btnSend)
        val closeBtn = floatingView.findViewById<ImageButton>(R.id.btnClose)
        val responseText = floatingView.findViewById<TextView>(R.id.tvAiResponse)

        // Tap pill to expand
        pillView.setOnClickListener {
            toggleExpanded(true)
        }

        // Close (collapse) button
        closeBtn.setOnClickListener {
            toggleExpanded(false)
        }

        // Drag support for the pill (collapsed only)
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        pillView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isExpanded) {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        true
                    } else false
                }
                else -> false
            }
        }

        // Send button
        sendBtn.setOnClickListener {
            val query = inputField.text.toString().trim()
            if (query.isEmpty()) return@setOnClickListener

            inputField.setText("")
            responseText.text = "⏳ Thinking..."

            serviceScope.launch {
                val answer = withContext(Dispatchers.IO) { runLocalAI(query) }
                responseText.text = answer
            }
        }
    }

    private fun toggleExpanded(expand: Boolean) {
        isExpanded = expand
        pillView.visibility = if (expand) View.GONE else View.VISIBLE
        expandedView.visibility = if (expand) View.VISIBLE else View.GONE

        // Allow keyboard input when expanded
        params.flags = if (expand)
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        else
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        windowManager.updateViewLayout(floatingView, params)

        if (expand) {
            floatingView.findViewById<EditText>(R.id.etAiInput)?.requestFocus()
        }
    }

    private fun runLocalAI(query: String): String {
        return try {
            val py = Python.getInstance()
            val mod = py.getModule("ai_engine")
            mod.callAttr("ask", query).toString()
        } catch (e: Exception) {
            try {
                val llama = LlamaEngine()
                llama.generate(query)
            } catch (e2: Exception) {
                "⚠️ Model not loaded. Please open OmniAgent and select a model."
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        try { windowManager.removeView(floatingView) } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
