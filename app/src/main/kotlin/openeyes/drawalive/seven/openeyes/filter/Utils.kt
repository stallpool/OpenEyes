package openeyes.drawalive.seven.openeyes.filter

object Utils {
    fun greyScale(pixels: IntArray) {
        val s = 1.0 / 3.0
        greyScale(pixels, s, s, s)
    }

    fun greyScale(pixels: IntArray, sr: Double, sg: Double, sb: Double) {
        for (i in pixels.indices.reversed()) {
            val color = pixels[i]
            val r = color shr 16 and 0xff
            val g = color shr 8 and 0xff
            val b = color and 0xff
            pixels[i] = (r * sr + g * sg + b * sb).toInt()
        }
    }

    fun greyRGB(pixels: IntArray) {
        for (i in pixels.indices.reversed()) {
            val grey = pixels[i] and 0xff
            pixels[i] = grey shl 16 or (grey shl 8) or grey or (0xff shl 24)
        }
    }

    fun convolve3x3(pixels: IntArray, w: Int, h: Int, kernel: DoubleArray) {
        val r = IntArray(pixels.size)
        for (j in h - 2 downTo 1) {
            val offset = j * w
            for (i in w - 2 downTo 1) {
                val offseti = offset + i
                val color = kernel[0] * pixels[offseti - w - 1] +
                        kernel[1] * pixels[offseti - w] +
                        kernel[2] * pixels[offseti - w + 1] +
                        kernel[3] * pixels[offseti - 1] +
                        kernel[4] * pixels[offseti] +
                        kernel[5] * pixels[offseti + 1] +
                        kernel[6] * pixels[offseti + w - 1] +
                        kernel[7] * pixels[offseti + w] +
                        kernel[8] * pixels[offseti + w + 1]
                //if (color < 0) color = -color;
                //if (color > 255) color = 255;
                r[offseti] = color.toInt()
            }
        }
        System.arraycopy(r, 0, pixels, 0, pixels.size)
    }
}
