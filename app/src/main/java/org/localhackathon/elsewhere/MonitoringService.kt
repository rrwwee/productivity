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
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.WindowManager
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToInt

class MonitoringService : Service() {
    companion object {
        const val START = "org.localhackathon.elsewhere.START"
        const val STOP = "org.localhackathon.elsewhere.STOP"
        const val SOON = "org.localhackathon.elsewhere.SOON"
        const val RESET = "org.localhackathon.elsewhere.RESET"
        const val INTERVAL_MS = 30_000L
        const val ARTICLE = "https://en.wikipedia.org/wiki/James_Webb_Space_Telescope"
        private const val ONGOING = "monitoring"
        private const val NUDGES = "nudges"
    }
    private val captureThread = HandlerThread("ElsewhereCapture")
    private lateinit var captureHandler: Handler
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private val inFlight = AtomicBoolean(false)
    @Volatile private var active = false
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
            if (!requestingFrame && DemoState.ready && !DemoState.nudged && !inFlight.get() && now >= DemoState.nextCheckAt) {
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
        notifications.createNotificationChannel(NotificationChannel(NUDGES, "A better next thing", NotificationManager.IMPORTANCE_HIGH))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            STOP -> { finishSession("Session ended. See you next time."); return START_NOT_STICKY }
            SOON -> {
                if (active) { DemoState.nextCheckAt = SystemClock.elapsedRealtime() + 5_000; DemoState.status = "Next check in 5 seconds." }
                return START_NOT_STICKY
            }
            RESET -> {
                DemoState.nudged = false; DemoState.lastDecision = ""
                getSystemService(NotificationManager::class.java).cancel(2)
                if (active) { DemoState.nextCheckAt = SystemClock.elapsedRealtime() + INTERVAL_MS; DemoState.status = "Ready for another moment." }
                return START_NOT_STICKY
            }
        }
        if (active || intent == null) return START_NOT_STICKY
        val returnToApp = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, MonitoringService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, ONGOING)
            .setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Elsewhere is here")
            .setContentText("Private checks every 30 seconds. Tap to return.")
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
        if (!active || !DemoState.ready || DemoState.nudged ||
            SystemClock.elapsedRealtime() < DemoState.nextCheckAt || !inFlight.compareAndSet(false, true)) return
        DemoState.nextCheckAt = SystemClock.elapsedRealtime() + INTERVAL_MS
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
            ModelRunner.classify(applicationContext, bytes) { decision, error ->
                Handler(Looper.getMainLooper()).post {
                    try {
                        if (active) {
                            when {
                                error != null -> DemoState.status = "Check unavailable. We’ll try again in 30 seconds."
                                decision == "X_FEED" && !DemoState.nudged -> sendNudge()
                                decision == "OTHER" -> DemoState.status = "Nothing to interrupt. Carry on."
                                else -> DemoState.status = "No confident match. Carry on."
                            }
                            DemoState.save(this)
                        }
                    } finally { inFlight.set(false) }
                }
            }
        } catch (error: Throwable) {
            inFlight.set(false)
            DemoState.status = "Could not read this frame. We’ll try again."
            Log.w("Elsewhere", "Frame conversion failed", error)
        }
    }

    private fun sendNudge() {
        DemoState.nudged = true
        val article = Intent(Intent.ACTION_VIEW, Uri.parse(ARTICLE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val openArticle = PendingIntent.getActivity(this, 2, article, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, NUDGES)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("A little curiosity instead?")
            .setContentText("Step out of the feed. Meet the James Webb Space Telescope.")
            .setStyle(Notification.BigTextStyle().bigText("Step out of the feed. Explore what the James Webb Space Telescope is discovering."))
            .setContentIntent(openArticle).setAutoCancel(true).setCategory(Notification.CATEGORY_RECOMMENDATION)
            .addAction(Notification.Action.Builder(null, "Explore", openArticle).build()).build()
        getSystemService(NotificationManager::class.java).notify(2, notification)
        DemoState.status = "A better next thing is waiting in your notifications."
        Log.i("Elsewhere", "Nudge delivered after Gemma X_FEED decision")
    }

    private fun finishSession(message: String) {
        if (!active && projection == null) { stopSelf(); return }
        active = false; DemoState.monitoring = false
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
        captureHandler.removeCallbacks(samplingLoop)
        display?.release(); reader?.close(); projection?.stop(); projection = null
        captureThread.quitSafely()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?) = null
}
