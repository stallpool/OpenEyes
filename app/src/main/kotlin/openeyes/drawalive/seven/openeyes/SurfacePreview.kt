package openeyes.drawalive.seven.openeyes

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import android.renderscript.Type
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView

internal class SurfacePreview(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Camera.PreviewCallback {
    var mCamera: Camera? = null
    var mHolder: SurfaceHolder
    var mSupportedPreviewSizes: List<Camera.Size>? = null

    private val rs: RenderScript
    private val yuvToRgbIntrinsic: ScriptIntrinsicYuvToRGB
    private var yuvType: Type.Builder? = null
    private var rgbaType: Type.Builder? = null
    private var `in`: Allocation? = null
    private var out: Allocation? = null
    var img: ImageView

    init {

        mHolder = holder
        mHolder.addCallback(this)
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        rs = RenderScript.create(context)
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

        img = ImageView(context)
    }

    override fun onLayout(b: Boolean, i: Int, i1: Int, i2: Int, i3: Int) {}

    fun setCamera(camera: Camera) {
        if (mCamera === camera) {
            return
        }
        stopPreviewAndFreeCamera()

        mCamera = camera

        if (mCamera != null) {
            val localSizes = mCamera!!.parameters.supportedPreviewSizes
            mSupportedPreviewSizes = localSizes
            requestLayout()

            try {
                mCamera!!.setDisplayOrientation(0)
                mCamera!!.setPreviewDisplay(mHolder)
                mCamera!!.setPreviewCallback(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            Log.i(TAG, String.format("%d %d", this.width, this.height))
            mCamera!!.startPreview()
        }
    }

    fun safeCameraOpen(): Boolean {
        var qOpened = false

        try {
            setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK))
            qOpened = mCamera != null
        } catch (e: Exception) {
            Log.e(TAG, "failed to open Camera")
            e.printStackTrace()
        }

        return qOpened
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return, so stop the preview.
        stopPreviewAndFreeCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (mCamera == null) return
        val parameters = mCamera!!.parameters
        parameters.setPreviewSize(this.width, this.height)
        Log.i(TAG, String.format("%d %d", this.width, this.height))
        requestLayout()
        mCamera!!.parameters = parameters
        mCamera!!.startPreview()
    }

    fun stopPreviewAndFreeCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera!!.stopPreview()
            mCamera!!.release()

            mCamera = null
        }
    }

    override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
        val size = camera.parameters.pictureSize
        val w = size.width
        val h = size.height
        if (yuvType == null) {
            yuvType = Type.Builder(rs, Element.U8(rs)).setX(bytes.size)
            `in` = Allocation.createTyped(rs, yuvType!!.create(), Allocation.USAGE_SCRIPT)

            rgbaType = Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h)
            out = Allocation.createTyped(rs, rgbaType!!.create(), Allocation.USAGE_SCRIPT)
        }

        `in`!!.copyFrom(bytes)

        yuvToRgbIntrinsic.setInput(`in`)
        yuvToRgbIntrinsic.forEach(out)

        val bmpout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out!!.copyTo(bmpout)
        img.setImageBitmap(bmpout)
    }

    companion object {
        private val TAG = "SurfacePreview"
    }
}
