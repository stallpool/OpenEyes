package openeyes.drawalive.seven.openeyes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.widget.ImageView

import java.nio.ByteBuffer
import java.util.Collections

import openeyes.drawalive.seven.openeyes.filter.BitmapFilter
import openeyes.drawalive.seven.openeyes.filter.HistogramEqualizationFilter
import openeyes.drawalive.seven.openeyes.filter.Kernel3x3Filter

import android.content.Context.CAMERA_SERVICE
import android.view.Surface
import openeyes.drawalive.seven.openeyes.filter.SobelOperator

internal class Preview2(context: Context) : Preview(context), ImageReader.OnImageAvailableListener {

    protected var mCameraMgr: CameraManager
    protected var mCamera2List: Array<String>? = null
    protected var mCamera2: CameraDevice? = null
    protected var mCamera2Size: Size? = null
    protected var mPreviewRequest: CaptureRequest? = null
    protected var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    protected var mCaptureSession: CameraCaptureSession? = null
    protected var mImageReader: ImageReader? = null
    protected var mBackgroundThread: HandlerThread? = null
    protected var mBackgroundHandler: Handler? = null
    protected var mCamera2Timestamp: Long = 0
    protected val processLock = Any()

    private var busy: Boolean? = false

    init {
        mCameraMgr = context.getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            mCamera2List = mCameraMgr.cameraIdList
        } catch (e: Exception) {
            mCamera2List = null
        }

        mCamera2Timestamp = System.currentTimeMillis()
    }

    override fun safeCameraOpen(): Boolean {
        var qOpened = false
        try {
            // assume last camera is front one
            val cameraId = mCamera2List!![mCamera2List!!.size - 1]
            val cc = mCameraMgr.getCameraCharacteristics(cameraId)
            val map = cc.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            )
            mCamera2Size = map!!.getOutputSizes(SurfaceTexture::class.java)[0]
            var w = mCamera2Size!!.width
            var h = mCamera2Size!!.height
            Log.i(TAG, String.format("CameraSize %d %d ......", w, h))
            if (w!! > 640) w = 640
            if (h!! > 480) h = 480
            mCamera2Size = Size(w, h)
            mImageReader = ImageReader.newInstance(
                    mCamera2Size!!.width, mCamera2Size!!.height,
                    ImageFormat.YUV_420_888, 1
            )
            Log.i(TAG, String.format("%d %d ...", mCamera2Size!!.width, mCamera2Size!!.height))
            mImageReader!!.setOnImageAvailableListener(this, Handler())
            mCameraMgr.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(cameraDevice: CameraDevice) {
                    Log.i(TAG, "camera opened ...")
                    mCamera2 = cameraDevice
                    createCamera2PreviewSession()
                }

                override fun onDisconnected(cameraDevice: CameraDevice) {
                    Log.i(TAG, "camera disconnected ...")
                    mCamera2!!.close()
                    mCamera2 = null
                }

                override fun onError(cameraDevice: CameraDevice, i: Int) {
                    Log.i(TAG, "camera error ...")
                    mCamera2!!.close()
                    mCamera2 = null
                }
            }, null)
            qOpened = true
        } catch (e: Exception) {
            Log.e(TAG, "failed to open Camera")
            e.printStackTrace()
        }

        return qOpened
    }

    override fun stopPreview() {
        stopPreviewAndFreeCamera()
    }

    override fun stopPreviewAndFreeCamera() {
        try {
            mCaptureSession!!.stopRepeating()
            if (mCamera2 != null) {
                mCamera2!!.close()
                mCamera2 = null
            }
            stopBackgroundThread()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        //mBackgroundHandler = new Handler();
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    private fun createCamera2PreviewSession() {
        try {
            Log.i(TAG, "create camera preview session ...")
            startBackgroundThread()
            mPreviewRequestBuilder = mCamera2!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(mImageReader!!.surface)
            mCamera2!!.createCaptureSession(
                    listOf<Surface>(mImageReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            mCaptureSession = cameraCaptureSession
                            try {
                                mPreviewRequestBuilder!!.set(
                                        CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                                )
                                /*mPreviewRequestBuilder.set(
                              CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF
                        );
                        mPreviewRequestBuilder.set(
                              CaptureRequest.COLOR_CORRECTION_MODE,
                              CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                        );
                        mPreviewRequestBuilder.set(
                              CaptureRequest.COLOR_CORRECTION_GAINS,
                              colorTemperature(6500)
                        );*/
                                mPreviewRequest = mPreviewRequestBuilder!!.build()
                                mCaptureSession!!.setRepeatingRequest(
                                        mPreviewRequest, null, mBackgroundHandler
                                )
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }

                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {}
                    }, mBackgroundHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /*public static RggbChannelVector colorTemperature(int whiteBalance) {
      float temperature = whiteBalance / 100;
      float red;
      float green;
      float blue;

      //Calculate red
      if (temperature <= 66)
         red = 255;
      else {
         red = temperature - 60;
         red = (float) (329.698727446 * (Math.pow((double) red, -0.1332047592)));
         if (red < 0)
            red = 0;
         if (red > 255)
            red = 255;
      }


      //Calculate green
      if (temperature <= 66) {
         green = temperature;
         green = (float) (99.4708025861 * Math.log(green) - 161.1195681661);
         if (green < 0)
            green = 0;
         if (green > 255)
            green = 255;
      } else {
         green = temperature - 60;
         green = (float) (288.1221695283 * (Math.pow((double) green, -0.0755148492)));
         if (green < 0)
            green = 0;
         if (green > 255)
            green = 255;
      }

      //calculate blue
      if (temperature >= 66)
         blue = 255;
      else if (temperature <= 19)
         blue = 0;
      else {
         blue = temperature - 10;
         blue = (float) (138.5177312231 * Math.log(blue) - 305.0447927307);
         if (blue < 0)
            blue = 0;
         if (blue > 255)
            blue = 255;
      }

      Log.v(TAG, "red=" + red + ", green=" + green + ", blue=" + blue);
      return new RggbChannelVector((red / 255) * 2, (green / 255), (green / 255), (blue / 255) * 2);
   }*/

    private inner class Camera2PostAction(private val view: ImageView, private val image: Image) : Runnable {
        override fun run() {
            val pixels = yuv420ToNV21(image)
            image.close()
            val w = mCamera2Size!!.width
            val h = mCamera2Size!!.height
            var bmp = yuvToBitmap(pixels, w, h, ImageFormat.NV21)
            bmp = rotateBitmap(bmp!!)
            processFrame(bmp)
        }
    }

    private fun yuv420ToNV21(imgYUV420: Image): ByteArray {
        // Converting YUV_420_888 data to YUV_420_SP (NV21).
        val crop = imgYUV420.cropRect
        val format = imgYUV420.format
        val width = crop.width()
        val height = crop.height()
        val planes = imgYUV420.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                val length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer.get(data, channelOffset, length)
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer.get(rowData, 0, length)
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }

    override fun onImageAvailable(imageReader: ImageReader) {
        try {
            if (busy!!) return
            val image = imageReader.acquireLatestImage()
            mBackgroundHandler!!.post(Camera2PostAction(imageView, image))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun processFrame(bmp: Bitmap) {
        synchronized(processLock) {
            busy = true
        }
        var filter: BitmapFilter? = null
        filter = SobelOperator();
        //filter = Kernel3x3Filter(doubleArrayOf(0.0, -2.0, 0.0, -2.0, 8.0, -2.0, 0.0, -2.0, 0.0))
        //filter = HistogramEqualizationFilter();
        //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,1,0, 0,0,0});
        //filter = new Kernel3x3Filter(new double[]{1.0/16,1.0/8,1.0/16, 1.0/8,1.0/4,1.0/8, 1.0/16,1.0/8,1.0/16});
        //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,0,0, 0,0,1});
        handler.post(FilterAction(this, filter, bmp))
        busy = false
    }

    private inner class FilterAction(internal var preview: Preview2, internal var filter: BitmapFilter?, internal var bmp: Bitmap) : Runnable {

        override fun run() {
            if (filter == null) {
                preview.imageView.setImageBitmap(bmp)
            } else {
                bmp = filter!!.act(bmp)
                preview.imageView.setImageBitmap(bmp)
            }
        }
    }

    companion object {
        private val TAG = "Preview"
    }
}
