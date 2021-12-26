package openeyes.drawalive.seven.openeyes;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.util.Collections;

import openeyes.drawalive.seven.openeyes.filter.BitmapFilter;
import openeyes.drawalive.seven.openeyes.filter.SobelOperator;

import static android.content.Context.CAMERA_SERVICE;

import androidx.core.app.ActivityCompat;

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
class Preview2 extends Preview implements ImageReader.OnImageAvailableListener {
   private static final String TAG = "Preview";

   Preview2(Context context) {
      super(context);
      mCameraMgr = (CameraManager) context.getSystemService(CAMERA_SERVICE);
      try {
         mCamera2List = mCameraMgr.getCameraIdList();
      } catch (Exception e) {
         mCamera2List = null;
      }
      mCamera2Timestamp = System.currentTimeMillis();
      processLock = new Object();
      processBusy = false;
   }

   @Override
   public boolean safeCameraOpen() {
      boolean qOpened = false;
      try {
         // assume last camera is front one
         String cameraId = mCamera2List[mCamera2List.length - 1];
         CameraCharacteristics cc = mCameraMgr.getCameraCharacteristics(cameraId);
         StreamConfigurationMap map = cc.get(
               CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
         );
         mCamera2Size = map.getOutputSizes(SurfaceTexture.class)[0];
         int w = mCamera2Size.getWidth(), h = mCamera2Size.getHeight();
         Log.i(TAG, String.format("CameraSize %d %d ......", w, h));
         if (w > 640) w = 640;
         if (h > 480) h = 480;
         mCamera2Size = new Size(w, h);
         mImageReader = ImageReader.newInstance(
               mCamera2Size.getWidth(), mCamera2Size.getHeight(),
               ImageFormat.YUV_420_888, 1
         );
         Log.i(TAG, String.format("%d %d ...", mCamera2Size.getWidth(), mCamera2Size.getHeight()));
         mImageReader.setOnImageAvailableListener(this, new Handler());
         if (ActivityCompat.checkSelfPermission(
               this.getImageView().getContext(), Manifest.permission.CAMERA
         ) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            throw new Exception("permission denied");
         }
         mCameraMgr.openCamera(cameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(CameraDevice cameraDevice) {
               Log.i(TAG, "camera opened ...");
               mCamera2 = cameraDevice;
               createCamera2PreviewSession();
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
               Log.i(TAG, "camera disconnected ...");
               mCamera2.close();
               mCamera2 = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int i) {
               Log.i(TAG, "camera error ...");
               mCamera2.close();
               mCamera2 = null;
            }
         }, null);
         qOpened = true;
      } catch (Exception e) {
         Log.e(TAG, "failed to open Camera");
         e.printStackTrace();
      }
      return qOpened;
   }

   @Override
   public void stopPreview() {
      stopPreviewAndFreeCamera();
   }

   @Override
   public void stopPreviewAndFreeCamera() {
      try {
         if (mCaptureSession != null) {
            mCaptureSession.stopRepeating();
            mCaptureSession = null;
         }
         if (mCamera2 != null) {
            mCamera2.close();
            mCamera2 = null;
         }
         stopBackgroundThread();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   protected CameraManager mCameraMgr;
   protected String[] mCamera2List;
   protected CameraDevice mCamera2;
   protected Size mCamera2Size;
   protected CaptureRequest mPreviewRequest;
   protected CaptureRequest.Builder mPreviewRequestBuilder;
   protected CameraCaptureSession mCaptureSession;
   protected ImageReader mImageReader;
   protected HandlerThread mBackgroundThread;
   protected Handler mBackgroundHandler;
   protected long mCamera2Timestamp;
   protected Object processLock;
   protected boolean processBusy;

   public void lock() { synchronized (processLock) { processBusy = true; } }
   public void unlock() { processBusy = false; }

   private void startBackgroundThread() {
      mBackgroundThread = new HandlerThread("CameraBackground");
      mBackgroundThread.start();
      mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
      //mBackgroundHandler = new Handler();
   }

   private void stopBackgroundThread() {
      mBackgroundThread.quitSafely();
      try {
         mBackgroundThread.join();
         mBackgroundThread = null;
         mBackgroundHandler = null;
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
   private void createCamera2PreviewSession() {
      try {
         Log.i(TAG, "create camera preview session ...");
         startBackgroundThread();
         mPreviewRequestBuilder = mCamera2.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
         mPreviewRequestBuilder.addTarget(mImageReader.getSurface());
         mCamera2.createCaptureSession(
               Collections.singletonList(mImageReader.getSurface()),
               new CameraCaptureSession.StateCallback() {
                  @Override
                  public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                     mCaptureSession = cameraCaptureSession;
                     try {
                        mPreviewRequestBuilder.set(
                              CaptureRequest.CONTROL_AF_MODE,
                              CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                        );
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
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(
                              mPreviewRequest, null, mBackgroundHandler
                        );
                     } catch (Exception e2) {
                        e2.printStackTrace();
                     }
                  }
                  @Override
                  public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                  }
               }, mBackgroundHandler
         );
      } catch (Exception e) {
         e.printStackTrace();
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

   private class Camera2PostAction implements Runnable {
      private ImageView view;
      private Image image;
      public Camera2PostAction(ImageView _view, Image _img) {
         view = _view;
         image = _img;
      }
      @Override
      public void run() {
         if (image == null) return;
         byte[] pixels = yuv420ToNV21(image);
         image.close();
         int w = mCamera2Size.getWidth(), h = mCamera2Size.getHeight();
         Bitmap bmp = yuvToBitmap(pixels, w, h, ImageFormat.NV21);
         bmp = rotatedBitmap(bmp);
         processFrame(bmp);
      }
   }

   private byte[] yuv420ToNV21(Image imgYUV420) {
      // Converting YUV_420_888 data to YUV_420_SP (NV21).
      Rect crop = imgYUV420.getCropRect();
      int format = imgYUV420.getFormat();
      int width = crop.width();
      int height = crop.height();
      Image.Plane[] planes = imgYUV420.getPlanes();
      byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
      byte[] rowData = new byte[planes[0].getRowStride()];
      int channelOffset = 0;
      int outputStride = 1;
      for (int i = 0; i < planes.length; i++) {
         switch (i) {
            case 0:
               channelOffset = 0;
               outputStride = 1;
               break;
            case 1:
               channelOffset = width * height + 1;
               outputStride = 2;
               break;
            case 2:
               channelOffset = width * height;
               outputStride = 2;
               break;
         }
         ByteBuffer buffer = planes[i].getBuffer();
         int rowStride = planes[i].getRowStride();
         int pixelStride = planes[i].getPixelStride();
         int shift = (i == 0) ? 0 : 1;
         int w = width >> shift;
         int h = height >> shift;
         buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
         for (int row = 0; row < h; row++) {
            int length;
            if (pixelStride == 1 && outputStride == 1) {
               length = w;
               buffer.get(data, channelOffset, length);
               channelOffset += length;
            } else {
               length = (w - 1) * pixelStride + 1;
               buffer.get(rowData, 0, length);
               for (int col = 0; col < w; col++) {
                  data[channelOffset] = rowData[col * pixelStride];
                  channelOffset += outputStride;
               }
            }
            if (row < h - 1) {
               buffer.position(buffer.position() + rowStride - length);
            }
         }
      }
      return data;
   }

   @Override
   public void onImageAvailable(ImageReader imageReader) {
      if (processBusy) return;
      synchronized (processLock) {
         // XXX: many calls sleep here without notified ...
         lock();
      }
      try {
         Image image = imageReader.acquireLatestImage();
         mBackgroundHandler.post(new Camera2PostAction(getImageView(), image));
      } catch(Exception e) {
         unlock();
         e.printStackTrace();
      }
   }

   private void processFrame(Bitmap bmp) {
      BitmapFilter filter = null;
      //filter = new SobelOperator();
      //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,1,0, 0,0,0});
      //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,0,0, 0,0,1});
      //filter = new HistogramEqualizationFilter();
      handler.post(new FilterAction(this, filter, bmp));
   }

   private class FilterAction implements Runnable {

      BitmapFilter filter;
      Bitmap bmp;
      Preview2 preview;

      public FilterAction(Preview2 _preview, BitmapFilter _filter, Bitmap _bmp) {
         preview = _preview;
         filter = _filter;
         bmp = _bmp;
      }

      @Override
      public void run() {
         if (filter == null) {
            preview.getImageView().setImageBitmap(bmp);
         } else {
            preview.getImageView().setImageBitmap(filter.act(bmp));
         }
         preview.unlock();
      }
   }
}
