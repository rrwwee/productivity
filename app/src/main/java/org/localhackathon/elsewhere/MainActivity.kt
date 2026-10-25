package org.localhackathon.elsewhere

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.app.AlertDialog
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
    private lateinit var articleSelector: Spinner
    private lateinit var autoResumeButton: Button
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
        autoResumeButton = button("Enable auto-resume") { setupAutoResume() }
        space(10)
        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        button("Demo controls") {
            controls.visibility = if (controls.visibility == View.GONE) View.VISIBLE else View.GONE
        }
        column.addView(controls)
        controls.addView(text("Prepared article", 14f, true))
        val articles = ArticleLibrary.all(this)
        articleSelector = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, articles.map { it.title })
            setSelection(articles.indexOfFirst { it.id == ArticleLibrary.selected(this@MainActivity).id }.coerceAtLeast(0))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    ArticleLibrary.select(this@MainActivity, articles[position].id)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
        }
        controls.addView(articleSelector, LinearLayout.LayoutParams(-1, dp(58)))
        fun demoButton(label: String, action: () -> Unit) = Button(this).apply {
            text = label; isAllCaps = false; setTextColor(ink)
            setOnClickListener { action() }
            controls.addView(this, LinearLayout.LayoutParams(-1, dp(52)))
        }
        soon = demoButton("Check in 5 seconds") { command(MonitoringService.SOON) }
        reset = demoButton("Reset episode") { command(MonitoringService.RESET) }
        controls.addView(text("Demo rule", 14f, true).apply { setPadding(0, dp(18), 0, dp(8)) })
        controls.addView(text(ArticleLibrary.rule(this), 13f))
        controls.addView(text("Checks run every 15 seconds. Auto-resume pauses while locked and continues after unlock. One-session mode ends when the phone locks.", 12f)
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
        if (AutoResume.selected(this)) {
            if (AutoResume.permissionEnabled(this)) {
                AutoResume.setRequested(this, true)
                startForegroundService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.AUTO_START))
            } else setupAutoResume()
            return
        }
        preparing = true
        ModelRunner.prepare(this) { error ->
            runOnUiThread {
                preparing = false
                if (error == null && !isFinishing && !isDestroyed) {
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
        if (requestCode == 13) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) setupAutoResume()
            else DemoState.status = "Notifications are needed to offer you a nudge."
            return
        }
        if (requestCode == 11 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) begin()
        else DemoState.status = "Notifications are needed to offer you a nudge."
    }

    private fun setupAutoResume() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 13)
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Auto-resume after unlock")
            .setMessage("Allow Elsewhere’s Accessibility service to take a screenshot every 15 seconds while your phone is unlocked. Gemma processes each screenshot on this phone to suggest a better activity, then deletes it. Screenshots are never uploaded.\n\nThis does not read the accessibility view tree, type, or tap for you. Checks pause while locked and resume after unlock, even after you close Elsewhere. Tap Pause in Elsewhere to stop.\n\nEnable ‘Elsewhere auto-resume’ under Installed apps in Accessibility settings.")
            .setPositiveButton("Enable auto-resume") { _, _ ->
                if (DemoState.monitoring && !DemoState.autoResume) command(MonitoringService.STOP)
                AutoResume.select(this, true)
                AutoResume.setRequested(this, true)
                if (AutoResume.permissionEnabled(this)) {
                    startForegroundService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.AUTO_START))
                } else startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            .setNeutralButton("Use one-session mode") { _, _ ->
                if (DemoState.monitoring) command(MonitoringService.STOP)
                AutoResume.select(this, false)
                AutoResume.setRequested(this, false)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 12 && resultCode == RESULT_OK && data != null) {
            startForegroundService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.START)
                .putExtra("captureResult", resultCode).putExtra("captureData", data))
        } else if (requestCode == 12) {
            DemoState.status = "Screen sharing was not started."
            if (!DemoState.monitoring) ModelRunner.release(applicationContext)
        }
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
            DemoState.monitoring && DemoState.pausedForLock -> "Paused while locked."
            DemoState.busy -> "Finding your next thing…"
            DemoState.monitoring && DemoState.nudged -> "A better next thing is waiting."
            DemoState.monitoring -> "Watching quietly."
            else -> "Ready when you are."
        }
        start.text = if (DemoState.monitoring) "Pause" else "Start"
        autoResumeButton.text = if (AutoResume.selected(this) && AutoResume.permissionEnabled(this)) "Auto-resume settings" else "Enable auto-resume"
        start.isEnabled = DemoState.monitoring || (!preparing && !DemoState.busy)
        soon.isEnabled = DemoState.monitoring && !DemoState.busy
        reset.isEnabled = !DemoState.busy
        articleSelector.isEnabled = !preparing && !DemoState.busy
        val filePresent = ModelRunner.modelFile(this).isFile
        detail.text = when {
            error != null -> error
            !filePresent -> "Waiting for the local model to be installed."
            DemoState.monitoring && DemoState.pausedForLock -> "Checks resume automatically after you unlock."
            DemoState.busy -> "Gemma is working entirely on this phone."
            DemoState.monitoring && DemoState.nudged -> "${DemoState.episodeNudges} invitation(s) this episode. Checks continue every 15 seconds."
            DemoState.monitoring -> {
                val seconds = ((DemoState.nextCheckAt - SystemClock.elapsedRealtime()).coerceAtLeast(0) / 1000) + 1
                "Next check in ${seconds}s. " + if (DemoState.autoResume) "Auto-resume is on." else "You can use your phone normally."
            }
            else -> "Your model is on-device. Start when you’re ready."
        }
    }

    override fun onResume() {
        super.onResume()
        if (AutoResume.requested(this) && AutoResume.permissionEnabled(this) && !DemoState.monitoring) {
            startForegroundService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.AUTO_START))
        }
        ui.post(tick)
    }
    override fun onPause() { ui.removeCallbacks(tick); super.onPause() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); handleDebugIntent(intent) }
    private fun handleDebugIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        val articleId = intent?.getStringExtra("generate_article")
        if (articleId != null) {
            val article = ArticleLibrary.all(this).firstOrNull { it.id == articleId } ?: return
            val previous = intent.getStringExtra("previous_title")?.let {
                listOf(NudgeCopy(it, intent.getStringExtra("previous_body") ?: ""))
            } ?: emptyList()
            ModelRunner.prepare(this) { error ->
                if (error == null) ModelRunner.generateNudge(applicationContext, article, previous,
                    intent.getStringExtra("copy_instruction")) { _, failure ->
                    DemoState.status = if (failure == null) "Copy test ready." else "Copy test failed: ${failure.message}"
                    DemoState.save(this)
                }
            }
            return
        }
        val fixture = intent?.getStringExtra("classify_file") ?: return
        // ADB can stage a test image in this debuggable app's private directory.
        val file = File(filesDir, fixture).canonicalFile
        if (file.parentFile != filesDir.canonicalFile || !file.isFile) return
        val ruleOverride = intent.getStringExtra("classify_rule")
        ModelRunner.prepare(this) { error ->
            if (error == null) ModelRunner.classify(applicationContext, file.readBytes(), ruleOverride) { decision, failure ->
                DemoState.status = if (failure == null) "Vision test: $decision" else "Vision test failed: ${failure.message}"
                DemoState.save(this)
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
