package org.localhackathon.elsewhere

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.AudioAttributes
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.WindowManager
import android.provider.Settings
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class MonitoringService : Service() {
    companion object {
        const val START = "org.localhackathon.elsewhere.START"
        const val STOP = "org.localhackathon.elsewhere.STOP"
        const val SOON = "org.localhackathon.elsewhere.SOON"
        const val RESET = "org.localhackathon.elsewhere.RESET"
        const val INTERVAL_MS = 15_000L
        private const val ONGOING = "monitoring"
        private const val NUDGES = "curiosity_v2"
    }
    private val captureThread = HandlerThread("ElsewhereCapture")
    private lateinit var captureHandler: Handler
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private val inFlight = AtomicBoolean(false)
    @Volatile private var active = false
    @Volatile private var epoch = 0L
    private val episode = NudgeEpisode()
    private var requestingFrame = false
    private var frameRequestedAt = 0L
    private val samplingLoop = object : Runnable {
        override fun run() {
            if (!active) return
            val now = SystemClock.elapsedRealtime()
            if (requestingFrame && now - frameRequestedAt > 5_000) {
                display?.surface = null
                requestingFrame = false
                DemoState.nextCheckAt = now + INTERVAL_MS
                DemoState.status = "No frame available. We’ll try again."
            }
            if (!requestingFrame && DemoState.ready && !inFlight.get() && now >= DemoState.nextCheckAt) {
                requestingFrame = true
                frameRequestedAt = now
                display?.surface = reader?.surface
            }
            captureHandler.postDelayed(this, 250)
        }
    }

    override fun onCreate() {
        super.onCreate()
        captureThread.start(); captureHandler = Handler(captureThread.looper)
        val notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(ONGOING, "Monitoring session", NotificationManager.IMPORTANCE_LOW))
        notifications.createNotificationChannel(NotificationChannel(NUDGES, "Curiosity breaks", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120, 70, 120)
            setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> { finishSession("Session ended. See you next time."); return START_NOT_STICKY }
            SOON -> {
                if (active) { DemoState.nextCheckAt = SystemClock.elapsedRealtime() + 5_000; DemoState.status = "Next check in 5 seconds." }
                DemoState.save(this)
                return START_NOT_STICKY
            }
            RESET -> {
                epoch++
                episode.clear()
                syncEpisodeState()
                DemoState.lastDecision = ""
                getSystemService(NotificationManager::class.java).cancel(2)
                if (active) { DemoState.nextCheckAt = SystemClock.elapsedRealtime() + INTERVAL_MS; DemoState.status = "Ready for another moment." }
                DemoState.save(this)
                return START_NOT_STICKY
            }
        }
        if (active || intent == null) return START_NOT_STICKY
        val returnToApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, MonitoringService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Elsewhere is here")
            .setContentText("Private checks every 15 seconds. Tap to return.")
            .setContentIntent(returnToApp).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        try {
            val data = intent.getParcelableExtra("captureData", Intent::class.java)
                ?: error("Screen capture permission was missing.")
            val currentProjection = checkNotNull(getSystemService(MediaProjectionManager::class.java)
                .getMediaProjection(intent.getIntExtra("captureResult", Activity.RESULT_CANCELED), data)) {
                "Screen capture permission was denied."
            }
            projection = currentProjection
            currentProjection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Handler(Looper.getMainLooper()).post {
                        if (projection === currentProjection) finishSession("Screen sharing ended. Start a new session when ready.")
                    }
                }
            }, captureHandler)
            val bounds = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            val width = 540
            val height = (width.toFloat() * bounds.height() / bounds.width()).roundToInt()
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            active = true; DemoState.monitoring = true; DemoState.nudged = false
            syncEpisodeState()
            DemoState.nextCheckAt = SystemClock.elapsedRealtime() + INTERVAL_MS
            DemoState.status = "Go about your day. We’ll check quietly."
            reader!!.setOnImageAvailableListener({ source ->
                val image = try { source.acquireLatestImage() } catch (_: Exception) { null }
                if (image != null) {
                    try {
                        if (requestingFrame) {
                            requestingFrame = false
                            display?.surface = null
                            considerFrame(image)
                        }
                    } finally { image.close() }
                }
            }, captureHandler)
            display = projection!!.createVirtualDisplay("Elsewhere", width, height, resources.displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, captureHandler)
            captureHandler.post(samplingLoop)
            DemoState.save(this)
        } catch (error: Throwable) {
            Log.e("Elsewhere", "Capture failed", error)
            finishSession("Could not start screen sharing: ${error.message}")
        }
        return START_NOT_STICKY
    }

    private fun considerFrame(image: Image) {
        if (!active || !DemoState.ready ||
            SystemClock.elapsedRealtime() < DemoState.nextCheckAt || !inFlight.compareAndSet(false, true)) return
        DemoState.nextCheckAt = SystemClock.elapsedRealtime() + INTERVAL_MS
        val runEpoch = epoch
        try {
            val plane = image.planes[0]
            val paddedWidth = plane.rowStride / plane.pixelStride
            val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
            padded.copyPixelsFromBuffer(plane.buffer)
            val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
            val stream = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()
            if (cropped !== padded) cropped.recycle()
            padded.recycle(); stream.close()
            DemoState.status = "Checking this moment, privately…"
            ModelRunner.classify(applicationContext, bytes, episodeActive = episode.id != null) { decision, error ->
                Handler(Looper.getMainLooper()).post {
                    var generating = false
                    try {
                        if (active && epoch == runEpoch) {
                            when {
                                error != null -> DemoState.status = "Check unavailable. We’ll try again in 15 seconds."
                                decision == "NUDGE" -> {
                                    generating = true
                                    DemoState.status = "Finding a more interesting next thing…"
                                    val selectedArticle = ArticleLibrary.selected(applicationContext)
                                    val episodeId = episode.begin()
                                    syncEpisodeState()
                                    ModelRunner.generateNudge(applicationContext, selectedArticle, episode.history) { copy, failure ->
                                        Handler(Looper.getMainLooper()).post {
                                            try {
                                                if (active && epoch == runEpoch && episode.id == episodeId) {
                                                    if (copy != null && failure == null && episode.record(episodeId, copy)) {
                                                        sendNudge(copy, selectedArticle)
                                                        syncEpisodeState()
                                                    } else DemoState.status = "Could not prepare a nudge. We’ll try again."
                                                }
                                                DemoState.save(this)
                                            } finally { finishCheck() }
                                        }
                                    }
                                }
                                decision == "WAIT" -> {
                                    val resolved = episode.resolve()
                                    if (resolved) {
                                        epoch++
                                        getSystemService(NotificationManager::class.java).cancel(2)
                                        Log.i("Elsewhere", "Episode completed; monitoring continues")
                                    }
                                    syncEpisodeState()
                                    DemoState.status = if (resolved) "You moved on. Episode finished." else "Nothing to interrupt. Carry on."
                                }
                                else -> DemoState.status = "No confident match. Carry on."
                            }
                            DemoState.save(this)
                        }
                    } finally { if (!generating) finishCheck() }
                }
            }
        } catch (error: Throwable) {
            finishCheck()
            DemoState.status = "Could not read this frame. We’ll try again."
            Log.w("Elsewhere", "Frame conversion failed", error)
        }
    }

    private fun finishCheck() {
        // Skip missed slots if generation took longer than the sampling interval.
        val now = SystemClock.elapsedRealtime()
        while (DemoState.nextCheckAt <= now) DemoState.nextCheckAt += INTERVAL_MS
        inFlight.set(false)
        DemoState.save(this)
    }

    private fun syncEpisodeState() {
        DemoState.episodeId = episode.id ?: 0L
        DemoState.episodeNudges = episode.sentCount
        DemoState.completedEpisodes = episode.completedCount
        DemoState.nudged = episode.sentCount > 0
    }

    private fun sendNudge(copy: NudgeCopy, selectedArticle: PreparedArticle) {
        DemoState.totalNudges++
        DemoState.lastNudgeAt = SystemClock.elapsedRealtime()
        val article = Intent(Intent.ACTION_VIEW, Uri.parse(selectedArticle.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val openArticle = PendingIntent.getActivity(this, 2, article, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        // This copy contains only public article facts, never content from the captured feed.
        // Android otherwise redacts it to the app name while MediaProjection is active.
        val publicVersion = Notification.Builder(this, NUDGES)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(copy.title).setContentText(copy.body)
            .setStyle(Notification.BigTextStyle().bigText(copy.body))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openArticle).setAutoCancel(true).setOnlyAlertOnce(false)
            .addAction(Notification.Action.Builder(null, "See how it worked", openArticle).build()).build()
        val notification = Notification.Builder(this, NUDGES)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setColor(android.graphics.Color.rgb(0, 91, 225))
            .setStyle(Notification.BigTextStyle().bigText(copy.body))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setPublicVersion(publicVersion)
            .setContentIntent(openArticle).setAutoCancel(true).setOnlyAlertOnce(false).setCategory(Notification.CATEGORY_RECOMMENDATION)
            .addAction(Notification.Action.Builder(null, "See how it worked", openArticle).build()).build()
        getSystemService(NotificationManager::class.java).notify(2, notification)
        DemoState.status = "A better next thing is waiting in your notifications."
        Log.i("Elsewhere", "Nudge delivered after Gemma NUDGE verdict; article=${selectedArticle.id}")
    }

    private fun finishSession(message: String) {
        if (!active && projection == null) { stopSelf(); return }
        active = false; DemoState.monitoring = false
        epoch++
        episode.clear()
        syncEpisodeState()
        getSystemService(NotificationManager::class.java).cancel(2)
        captureHandler.removeCallbacks(samplingLoop)
        display?.release(); display = null
        reader?.close(); reader = null
        val oldProjection = projection; projection = null
        oldProjection?.stop()
        DemoState.status = message
        DemoState.save(this)
        ModelRunner.release(applicationContext)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        active = false; DemoState.monitoring = false
        epoch++
        episode.clear()
        syncEpisodeState()
        getSystemService(NotificationManager::class.java).cancel(2)
        captureHandler.removeCallbacks(samplingLoop)
        display?.release(); reader?.close(); projection?.stop(); projection = null
        captureThread.quitSafely()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}
