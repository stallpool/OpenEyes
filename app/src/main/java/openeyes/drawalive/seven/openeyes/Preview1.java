package openeyes.drawalive.seven.openeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.util.Log;

import java.util.List;

import openeyes.drawalive.seven.openeyes.filter.BitmapFilter;
import openeyes.drawalive.seven.openeyes.filter.Kernel3x3Filter;
import openeyes.drawalive.seven.openeyes.filter.SobelOperator;

import static android.content.Context.CAMERA_SERVICE;

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
class Preview1 extends Preview implements Camera.PreviewCallback {
   Preview1(Context context) {
      super(context);
   }

   @Override
   public boolean safeCameraOpen() {
      boolean qOpened = false;
      try {
         setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT));
         qOpened = (mCamera != null);
      } catch (Exception e) {
         try {
            setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK));
            qOpened = (mCamera != null);
         } catch (Exception e2) {
            Log.e(TAG, "failed to open Camera");
            e.printStackTrace();
         }
      }
      return qOpened;
   }

   @Override
   public void stopPreview() {
      if (mCamera == null) return;
      mCamera.stopPreview();
   }

   @Override
   public void stopPreviewAndFreeCamera() {
      if (mCamera != null) {
         mCamera.stopPreview();
         mCamera.release();
         mCamera = null;
      }
   }

   protected Camera mCamera;
   private List<Camera.Size> mSupportedPreviewSizes;

   private void setCamera(Camera camera) {
      if (mCamera == camera) { return; }
      stopPreviewAndFreeCamera();

      mCamera = camera;

      if (mCamera != null) {
         List<Camera.Size> localSizes = mCamera.getParameters().getSupportedPreviewSizes();
         mSupportedPreviewSizes = localSizes;

         try {
            mCamera.setDisplayOrientation(0);
            mCamera.setPreviewCallback(this);
         } catch (Exception e) {
            e.printStackTrace();
         }
         mCamera.startPreview();
      }
   }

   @Override
   public void onPreviewFrame(byte[] bytes, Camera camera) {
      Camera.Parameters params = camera.getParameters();
      Camera.Size size = params.getPictureSize();
      int w = size.width, h = size.height;
      Bitmap bmp = yuvToBitmap(bytes, w, h, params.getPreviewFormat());
      bmp = rotatedBitmap(bmp);
      processFrame(bmp);
   }

   private void processFrame(Bitmap bmp) {
      BitmapFilter filter = null;
      filter = new SobelOperator();
      //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,1,0, 0,0,0});
      //filter = new Kernel3x3Filter(new double[]{-1,0,0, 0,0,0, 0,0,1});
      //filter = new HistogramEqualizationFilter();
      handler.post(new FilterAction(this, filter, bmp));
   }

   private class FilterAction implements Runnable {

      BitmapFilter filter;
      Bitmap bmp;
      Preview1 preview;

      public FilterAction(Preview1 _preview, BitmapFilter _filter, Bitmap _bmp) {
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
      }
   }
}
