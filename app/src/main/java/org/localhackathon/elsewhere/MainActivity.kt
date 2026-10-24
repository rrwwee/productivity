package org.localhackathon.elsewhere

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.widget.*
import java.io.File

class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var start: Button
    private lateinit var soon: Button
    private lateinit var reset: Button
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
        space(42)
        column.addView(text("Elsewhere", 38f, true))
        space(12)
        column.addView(text("A better next thing.", 19f))
        space(52)
        status = text("Ready when you are.", 20f, true)
        column.addView(status)
        detail = text("Your screen is processed here, on this phone.", 14f).apply { setPadding(0, dp(12), 0, 0) }
        column.addView(detail)
        space(28)
        fun button(label: String, primary: Boolean = false, action: () -> Unit): Button {
            return Button(this).apply {
                text = label; isAllCaps = false; textSize = 16f
                setTextColor(if (primary) Color.WHITE else ink)
                backgroundTintList = android.content.res.ColorStateList.valueOf(if (primary) ink else Color.rgb(227, 233, 221))
                setOnClickListener { action() }
                column.addView(this, LinearLayout.LayoutParams(-1, dp(58)).apply { bottomMargin = dp(8) })
            }
        }
        start = button("Start", true) {
            if (DemoState.monitoring) command(MonitoringService.STOP) else begin()
        }
        space(10)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        button("Demo controls") {
            controls.visibility = if (controls.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        column.addView(controls)
        controls.addView(text("Prepared article", 14f, true))
        val articles = ArticleLibrary.all(this)
        val selector = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, articles.map { it.title })
            setSelection(articles.indexOfFirst { it.id == ArticleLibrary.selected(this@MainActivity).id }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    ArticleLibrary.select(this@MainActivity, articles[position].id)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        controls.addView(selector, LinearLayout.LayoutParams(-1, dp(58)))
        fun demoButton(label: String, action: () -> Unit) = Button(this).apply {
            text = label; isAllCaps = false; setTextColor(ink)
            setOnClickListener { action() }
            controls.addView(this, LinearLayout.LayoutParams(-1, dp(52)))
        }
        soon = demoButton("Check in 5 seconds") { command(MonitoringService.SOON) }
        reset = demoButton("Reset nudge") { command(MonitoringService.RESET) }
        controls.addView(text("Demo rule", 14f, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        controls.addView(text(ArticleLibrary.rule(this), 13f))
        controls.addView(text("Checks run every 30 seconds. Android shows a sharing indicator. Locking the phone or starting another screen share ends the session.", 12f)
            .apply { setPadding(0, dp(18), 0, 0) })
        space(28)
        column.addView(text("Private, on-device screen checks.\nYou can leave this screen after starting.", 13f))
        setContentView(scroll)
        handleDebugIntent(intent)
    }

    private fun begin() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
        val error = DemoState.status.takeIf {
            it.startsWith("Could not") || it.startsWith("Model could not") ||
                it.startsWith("Notifications are needed") || it.startsWith("Screen sharing was not started")
        }
        status.text = when {
            error != null -> "Needs your attention."
            !ModelRunner.modelFile(this).isFile -> "Waiting for the local model."
            preparing || (DemoState.busy && !DemoState.ready) -> "Getting ready…"
            DemoState.busy -> "Finding your next thing…"
            DemoState.monitoring && DemoState.nudged -> "A nudge was sent."
            DemoState.monitoring -> "Watching quietly."
            else -> "Ready when you are."
        }
        start.text = if (DemoState.monitoring) "Pause" else "Start"
        start.isEnabled = DemoState.monitoring || (!preparing && !DemoState.busy)
        soon.isEnabled = DemoState.monitoring && !DemoState.busy && !DemoState.nudged
        reset.isEnabled = !DemoState.busy
        val filePresent = ModelRunner.modelFile(this).isFile
        detail.text = when {
            error != null -> error
            !filePresent -> "Waiting for the local model to be installed."
            DemoState.busy -> "Gemma is working entirely on this phone."
            DemoState.monitoring && DemoState.nudged -> "Keep exploring, or reset the nudge to rehearse again."
            DemoState.monitoring -> {
                val seconds = ((DemoState.nextCheckAt - SystemClock.elapsedRealtime()).coerceAtLeast(0) / 1000) + 1
                "Next check in ${seconds}s. You can use your phone normally."
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
