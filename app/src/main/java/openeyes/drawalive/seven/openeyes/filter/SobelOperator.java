package openeyes.drawalive.seven.openeyes.filter;


import android.graphics.Bitmap;

public class SobelOperator implements BitmapFilter {
   public SobelOperator() {
      setSobelKernel(
            new double[] {-1,0,1, -2,0,2, -1,0,1},
            new double[] {-1,-2,-1, 0,0,0, 1,2,1}
      );
   }

   public void setSobelKernel(double[] kernelX, double[] kernelY) {
      kernel_x = kernelX;
      kernel_y = kernelY;
   }

   @Override
   public Bitmap act(Bitmap bmp) {
      int w = bmp.getWidth(), h = bmp.getHeight(), wh = w*h;
      int[] pixels = new int[wh];
      int[] px = new int[wh], py = new int[wh];
      bmp.getPixels(pixels, 0, w, 0, 0, w, h);
      Utils.greyScale(pixels);
      System.arraycopy(pixels, 0, px, 0, pixels.length);
      System.arraycopy(pixels, 0, py, 0, pixels.length);
      Utils.convolve3x3(px, w, h, kernel_x);
      Utils.convolve3x3(py, w, h, kernel_y);
      for (int i = pixels.length-1; i >= 0; i--) {
         double x = px[i], y = py[i];
         double color = Math.sqrt(x*x+y*y);
         if (color > 255) color = 255;
         pixels[i] = (int)color;
      }
      Utils.greyRGB(pixels);
      bmp.setPixels(pixels, 0, w, 0, 0, w, h);
      return bmp;
   }

   protected double[] kernel_x, kernel_y;
}
