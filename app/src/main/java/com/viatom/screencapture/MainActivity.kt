package com.viatom.screencapture

import android.app.Activity
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import com.viatom.screencapture.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private val REQUEST_MEDIA_PROJECTION = 1
    private val STATE_RESULT_CODE = "result_code"
    private val STATE_RESULT_DATA = "result_data"

    private var mResultCode = 0
    lateinit var mResultData: Intent
    private var mScreenDensity = 0
    lateinit var binding:ActivityMainBinding

    private var mVirtualDisplay: VirtualDisplay? = null



    private fun tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }

    private fun startScreenCapture() {
        val activity: Activity = this

        if (mMediaProjection != null) {
            setUpVirtualDisplay()
        } else if (mResultCode != 0 && mResultData != null) {
            setUpMediaProjection()
            setUpVirtualDisplay()
        } else {
            Log.i(
              "fuck",
                "Requesting confirmation"
            )
            // This initiates a prompt dialog for the user to confirm screen projection.
            startActivityForResult(
                mMediaProjectionManager!!.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            )
        }
    }

    private fun setUpVirtualDisplay() {
        Log.i(
           "fuck",
            "Setting up a VirtualDisplay: " +
                    binding.surface.getWidth() + "x" +binding.surface.height +
                    " (" + mScreenDensity + ")"
        )
        mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
            "ScreenCapture",
           binding.surface.width, binding.surface.height, mScreenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            binding.surface.holder.surface, null, null
        )
       // mButtonToggle.setText(R.string.stop)
    }

    private fun stopScreenCapture() {
        if (mVirtualDisplay == null) {
            return
        }
        mVirtualDisplay?.release()
        mVirtualDisplay = null
      //  mButtonToggle.setText(R.string.start)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this,ScreenCaptureService::class.java))
        if (savedInstanceState != null) {
            mResultCode =
                savedInstanceState.getInt(STATE_RESULT_CODE)
            mResultData =
                savedInstanceState.getParcelable(STATE_RESULT_DATA)!!
        }
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val metrics = DisplayMetrics()
        getWindowManager().getDefaultDisplay().getMetrics(metrics)
        mScreenDensity = metrics.densityDpi
        mMediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager?

        binding.fuck.setOnClickListener {
            startScreenCapture()
        }
    }


    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        if (mResultData != null) {
            outState.putInt(
            STATE_RESULT_CODE,
                mResultCode
            )
            outState.putParcelable(
               STATE_RESULT_DATA,
                mResultData
            )
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != RESULT_OK) {
                Log.i("com.example.android.screencapture.ScreenCaptureFragment.TAG", "User cancelled")
                Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show()
                return
            }
            val activity: Activity = this
            Log.i(
               " com.example.android.screencapture.ScreenCaptureFragment.TAG",
                "Starting screen capture"
            )
            mResultCode = resultCode
            if (data != null) {
                mResultData = data
            }
            setUpMediaProjection()
            setUpVirtualDisplay()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScreenCapture()
    }

    override fun onDestroy() {
        super.onDestroy()
        tearDownMediaProjection()
    }
}