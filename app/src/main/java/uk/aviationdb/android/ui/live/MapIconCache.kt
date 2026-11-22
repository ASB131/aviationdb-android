package uk.aviationdb.android.ui.live

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MapIconCache {
    private val cache = mutableMapOf<String, Bitmap>()

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
}