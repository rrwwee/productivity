package org.localhackathon.elsewhere

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import java.io.File

/** Shell-only development check; does not start or impersonate a monitoring session. */
class DiagnosticService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("diagnostics", "Development checks", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(3, Notification.Builder(this, "diagnostics")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Checking local Gemma")
            .setContentText("On-device development test").build(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
        val name = intent?.getStringExtra("classify_file")
        intent?.getStringExtra("article_id")?.let { ArticleLibrary.select(applicationContext, it) }
        val generateCopy = intent?.getBooleanExtra("generate_copy", false) == true
        ModelRunner.prepare(applicationContext) { error ->
            if (error != null) finishTest()
            else if (generateCopy) ModelRunner.generateNudge(applicationContext) { _, failure ->
                DemoState.status = if (failure == null) "Notification copy generated on-device." else "Copy generation failed: ${failure.message}"
                finishTest()
            }
            else if (name == null) finishTest()
            else {
                val image = File(filesDir, name).canonicalFile
                if (image.parentFile != filesDir.canonicalFile || !image.isFile) finishTest()
                else ModelRunner.classify(applicationContext, image.readBytes()) { decision, failure ->
                    DemoState.status = if (failure == null) "Vision test: $decision" else "Vision test failed: ${failure.message}"
                    finishTest()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun finishTest() {
        DemoState.save(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
