package openeyes.drawalive.seven.openeyes.filter;

import android.graphics.Bitmap;

public class HistogramEqualizationFilter implements BitmapFilter {
   @Override
   public Bitmap act(Bitmap bmp) {
      int w = bmp.getWidth(), h = bmp.getHeight(), wh = w*h;
      int[] pixels = new int[wh];
      bmp.getPixels(pixels, 0, w, 0, 0, w, h);
      int[] histr = new int[256], histg = new int[256], histb = new int[256];
      for (int i = pixels.length-1; i >= 0; i--) {
         int color = pixels[i];
         int r = (color >> 16) & 0xff;
         int g = (color >> 8) & 0xff;
         int b = color & 0xff;
         histr[r] ++; histg[g] ++; histb[b] ++;
      }
      for (int i = 1; i < 256; i++) {
         histr[i] += histr[i-1];
         histg[i] += histg[i-1];
         histb[i] += histb[i-1];
      }
      for (int i = 1; i < 256; i++) {
         histr[i] = (int)(255.0*histr[i]/wh);
         histg[i] = (int)(255.0*histg[i]/wh);
         histb[i] = (int)(255.0*histb[i]/wh);
      }
      for (int i = pixels.length-1; i >= 0; i--) {
         int color = pixels[i];
         int r = (color >> 16) & 0xff;
         int g = (color >> 8) & 0xff;
         int b = color & 0xff;
         int a = (color >> 24) & 0xff;
         r = histr[r];
         g = histg[g];
         b = histb[b];
         pixels[i] = (a<<24) | (r<<16) | (g<<8) | b;
      }
      bmp.setPixels(pixels, 0, w, 0, 0, w, h);
      return bmp;
   }
}
