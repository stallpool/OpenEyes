package openeyes.drawalive.seven.openeyes.filter;

import android.graphics.Bitmap;

public class Kernel3x3Filter implements BitmapFilter {
   private double[] kernel;
   public Kernel3x3Filter(double[] _kernel) {
      kernel = _kernel;
   }

   @Override
   public Bitmap act(Bitmap bmp) {
      int w = bmp.getWidth(), h = bmp.getHeight(), wh = w*h;
      int[] pixels = new int[wh];
      bmp.getPixels(pixels, 0, w, 0, 0, w, h);
      // grey scale
      Utils.greyScale(pixels);
      // kernel scale
      Utils.convolve3x3(pixels, w, h, kernel);
      // grep -> r,g,b
      Utils.greyRGB(pixels);
      bmp.setPixels(pixels, 0, w, 0, 0, w, h);
      return bmp;
   }
}
