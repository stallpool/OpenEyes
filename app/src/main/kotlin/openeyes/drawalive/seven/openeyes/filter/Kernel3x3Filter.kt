package openeyes.drawalive.seven.openeyes.filter

import android.graphics.Bitmap

class Kernel3x3Filter(private val kernel: DoubleArray) : BitmapFilter {

    override fun act(bmp: Bitmap): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val wh = w * h
        val pixels = IntArray(wh)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        // grey scale
        Utils.greyScale(pixels)
        // kernel scale
        Utils.convolve3x3(pixels, w, h, kernel)
        // grep -> r,g,b
        Utils.greyRGB(pixels)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
