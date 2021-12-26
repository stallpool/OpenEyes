package openeyes.drawalive.seven.openeyes;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;


public class MainEyes extends AppCompatActivity {

   // Used to load the 'native-lib' library on application startup.
   static {
      System.loadLibrary("native-lib");
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      selectedCameraId = "0";

      if(ActivityCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
      ) != PackageManager.PERMISSION_GRANTED) {
         ActivityCompat.requestPermissions(this, new String[]{
               Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
         }, 1);
      } else { }

      LinearLayout.LayoutParams params;
      LinearLayout panel = new LinearLayout(this);
      params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
      );
      panel.setLayoutParams(params);
      panel.setOrientation(LinearLayout.VERTICAL);

      LinearLayout btnpanel = new LinearLayout(this);
      params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
      );
      btnpanel.setLayoutParams(params);
      btnpanel.setOrientation(LinearLayout.HORIZONTAL);
      Button button;
      button = new Button(this);
      button.setText("Start");
      button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            preview.safeCameraOpen(selectedCameraId);
         }
      });
      btnpanel.addView(button);
      button = new Button(this);
      button.setText("Stop");
      button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            preview.stopPreview();
         }
      });
      btnpanel.addView(button);
      panel.addView(btnpanel);

      btnpanel = new LinearLayout(this);
      params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
      );
      btnpanel.setLayoutParams(params);
      btnpanel.setOrientation(LinearLayout.HORIZONTAL);
      button = new Button(this);
      button.setText("R+-0");
      button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            preview.rotateCamera(0);
         }
      });
      btnpanel.addView(button);
      button = new Button(this);
      button.setText("R+90");
      button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            preview.rotateCamera(90);
         }
      });
      btnpanel.addView(button);
      button = new Button(this);
      button.setText("R-90");
      button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            preview.rotateCamera(-90);
         }
      });
      btnpanel.addView(button);
      button = new Button(this);
      button.setText("R180");
      button.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            preview.rotateCamera(180);
         }
      });
      btnpanel.addView(button);
      panel.addView(btnpanel);

      btnpanel = new LinearLayout(this);
      params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
      );
      btnpanel.setLayoutParams(params);
      btnpanel.setOrientation(LinearLayout.HORIZONTAL);
      CameraSpinner spinner = new CameraSpinner(this);
      spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
         @Override
         public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            selectedCameraId = (String)spinner.getItemAtPosition(position);
         }

         @Override
         public void onNothingSelected(AdapterView<?> parent) {
            selectedCameraId = "0";
         }
      });
      btnpanel.addView(spinner);
      panel.addView(btnpanel);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
         preview = new Preview2(this);
      } else {
         preview = new Preview1(this);
      }
      panel.addView(preview.getImageView());
      this.setContentView(panel);

      // ref: https://developer.android.com/training/scheduling/wakelock.html
      // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
   }

   @Override
   protected void onPause() {
      super.onPause();
      preview.stopPreviewAndFreeCamera();
   }

   @Override
   protected void onResume() {
      super.onResume();
   }

   @Override
   protected void onDestroy() {
      super.onDestroy();
      preview.stopPreviewAndFreeCamera();
   }

   private native String stringFromJNI();

   private Preview preview;
   private String selectedCameraId;
}
