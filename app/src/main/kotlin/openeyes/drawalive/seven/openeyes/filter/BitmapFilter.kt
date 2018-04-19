package openeyes.drawalive.seven.openeyes.filter

import android.graphics.Bitmap

interface BitmapFilter {
    fun act(bmp: Bitmap): Bitmap
}
