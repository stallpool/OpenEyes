package openeyes.drawalive.seven.openeyes;

import static android.content.Context.CAMERA_SERVICE;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

import androidx.appcompat.widget.AppCompatSpinner;

public class CameraSpinner extends AppCompatSpinner {
   public CameraSpinner(Context context) {
      super(context);
      mCameraMgr = (CameraManager) context.getSystemService(CAMERA_SERVICE);
      try {
         String[] list = mCameraMgr.getCameraIdList();
         ArrayAdapter<String> adapter = new ArrayAdapter<String>(
               context, android.R.layout.simple_spinner_dropdown_item, list
         );
         adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
         setAdapter(adapter);
         LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT,
               LinearLayout.LayoutParams.WRAP_CONTENT
         );
         setLayoutParams(params);
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   protected CameraManager mCameraMgr;
}
