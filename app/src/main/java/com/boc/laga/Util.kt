package com.boc.laga

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs

/** Satu overlay (gambar atau video). Ukuran sama di tegak & landscape; posisi terpisah. */
class Layer(
    val id: Int, var scale: Int = 50, var removeBg: Boolean = false,
    var px: Float = 0.5f, var py: Float = 0.5f, var lx: Float = 0.5f, var ly: Float = 0.5f,
    val video: Boolean = false
) {
    fun x(land: Boolean) = if (land) lx else px
    fun y(land: Boolean) = if (land) ly else py
    fun setPos(land: Boolean, x: Float, y: Float) { if (land) { lx = x; ly = y } else { px = x; py = y } }
}

object Util {
    const val MAX_VIDEO_MS = 10_000L
    @Volatile var lastError: String? = null

    fun prefs(c: Context) = c.getSharedPreferences("boc", Context.MODE_PRIVATE)

    // ---------- daftar layer ----------
    fun layers(c: Context): MutableList<Layer> {
        val out = mutableListOf<Layer>()
        try {
            val a = JSONArray(prefs(c).getString("layers", "[]"))
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val l = Layer(o.getInt("id"), o.getInt("scale"), o.getBoolean("rm"),
                    o.getDouble("px").toFloat(), o.getDouble("py").toFloat(),
                    o.getDouble("lx").toFloat(), o.getDouble("ly").toFloat(), o.optBoolean("vid", false))
                if (file(c, l).exists()) out.add(l)
            }
        } catch (e: Exception) { }
        return out
    }

    fun saveLayers(c: Context, ls: List<Layer>) {
        val a = JSONArray()
        ls.forEach { l ->
            a.put(JSONObject().put("id", l.id).put("scale", l.scale).put("rm", l.removeBg)
                .put("px", l.px.toDouble()).put("py", l.py.toDouble())
                .put("lx", l.lx.toDouble()).put("ly", l.ly.toDouble()).put("vid", l.video))
        }
        prefs(c).edit().putString("layers", a.toString()).apply()
    }

    fun file(c: Context, l: Layer) = File(c.filesDir, if (l.video) "layer_${l.id}.mp4" else "layer_${l.id}.png")

    /** Tambah overlay dari file pilihan: gambar atau video. */
    fun newLayer(c: Context, ls: MutableList<Layer>, uri: Uri): Layer? {
        val isVideo = c.contentResolver.getType(uri)?.startsWith("video/") == true
        val id = prefs(c).getInt("nextId", 1)
        val l: Layer
        if (isVideo) {
            l = Layer(id, video = true)
            try {
                val dst = file(c, l)
                c.contentResolver.openInputStream(uri).use { ins -> dst.outputStream().use { ins!!.copyTo(it) } }
            } catch (e: Exception) { file(c, l).delete(); return null }
            if (videoFrame(file(c, l).path) == null) { file(c, l).delete(); return null }
            val dur = videoDuration(file(c, l).path)
            if (dur > MAX_VIDEO_MS + 400) {
                file(c, l).delete()
                lastError = "Video maksimal 10 detik (video ini ${dur / 1000} detik)"
                return null
            }
        } else {
            val bmp = decodeUri(c, uri, 1600) ?: return null
            l = Layer(id)
            file(c, l).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        prefs(c).edit().putInt("nextId", id + 1).apply()
        ls.add(l); saveLayers(c, ls)
        return l
    }

    /** Frame pertama video (untuk pratinjau & ukuran). */
    fun videoFrame(path: String): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
            val f = r.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val m = maxOf(f.width, f.height)
            if (m > 1280) {
                val k = 1280f / m
                Bitmap.createScaledBitmap(f, (f.width * k).toInt().coerceAtLeast(1), (f.height * k).toInt().coerceAtLeast(1), true)
            } else f
        } catch (e: Exception) { null } finally { try { r.release() } catch (e: Exception) { } }
    }

    fun deleteLayer(c: Context, ls: MutableList<Layer>, l: Layer) {
        file(c, l).delete()
        framesDir(c, l, false).deleteRecursively(); framesDir(c, l, true).deleteRecursively()
        ls.remove(l); saveLayers(c, ls)
        if (prefs(c).getInt("tplLayerId", -1) == l.id) prefs(c).edit().remove("tplLayerId").apply()
    }

    fun videoDuration(path: String): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(path)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (e: Exception) { 0L } finally { try { r.release() } catch (e: Exception) { } }
    }

    // ---------- frame video (untuk diputar di pratinjau & hapus background video) ----------
    fun framesDir(c: Context, l: Layer, processed: Boolean) = File(c.filesDir, (if (processed) "vr" else "vf") + l.id)

    fun hasProcessed(c: Context, l: Layer): Boolean = (framesDir(c, l, true).list()?.size ?: 0) > 0

    /** Daftar frame yang dipakai sekarang: hasil hapus background kalau aktif, kalau tidak frame asli. */
    fun frameFiles(c: Context, l: Layer): List<File> {
        if (!l.video) return emptyList()
        val d = if (l.removeBg && hasProcessed(c, l)) framesDir(c, l, true) else framesDir(c, l, false)
        return d.listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList()
    }

    private fun scaleDown(f: Bitmap, maxSide: Int): Bitmap {
        val m = maxOf(f.width, f.height)
        if (m <= maxSide) return f
        val k = maxSide.toFloat() / m
        return Bitmap.createScaledBitmap(f, (f.width * k).toInt().coerceAtLeast(1), (f.height * k).toInt().coerceAtLeast(1), true)
    }

    /** Ambil frame video (10 fps, maks 100 frame) ke folder. Dipanggil di thread latar. */
    fun buildFrames(c: Context, l: Layer) {
        val dir = framesDir(c, l, false)
        dir.deleteRecursively(); dir.mkdirs()
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(file(c, l).path)
            val dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val n = (dur / 100).toInt().coerceIn(1, 100)
            for (i in 0 until n) {
                val tUs = if (n == 1) 0L else dur * 1000L * i / n
                val f = r.getFrameAtTime(tUs, MediaMetadataRetriever.OPTION_CLOSEST) ?: continue
                val b = scaleDown(f, 480)
                File(dir, "%03d.jpg".format(i)).outputStream().use { b.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            }
        } catch (e: Exception) { } finally { try { r.release() } catch (e: Exception) { } }
    }

    /** Hapus background semua frame video -> PNG transparan. Dipanggil di thread latar. */
    fun processFrames(c: Context, l: Layer) {
        if ((framesDir(c, l, false).list()?.size ?: 0) == 0) buildFrames(c, l)   // video lama belum punya frame
        val src = framesDir(c, l, false).listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: return
        val dir = framesDir(c, l, true)
        dir.deleteRecursively(); dir.mkdirs()
        for (f in src) {
            val b = BitmapFactory.decodeFile(f.path) ?: continue
            val o = removeBg(b)
            File(dir, f.nameWithoutExtension + ".png").outputStream().use { o.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    fun previewItem(c: Context, l: Layer): PreviewView.Item? =
        loadBitmap(c, l)?.let { b -> PreviewView.Item(b, l).also { it.frames = frameFiles(c, l) } }

    /** Tambah media (gambar/video) di thread latar; video diproses frame-nya. */
    fun addAsync(a: Activity, ls: MutableList<Layer>, uri: Uri, done: (Layer?) -> Unit) {
        val video = a.contentResolver.getType(uri)?.startsWith("video/") == true
        val dlg = if (video) AlertDialog.Builder(a).setMessage("Memproses video...").setCancelable(false).show() else null
        Thread {
            lastError = null
            val l = newLayer(a, ls, uri)
            if (l != null && l.video) buildFrames(a, l)
            a.runOnUiThread {
                try { dlg?.dismiss() } catch (e: Exception) { }
                done(l)
            }
        }.start()
    }

    // ---------- template Canvas -> layer biasa (bisa diatur posisi & ukuran) ----------
    fun hasTemplate(c: Context): Boolean {
        val id = prefs(c).getInt("tplLayerId", -1)
        return layers(c).any { it.id == id }
    }

    fun applyTemplate(c: Context, bmp: Bitmap) {
        val ls = layers(c)
        val tid = prefs(c).getInt("tplLayerId", -1)
        var l = ls.firstOrNull { it.id == tid }
        if (l == null) {
            val id = prefs(c).getInt("nextId", 1)
            l = Layer(id, scale = 100, px = 0.5f, py = 0.1f, lx = 0.5f, ly = 0.1f)
            ls.add(l)
            prefs(c).edit().putInt("nextId", id + 1).putInt("tplLayerId", id).apply()
        }
        file(c, l).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        saveLayers(c, ls)
    }

    fun removeTemplate(c: Context) {
        val ls = layers(c)
        val l = ls.firstOrNull { it.id == prefs(c).getInt("tplLayerId", -1) } ?: return
        deleteLayer(c, ls, l)
    }

    fun loadBitmap(c: Context, l: Layer): Bitmap? {
        if (l.video) return videoFrame(file(c, l).path)
        val bmp = BitmapFactory.decodeFile(file(c, l).path) ?: return null
        return if (l.removeBg) removeBg(bmp) else bmp
    }

    // ---------- gambar latar (hanya referensi di editor) ----------
    private fun bgFile(c: Context) = File(c.filesDir, "bg.jpg")
    fun loadBg(c: Context): Bitmap? = if (bgFile(c).exists()) BitmapFactory.decodeFile(bgFile(c).path) else null
    fun hasBg(c: Context) = bgFile(c).exists()
    fun clearBg(c: Context) { bgFile(c).delete() }
    fun setBg(c: Context, uri: Uri): Boolean {
        val bmp = decodeUri(c, uri, 1920) ?: return false
        bgFile(c).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return true
    }

    fun decodeUri(c: Context, uri: Uri, maxSide: Int): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        c.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var s = 1
        while (bounds.outWidth / s > maxSide || bounds.outHeight / s > maxSide) s *= 2
        c.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = s })
        }
    } catch (e: Exception) { null }

    // ---------- start overlay / game ----------
    fun startOverlay(c: Context) {
        c.startForegroundService(Intent(c, OverlayService::class.java))
    }

    fun launchGame(c: Context): Boolean {
        val pkg = prefs(c).getString("pkg", null) ?: return false
        val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        c.startActivity(i)
        return true
    }

    /** Hapus background: flood-fill dari tepi dengan warna mirip pojok gambar. */
    fun removeBg(src: Bitmap, tol: Int = 60): Bitmap {
        val w = src.width; val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        val bg = px[0]
        val br = (bg shr 16) and 255; val bgc = (bg shr 8) and 255; val bb = bg and 255
        fun match(p: Int) = abs(((p shr 16) and 255) - br) + abs(((p shr 8) and 255) - bgc) + abs((p and 255) - bb) < tol
        val seen = BooleanArray(w * h)
        val q = IntArray(w * h)
        var head = 0; var tail = 0
        fun push(i: Int) { if (!seen[i] && (px[i] ushr 24 == 0 || match(px[i]))) { seen[i] = true; q[tail++] = i } }
        for (x in 0 until w) { push(x); push((h - 1) * w + x) }
        for (y in 0 until h) { push(y * w); push(y * w + w - 1) }
        while (head < tail) {
            val i = q[head++]
            px[i] = 0
            val x = i % w; val y = i / w
            if (x > 0) push(i - 1)
            if (x < w - 1) push(i + 1)
            if (y > 0) push(i - w)
            if (y < h - 1) push(i + w)
        }
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }
}
