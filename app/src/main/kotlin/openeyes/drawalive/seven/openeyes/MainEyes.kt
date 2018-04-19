package openeyes.drawalive.seven.openeyes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.widget.Button
import android.widget.LinearLayout

class MainEyes : AppCompatActivity() {

    private var preview: Preview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ActivityCompat.checkSelfPermission(
                        this, Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        } else {
        }

        var button: Button
        var params: LinearLayout.LayoutParams
        val panel = LinearLayout(this)
        params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        )
        panel.layoutParams = params
        panel.orientation = LinearLayout.VERTICAL

        val btnpanel = LinearLayout(this)
        params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnpanel.layoutParams = params
        btnpanel.orientation = LinearLayout.HORIZONTAL
        button = Button(this)
        button.text = "Start"
        button.setOnClickListener { preview!!.safeCameraOpen() }
        btnpanel.addView(button)
        button = Button(this)
        button.text = "Stop"
        button.setOnClickListener { preview!!.stopPreview() }
        btnpanel.addView(button)
        panel.addView(btnpanel)

        val camerabtn = LinearLayout(this)
        params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
        camerabtn.layoutParams = params
        camerabtn.orientation = LinearLayout.HORIZONTAL
        button = Button(this)
        button.text = "R+-0"
        button.setOnClickListener { preview!!.rotateCamera(0) }
        camerabtn.addView(button)
        button = Button(this)
        button.text = "R+90"
        button.setOnClickListener { preview!!.rotateCamera(90) }
        camerabtn.addView(button)
        button = Button(this)
        button.text = "R-90"
        button.setOnClickListener { preview!!.rotateCamera(-90) }
        camerabtn.addView(button)
        button = Button(this)
        button.text = "R180"
        button.setOnClickListener { preview!!.rotateCamera(180) }
        camerabtn.addView(button)
        panel.addView(camerabtn)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview = Preview2(this)
        } else {
            preview = Preview1(this)
        }
        panel.addView(preview!!.imageView)
        this.setContentView(panel)

        // ref: https://developer.android.com/training/scheduling/wakelock.html
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    override fun onPause() {
        super.onPause()
        preview!!.stopPreviewAndFreeCamera()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        preview!!.stopPreviewAndFreeCamera()
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
