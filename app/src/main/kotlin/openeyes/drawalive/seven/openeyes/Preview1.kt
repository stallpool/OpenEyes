package openeyes.drawalive.seven.openeyes

import android.content.Context
import android.graphics.Bitmap
import android.hardware.Camera
import android.util.Log

import openeyes.drawalive.seven.openeyes.filter.BitmapFilter
import openeyes.drawalive.seven.openeyes.filter.Kernel3x3Filter
import openeyes.drawalive.seven.openeyes.filter.SobelOperator

internal class Preview1(context: Context) : Preview(context), Camera.PreviewCallback {

    protected var mCamera: Camera? = null
    private var mSupportedPreviewSizes: List<Camera.Size>? = null

    override fun safeCameraOpen(): Boolean {
        var qOpened = false
        try {
            setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT))
            qOpened = mCamera != null
        } catch (e: Exception) {
            try {
                setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK))
                qOpened = mCamera != null
            } catch (e2: Exception) {
                Log.e(Preview.TAG, "failed to open Camera")
                e.printStackTrace()
            }

        }

        return qOpened
    }

    override fun stopPreview() {
        if (mCamera == null) return
        mCamera!!.stopPreview()
    }

    override fun stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera!!.stopPreview()
            mCamera!!.release()
            mCamera = null
        }
    }

    private fun setCamera(camera: Camera) {
        if (mCamera === camera) {
            return
        }
        stopPreviewAndFreeCamera()

        mCamera = camera

        if (mCamera != null) {
            val localSizes = mCamera!!.parameters.supportedPreviewSizes
            mSupportedPreviewSizes = localSizes

            try {
                mCamera!!.setDisplayOrientation(0)
                mCamera!!.setPreviewCallback(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            mCamera!!.startPreview()
        }
    }

    override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
        val params = camera.parameters
        val size = params.pictureSize
        val w = size.width
        val h = size.height
        var bmp = yuvToBitmap(bytes, w, h, params.previewFormat)
        bmp = rotateBitmap(bmp!!)
        processFrame(bmp)
    }

    private fun processFrame(bmp: Bitmap) {
        var filter: BitmapFilter? = null
        filter = SobelOperator()
        //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,1,0, 0,0,0});
        //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,0,0, 0,0,1});
        //filter = new HistogramEqualizationFilter();
        handler.post(FilterAction(this, filter, bmp))
    }

    private inner class FilterAction(internal var preview: Preview1, internal var filter: BitmapFilter?, internal var bmp: Bitmap) : Runnable {

        override fun run() {
            if (filter == null) {
                preview.imageView.setImageBitmap(bmp)
            } else {
                preview.imageView.setImageBitmap(filter!!.act(bmp))
            }
        }
    }
}
