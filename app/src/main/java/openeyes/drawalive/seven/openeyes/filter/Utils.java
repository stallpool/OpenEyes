package openeyes.drawalive.seven.openeyes.filter;

public class Utils {
   public static void greyScale(int[] pixels) {
      double s = 1.0/3.0;
      greyScale(pixels, s, s, s);
   }

   public static void greyScale(int[] pixels, double sr, double sg, double sb) {
      for(int i = pixels.length-1; i >= 0; i--) {
         int color = pixels[i];
         int r = (color >> 16) & 0xff;
         int g = (color >> 8) & 0xff;
         int b = color & 0xff;
         pixels[i] = (int)(r*sr + g*sg + b*sb);
      }
   }

   public static void greyRGB(int[] pixels) {
      for (int i = pixels.length-1; i >= 0; i--) {
         int grey = pixels[i] & 0xff;
         pixels[i] = (grey << 16) | (grey << 8) | grey | (0xff << 24);
      }
   }

   public static void convolve3x3(int[] pixels, int w, int h, double[] kernel) {
      int[] r = new int[pixels.length];
      for (int j = h-2; j >= 1; j--) {
         int offset = j*w;
         for (int i = w-2; i >= 1; i--) {
            int offseti = offset + i;
            double color = (
               kernel[0] * pixels[offseti-w-1] +
               kernel[1] * pixels[offseti-w] +
               kernel[2] * pixels[offseti-w+1] +
               kernel[3] * pixels[offseti-1] +
               kernel[4] * pixels[offseti] +
               kernel[5] * pixels[offseti+1] +
               kernel[6] * pixels[offseti+w-1] +
               kernel[7] * pixels[offseti+w] +
               kernel[8] * pixels[offseti+w+1]
            );
            //if (color < 0) color = -color;
            //if (color > 255) color = 255;
            r[offseti] = (int)color;
         }
      }
      System.arraycopy(r, 0, pixels, 0, pixels.length);
   }
}
