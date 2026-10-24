package org.localhackathon.elsewhere

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var start: Button
    private lateinit var stop: Button
    private lateinit var soon: Button
    private val ink = Color.rgb(30, 54, 45)
    private val paper = Color.rgb(246, 245, 237)
    private var preparing = false
    private val tick = object : Runnable {
        override fun run() {
            render()
            ui.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = paper; window.navigationBarColor = paper
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val scroll = ScrollView(this).apply { setBackgroundColor(paper); isFillViewport = true }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(28), dp(38), dp(28), dp(32))
        }
        scroll.addView(column)
        fun text(value: String, size: Float, bold: Boolean = false) = TextView(this).apply {
            text = value; textSize = size; setTextColor(ink)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        fun space(height: Int) { column.addView(Space(this), LinearLayout.LayoutParams(1, dp(height))) }
        column.addView(text("ELSEWHERE", 13f, true).apply { letterSpacing = .22f })
        space(36)
        column.addView(text("A better\nnext thing.", 44f, true))
        space(18)
        column.addView(text("Less feed. More curiosity.\nA quiet nudge when you drift into X.", 18f))
        space(34)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(22), dp(22), dp(22))
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(22).toFloat() }
        }
        status = text("Ready when you are.", 20f, true)
        card.addView(status)
        detail = text("Your screen is processed here, on this phone.", 14f).apply { setPadding(0, dp(12), 0, 0) }
        card.addView(detail); column.addView(card)
        space(24)
        fun button(label: String, primary: Boolean = false, action: () -> Unit): Button {
            return Button(this).apply {
                text = label; isAllCaps = false; textSize = 16f
                setTextColor(if (primary) Color.WHITE else ink)
                backgroundTintList = android.content.res.ColorStateList.valueOf(if (primary) ink else Color.rgb(227, 233, 221))
                setOnClickListener { action() }
                column.addView(this, LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(8) })
            }
        }
        start = button("Start a quiet session", true) { begin() }
        soon = button("Check in 5 seconds") { command(MonitoringService.SOON) }
        stop = button("End session") { command(MonitoringService.STOP) }
        button("Reset the nudge") { command(MonitoringService.RESET) }
        space(20)
        column.addView(text("ON YOUR TERMS", 11f, true).apply { letterSpacing = .15f })
        space(10)
        column.addView(text("One check every 30 seconds. No screenshots saved. No screen data uploaded. You can stop at any time.", 14f))
        space(18)
        column.addView(text("Android shows a sharing indicator while the session is active. Locking your phone or starting another screen share ends the session.", 12f))
        setContentView(scroll)
        handleDebugIntent(intent)
    }

    private fun begin() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 11)
            return
        }
        preparing = true
        ModelRunner.prepare(this) { error ->
            runOnUiThread {
                preparing = false
                if (error == null && !isFinishing) {
                    val manager = getSystemService(MediaProjectionManager::class.java)
                    startActivityForResult(manager.createScreenCaptureIntent(), 12)
                }
                render()
            }
        }
        render()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 11 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) begin()
        else DemoState.status = "Notifications are needed to offer you a nudge."
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12 && resultCode == RESULT_OK && data != null) {
            startForegroundService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.START)
                .putExtra("captureResult", resultCode).putExtra("captureData", data))
        } else if (requestCode == 12) DemoState.status = "Screen sharing was not started."
    }

    private fun command(action: String) {
        if (DemoState.monitoring) startService(Intent(this, MonitoringService::class.java).setAction(action))
        else if (action == MonitoringService.RESET) {
            DemoState.nudged = false; DemoState.lastDecision = ""; DemoState.status = "Ready for a fresh start."
            getSystemService(android.app.NotificationManager::class.java).cancel(2)
        }
        render()
    }

    private fun render() {
        status.text = DemoState.status
        start.isEnabled = !preparing && !DemoState.monitoring && !DemoState.busy
        stop.isEnabled = DemoState.monitoring
        soon.isEnabled = DemoState.monitoring && !DemoState.busy && !DemoState.nudged
        val filePresent = ModelRunner.modelFile(this).isFile
        detail.text = when {
            !filePresent -> "Waiting for the local model to be installed."
            DemoState.busy -> "Gemma is working entirely on this phone."
            DemoState.nudged -> "One nudge delivered. Reset to rehearse again."
            DemoState.monitoring -> {
                val seconds = ((DemoState.nextCheckAt - SystemClock.elapsedRealtime()).coerceAtLeast(0) / 1000) + 1
                "Next check in ${seconds}s. No screen data leaves this phone."
            }
            else -> "Your model is on-device. Start when you’re ready."
        }
    }

    override fun onResume() { super.onResume(); ui.post(tick) }
    override fun onPause() { ui.removeCallbacks(tick); super.onPause() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleDebugIntent(intent) }
    private fun handleDebugIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        val fixture = intent?.getStringExtra("classify_file") ?: return
        // ADB can stage a test image in this debuggable app's private directory.
        val file = File(filesDir, fixture).canonicalFile
        if (file.parentFile != filesDir.canonicalFile || !file.isFile) return
        ModelRunner.prepare(this) { error ->
            if (error == null) ModelRunner.classify(applicationContext, file.readBytes()) { decision, failure ->
                DemoState.status = if (failure == null) "Vision test: $decision" else "Vision test failed: ${failure.message}"
                DemoState.save(this)
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
