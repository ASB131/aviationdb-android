package uk.aviationdb.android.ui.live

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MapIconCache {
    private val cache = mutableMapOf<String, Bitmap>()

    fun defaultPlaneIcon(context: Context, sizePx: Int): Bitmap {
        val key = "__default_plane__$sizePx"
        cache[key]?.let { return it }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1e88e5") }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = sizePx * 0.06f
        }
        val r = sizePx * 0.28f
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, r, fill)
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, r, stroke)
        cache[key] = bmp
        return bmp
    }

    suspend fun load(context: Context, url: String, sizePx: Int = 64): Bitmap? = withContext(Dispatchers.IO) {
        cache[url]?.let { return@withContext it }
        val loader = ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
        val req = ImageRequest.Builder(context)
            .data(url)
            .size(sizePx)
            .allowHardware(false)
            .build()
        val result = loader.execute(req)
        val drawable = result.drawable ?: return@withContext null
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        cache[url] = bmp
        bmp
    }

    fun rotated(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees, source.width / 2f, source.height / 2f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    fun withDropShadow(source: Bitmap): Bitmap {
        val pad = 8
        val outW = source.width + pad * 2
        val outH = source.height + pad * 2
        val result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val alphaMask = source.extractAlpha()
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            setAlpha(210)
            maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(alphaMask, pad + 1f, pad + 4f, shadowPaint)
        alphaMask.recycle()
        canvas.drawBitmap(source, pad.toFloat(), pad.toFloat(), null)
        return result
    }
}