package openeyes.drawalive.seven.openeyes.filter

import android.graphics.Bitmap

class HistogramEqualizationFilter : BitmapFilter {
    override fun act(bmp: Bitmap): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val wh = w * h
        val pixels = IntArray(wh)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        val histr = IntArray(256)
        val histg = IntArray(256)
        val histb = IntArray(256)
        for (i in pixels.indices.reversed()) {
            val color = pixels[i]
            val r = color shr 16 and 0xff
            val g = color shr 8 and 0xff
            val b = color and 0xff
            histr[r]++
            histg[g]++
            histb[b]++
        }
        for (i in 1..255) {
            histr[i] += histr[i - 1]
            histg[i] += histg[i - 1]
            histb[i] += histb[i - 1]
        }
        for (i in 1..255) {
            histr[i] = (255.0 * histr[i] / wh).toInt()
            histg[i] = (255.0 * histg[i] / wh).toInt()
            histb[i] = (255.0 * histb[i] / wh).toInt()
        }
        for (i in pixels.indices.reversed()) {
            val color = pixels[i]
            var r = color shr 16 and 0xff
            var g = color shr 8 and 0xff
            var b = color and 0xff
            val a = color shr 24 and 0xff
            r = histr[r]
            g = histg[g]
            b = histb[b]
            pixels[i] = a shl 24 or (r shl 16) or (g shl 8) or b
        }
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
