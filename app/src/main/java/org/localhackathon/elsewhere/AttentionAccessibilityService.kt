package org.localhackathon.elsewhere

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

object AutoResume {
    fun selected(context: Context) = context.getSharedPreferences("demo", Context.MODE_PRIVATE)
        .getBoolean("auto_resume_mode", false)
    fun select(context: Context, value: Boolean) {
        context.getSharedPreferences("demo", Context.MODE_PRIVATE).edit().putBoolean("auto_resume_mode", value).apply()
    }
    fun requested(context: Context) = context.getSharedPreferences("demo", Context.MODE_PRIVATE)
        .getBoolean("auto_resume", false)
    fun setRequested(context: Context, value: Boolean) {
        context.getSharedPreferences("demo", Context.MODE_PRIVATE).edit().putBoolean("auto_resume", value).apply()
    }
    fun permissionEnabled(context: Context): Boolean {
        val component = ComponentName(context, AttentionAccessibilityService::class.java)
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().split(':').any { ComponentName.unflattenFromString(it) == component }
    }
    fun unlocked(context: Context) = context.getSystemService(PowerManager::class.java).isInteractive &&
        !context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
}

/** Only screenshot access: no view-tree access, gestures, or text entry. */
class AttentionAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile var connected: AttentionAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        connected = this
        if (AutoResume.requested(this)) {
            runCatching {
                startForegroundService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.AUTO_START))
            }.onFailure { Log.w("Elsewhere", "Could not resume monitoring", it) }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        connected = null
        if (!AutoResume.permissionEnabled(this)) AutoResume.setRequested(this, false)
        if (DemoState.autoResume && DemoState.monitoring) {
            runCatching { startService(Intent(this, MonitoringService::class.java).setAction(MonitoringService.AUTO_UNAVAILABLE)) }
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (connected === this) connected = null
        super.onDestroy()
    }

    fun capture(executor: Executor, callback: (ByteArray?, String?) -> Unit) {
        if (!AutoResume.unlocked(this)) { callback(null, "Phone is locked"); return }
        takeScreenshot(Display.DEFAULT_DISPLAY, executor, object : TakeScreenshotCallback {
            override fun onSuccess(result: ScreenshotResult) {
                val buffer = result.hardwareBuffer
                var wrapped: Bitmap? = null
                var software: Bitmap? = null
                var scaled: Bitmap? = null
                try {
                    if (!AutoResume.unlocked(this@AttentionAccessibilityService)) {
                        callback(null, "Phone is locked"); return
                    }
                    wrapped = checkNotNull(Bitmap.wrapHardwareBuffer(buffer, result.colorSpace))
                    software = checkNotNull(wrapped.copy(Bitmap.Config.ARGB_8888, false))
                    val width = minOf(540, software.width)
                    val height = (software.height.toLong() * width / software.width).toInt().coerceAtLeast(1)
                    scaled = Bitmap.createScaledBitmap(software, width, height, true)
                    val bytes = ByteArrayOutputStream().use { stream ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        stream.toByteArray()
                    }
                    callback(bytes, null)
                } catch (error: Exception) {
                    callback(null, error.message ?: "Screenshot unavailable")
                } finally {
                    if (scaled !== software) scaled?.recycle()
                    software?.recycle(); wrapped?.recycle(); buffer.close()
                }
            }
            override fun onFailure(errorCode: Int) { callback(null, "Screenshot unavailable ($errorCode)") }
        })
    }
}
