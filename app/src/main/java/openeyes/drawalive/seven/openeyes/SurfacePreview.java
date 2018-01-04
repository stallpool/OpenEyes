package openeyes.drawalive.seven.openeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.util.List;

class SurfacePreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
   private static final String TAG = "SurfacePreview";
   public Camera mCamera;
   SurfaceHolder mHolder;
   List<Camera.Size> mSupportedPreviewSizes;

   SurfacePreview(Context context) {
      super(context);

      mHolder = getHolder();
      mHolder.addCallback(this);
      mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

      rs = RenderScript.create(context);
      yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

      img = new ImageView(context);
   }

   @Override
   protected void onLayout(boolean b, int i, int i1, int i2, int i3) {
   }

   public void setCamera(Camera camera) {
      if (mCamera == camera) { return; }
      stopPreviewAndFreeCamera();

      mCamera = camera;

      if (mCamera != null) {
         List<Camera.Size> localSizes = mCamera.getParameters().getSupportedPreviewSizes();
         mSupportedPreviewSizes = localSizes;
         requestLayout();

         try {
            mCamera.setDisplayOrientation(0);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);
         } catch (Exception e) {
            e.printStackTrace();
         }

         Log.i(TAG, String.format("%d %d", this.getWidth(), this.getHeight()));
         mCamera.startPreview();
      }
   }

   public boolean safeCameraOpen() {
      boolean qOpened = false;

      try {
         setCamera(Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK));
         qOpened = (mCamera != null);
      } catch (Exception e) {
         Log.e(TAG, "failed to open Camera");
         e.printStackTrace();
      }

      return qOpened;
   }

   public void surfaceCreated(SurfaceHolder holder) {
   }

   public void surfaceDestroyed(SurfaceHolder holder) {
      // Surface will be destroyed when we return, so stop the preview.
      stopPreviewAndFreeCamera();
   }

   public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      if (mCamera == null) return;
      Camera.Parameters parameters = mCamera.getParameters();
      parameters.setPreviewSize(this.getWidth(), this.getHeight());
      Log.i(TAG, String.format("%d %d", this.getWidth(), this.getHeight()));
      requestLayout();
      mCamera.setParameters(parameters);
      mCamera.startPreview();
   }

   public void stopPreviewAndFreeCamera() {

      if (mCamera != null) {
         // Call stopPreview() to stop updating the preview surface.
         mCamera.stopPreview();
         mCamera.release();

         mCamera = null;
      }
   }

   @Override
   public void onPreviewFrame(byte[] bytes, Camera camera) {
      Camera.Size size = camera.getParameters().getPictureSize();
      int w = size.width, h = size.height;
      if (yuvType == null)
      {
         yuvType = new Type.Builder(rs, Element.U8(rs)).setX(bytes.length);
         in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

         rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h);
         out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
      }

      in.copyFrom(bytes);

      yuvToRgbIntrinsic.setInput(in);
      yuvToRgbIntrinsic.forEach(out);

      Bitmap bmpout = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      out.copyTo(bmpout);
      img.setImageBitmap(bmpout);
   }

   private RenderScript rs;
   private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
   private Type.Builder yuvType, rgbaType;
   private Allocation in, out;
   public ImageView img;
}
