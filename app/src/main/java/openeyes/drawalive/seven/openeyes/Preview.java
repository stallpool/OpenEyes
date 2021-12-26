package openeyes.drawalive.seven.openeyes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.Matrix;
import android.graphics.YuvImage;
import android.os.Build;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

public abstract class Preview {
   protected static final String TAG = "Preview";

   public Preview(Context context) {
      cameraRotate = null;
      imageView = new ImageView(context);
      handler = new Handler();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
         rs = RenderScript.create(context);
         yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
      } else {
         rs = null;
         yuvToRgbIntrinsic = null;
      }
   }

   public ImageView getImageView() {
      return imageView;
   };

   protected Bitmap yuvToBitmap(byte[] bytes, int w, int h, int format) {
      if (rs == null) {
         if (format != ImageFormat.NV21) {
            Log.e(TAG, "Not support image format (not NV21)");
            return null;
         }
         return yuvToBitmapCommon(bytes, w, h);
      } else {
         return yuvToBitmapFaster(bytes, w, h);
      }
   }

   protected Bitmap yuvToBitmapFaster(byte[] bytes, int w, int h) {
      if (yuvType == null) {
         yuvType = new Type.Builder(rs, Element.U8(rs)).setX(bytes.length);
         in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
         rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(w).setY(h);
         out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
      }
      in.copyFrom(bytes);
      yuvToRgbIntrinsic.setInput(in);
      yuvToRgbIntrinsic.forEach(out);
      Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
      out.copyTo(bmp);
      return bmp;
   }

   protected Bitmap yuvToBitmapCommon(byte[] bytes, int w, int h) {
      try {
         YuvImage img = new YuvImage(bytes, ImageFormat.NV21, w, h, null);
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         img.compressToJpeg(
               new Rect(0, 0, img.getWidth(), img.getHeight()), 100, out
         );
         byte[] imageBytes = out.toByteArray();
         out.close();
         return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
      } catch (Exception e) {
         return null;
      }
   }

   public void rotateCamera(int degree) {
      switch(degree) {
         case 90:
            cameraRotate = new Matrix();
            cameraRotate.postRotate(90);
            break;
         case -90:
            cameraRotate = new Matrix();
            cameraRotate.postRotate(-90);
            break;
         case 180:
            cameraRotate = new Matrix();
            cameraRotate.postRotate(180);
            break;
         default:
            cameraRotate = null;
      }
   }

   protected Bitmap rotatedBitmap(Bitmap bmp) {
      if (cameraRotate == null) return bmp;
      return Bitmap.createBitmap(
            bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), cameraRotate, true
      );
   }

   public abstract boolean safeCameraOpen();
   public abstract void stopPreview();
   public abstract void stopPreviewAndFreeCamera();

   protected RenderScript rs;
   protected ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
   protected Type.Builder yuvType, rgbaType;
   protected Allocation in, out;
   protected ImageView imageView;
   protected Handler handler;
   protected Matrix cameraRotate;
}
