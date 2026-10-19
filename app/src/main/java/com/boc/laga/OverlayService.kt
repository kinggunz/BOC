package com.boc.laga

import android.app.*
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.graphics.drawable.Icon
import android.media.MediaPlayer
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import kotlin.math.min

class OverlayService : Service() {
    companion object { @Volatile var running = false }

    private class Entry(val layer: Layer, val bmp: Bitmap, val view: View, val player: MediaPlayer? = null)

    private lateinit var wm: WindowManager
    private val entries = mutableListOf<Entry>()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ch = "boc_overlay"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ch, "Overlay", NotificationManager.IMPORTANCE_LOW))
        val fl = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val stop = PendingIntent.getForegroundService(this, 1,
            Intent(this, OverlayService::class.java).setAction("STOP"), fl)
        val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), fl)
        val icon = Icon.createWithResource(this, R.drawable.ic_logo)
        startForeground(1, Notification.Builder(this, ch)
            .setSmallIcon(R.drawable.ic_logo)
            .setContentTitle("Boc Laga Overlay")
            .setContentText("Overlay aktif")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(icon, "Pengaturan", open).build())
            .addAction(Notification.Action.Builder(icon, "Stop", stop).build())
            .build())
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        reload()
        return START_STICKY
    }

    private fun clearViews() {
        entries.forEach {
            it.player?.let { p -> try { p.release() } catch (e: Exception) { } }
            if (it.view.parent != null) wm.removeView(it.view)
        }
        entries.clear()
    }

    private fun reload() {
        clearViews()
        for (l in Util.layers(this)) {
            val b = Util.loadBitmap(this, l) ?: continue
            var mp: MediaPlayer? = null
            val v: View = if (l.video) {
                mp = MediaPlayer()
                videoView(l, mp)
            } else ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_XY; setImageBitmap(b) }
            val lp = WindowManager.LayoutParams(
                10, 10, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT)
            applyLayout(lp, l, b)
            wm.addView(v, lp)
            entries.add(Entry(l, b, v, mp))
        }
        if (entries.isEmpty()) stopSelf()
    }

    /** Video overlay: diputar berulang tanpa suara agar tidak bentrok dengan suara game. */
    private fun videoView(l: Layer, mp: MediaPlayer): TextureView = TextureView(this).apply {
        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                try {
                    mp.setSurface(Surface(st))
                    mp.setDataSource(Util.file(this@OverlayService, l).path)
                    mp.isLooping = true
                    mp.setVolume(0f, 0f)
                    mp.setOnPreparedListener { it.start() }
                    mp.prepareAsync()
                } catch (e: Exception) { }
            }
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture) = true
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }
    }

    /** Ukuran relatif ke sisi PENDEK layar (sama di tegak & landscape); posisi per orientasi. */
    private fun applyLayout(lp: WindowManager.LayoutParams, l: Layer, b: Bitmap) {
        val dm = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(dm)
        val land = dm.widthPixels > dm.heightPixels
        val box = min(dm.widthPixels, dm.heightPixels) * l.scale / 100f
        val r = min(box / b.width, box / b.height)
        lp.width = (b.width * r).toInt().coerceAtLeast(10)
        lp.height = (b.height * r).toInt().coerceAtLeast(10)
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = (l.x(land) * dm.widthPixels - lp.width / 2f).toInt()
        lp.y = (l.y(land) * dm.heightPixels - lp.height / 2f).toInt()
    }

    override fun onConfigurationChanged(c: Configuration) {
        super.onConfigurationChanged(c)
        entries.forEach {
            if (it.view.parent != null) {
                val lp = it.view.layoutParams as WindowManager.LayoutParams
                applyLayout(lp, it.layer, it.bmp)
                wm.updateViewLayout(it.view, lp)
            }
        }
    }

    override fun onDestroy() {
        running = false
        clearViews()
        super.onDestroy()
    }
}
