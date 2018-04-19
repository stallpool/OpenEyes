package openeyes.drawalive.seven.openeyes.filter


import android.graphics.Bitmap

class SobelOperator : BitmapFilter {

    protected var kernel_x: DoubleArray? = null
    protected var kernel_y: DoubleArray? = null

    init {
        setSobelKernel(
                doubleArrayOf(-1.0, 0.0, 1.0, -2.0, 0.0, 2.0, -1.0, 0.0, 1.0),
                doubleArrayOf(-1.0, -2.0, -1.0, 0.0, 0.0, 0.0, 1.0, 2.0, 1.0)
        )
    }

    fun setSobelKernel(kernelX: DoubleArray, kernelY: DoubleArray) {
        kernel_x = kernelX
        kernel_y = kernelY
    }

    override fun act(bmp: Bitmap): Bitmap {
        val w = bmp.width
        val h = bmp.height
        val wh = w * h
        val pixels = IntArray(wh)
        val px = IntArray(wh)
        val py = IntArray(wh)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        Utils.greyScale(pixels)
        System.arraycopy(pixels, 0, px, 0, pixels.size)
        System.arraycopy(pixels, 0, py, 0, pixels.size)
        Utils.convolve3x3(px, w, h, kernel_x!!)
        Utils.convolve3x3(py, w, h, kernel_y!!)
        for (i in pixels.indices.reversed()) {
            val x = px[i].toDouble()
            val y = py[i].toDouble()
            var color = Math.sqrt(x * x + y * y)
            if (color > 255) color = 255.0
            pixels[i] = color.toInt()
        }
        Utils.greyRGB(pixels)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)
        return bmp
    }
}
