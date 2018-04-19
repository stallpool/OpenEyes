package openeyes.drawalive.seven.openeyes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Handler
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import android.util.Log
import android.widget.ImageView

import java.io.ByteArrayOutputStream

// Ref:
// Camera      >> https://developer.android.com/training/camera/cameradirect.html#TaskOpenCamera
// CameraExternalIntent: https://developer.android.com/training/camera/photobasics.html
// N21->Bitmap >> https://stackoverflow.com/questions/35826709/yuv-nv21-image-converting-to-bitmap/35852318#35852318

// AutoWhiteBalance:
//   https://stackoverflow.com/questions/35439159/camera2-api-set-custom-white-balance-temperature-color
// YUV420888 to RGB:
//   http://www.polarxiong.com/archives/Android-YUV_420_888%E7%BC%96%E7%A0%81Image%E8%BD%AC%E6%8D%A2%E4%B8%BAI420%E5%92%8CNV21%E6%A0%BC%E5%BC%8Fbyte%E6%95%B0%E7%BB%84.html
// Set Orientation:
//   https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
// ScriptIntrinsic:
//   https://medium.com/@qhutch/android-simple-and-fast-image-processing-with-renderscript-2fa8316273e1

abstract class Preview(context: Context) {

    protected var rs: RenderScript? = null
    protected var yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB? = null
    protected var yuvType: Type.Builder? = null
    protected var rgbaType: Type.Builder? = null
    protected var `in`: Allocation? = null
    protected var out: Allocation? = null
    var imageView: ImageView
        protected set
    protected var handler: Handler

    protected var cameraRotate: Matrix? = null

    init {
        cameraRotate = null
        imageView = ImageView(context)
        handler = Handler()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            rs = RenderScript.create(context)
            yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        } else {
            rs = null
            yuvToRgbIntrinsic = null
        }
    }

    fun rotateCamera(degree: Int) {
        when (degree) {
            90 -> {
                cameraRotate = Matrix()
                cameraRotate!!.postRotate(90f)
            }
            -90 -> {
                cameraRotate = Matrix()
                cameraRotate!!.postRotate(-90f)
            }
            180 -> {
                cameraRotate = Matrix()
                cameraRotate!!.postRotate(180f)
            }
            0 -> cameraRotate = null
            else -> cameraRotate = null
        }
    }

    protected fun yuvToBitmap(bytes: ByteArray, w: Int, h: Int, format: Int): Bitmap? {
        if (rs == null) {
            if (format != ImageFormat.NV21) {
                Log.e(TAG, "Not support image format (not NV21)")
                return null
            }
            return yuvToBitmapCommon(bytes, w, h)
        } else {
            return yuvToBitmapFaster(bytes, w, h)
        }
    }

    protected fun yuvToBitmapFaster(bytes: ByteArray, w: Int, h: Int): Bitmap {
        if (yuvType == null) {
            yuvType = Type.Builder(rs, Element.U8(rs)).setX(bytes.size)
            `in` = Allocation.createTyped(rs, yuvType!!.create(), Allocation.USAGE_SCRIPT)
            rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h)
            out = Allocation.createTyped(rs, rgbaType?.create(), Allocation.USAGE_SCRIPT)
        }
        `in`?.copyFrom(bytes)
        yuvToRgbIntrinsic!!.setInput(`in`)
        yuvToRgbIntrinsic!!.forEach(out)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out?.copyTo(bmp)
        return bmp
    }

    protected fun yuvToBitmapCommon(bytes: ByteArray, w: Int, h: Int): Bitmap? {
        try {
            val img = YuvImage(bytes, ImageFormat.NV21, w, h, null)
            val out = ByteArrayOutputStream()
            img.compressToJpeg(
                    Rect(0, 0, img.width, img.height), 100, out
            )
            val imageBytes = out.toByteArray()
            out.close()
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            return null
        }

    }

    protected fun rotateBitmap(bmp: Bitmap): Bitmap {
        if (cameraRotate == null) return bmp
        val rotated: Bitmap
        //rotated = Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), true);
        rotated = Bitmap.createBitmap(
                bmp, 0, 0,
                bmp.width, bmp.height,
                cameraRotate, true
        )
        return rotated
    }

    abstract fun safeCameraOpen(): Boolean
    abstract fun stopPreview()
    abstract fun stopPreviewAndFreeCamera()

    companion object {
        val TAG = "Preview"
    }
}
