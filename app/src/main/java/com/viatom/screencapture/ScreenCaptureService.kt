package com.viatom.screencapture

import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

import android.R
import android.app.*
import android.app.Activity.RESULT_OK

import android.content.BroadcastReceiver
import android.content.Context

import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.os.*
import android.os.Environment.DIRECTORY_MOVIES
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import java.io.IOException


class ScreenCaptureService:Service() {

    private var mServiceHandler: ServiceHandler? = null
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var mMediaRecorder: MediaRecorder? = null
    private var resultCode = 0
    private var data: Intent? = null
    private var mScreenStateReceiver: BroadcastReceiver? = null
    var isInitialized=false

    private inner class ServiceHandler(looper: Looper?) : Handler(looper!!) {

        override fun handleMessage(msg: Message) {
            if (resultCode == RESULT_OK) {
                startRecording(resultCode, data)
            } else {
            }
        }


    }

    private fun startRecording(resultCode: Int, data: Intent?) {
        val mProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mMediaRecorder = MediaRecorder()
        val metrics = DisplayMetrics()
        val wm =
            applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(metrics)
        val mScreenDensity = metrics.densityDpi
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setVideoEncodingBitRate(8 * 1000 * 1000)
        mMediaRecorder!!.setVideoFrameRate(15)
        mMediaRecorder!!.setVideoSize(displayWidth, displayHeight)
        val videoDir =
            Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES)
                .absolutePath
        val timestamp = System.currentTimeMillis()
        var orientation = "portrait"
        if (displayWidth > displayHeight) {
            orientation = "landscape"
        }
        val filePathAndName =
            videoDir + "/time_" + timestamp.toString() + "_mode_" + orientation + ".mp4"
        mMediaRecorder!!.setOutputFile(filePathAndName)
        try {
            mMediaRecorder!!.prepare()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if(isInitialized){
            mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data!!)
            val surface: Surface = mMediaRecorder!!.surface
            mVirtualDisplay = mMediaProjection!!.createVirtualDisplay(
                "MainActivity",
                displayWidth,
                displayHeight,
                mScreenDensity,
                VIRTUAL_DISPLAY_FLAG_PRESENTATION,
                surface,
                null,
                null
            )
            mMediaRecorder!!.start()
            Log.v(TAG, "Started recording")

        }
    }

    companion object {
        private const val TAG = "RECORDERSERVICE"
        private const val EXTRA_RESULT_CODE = "resultcode"
        private const val EXTRA_DATA = "data"
        private const val ONGOING_NOTIFICATION_ID = 23

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
        createNotificationChannel()
    }


    private fun createNotificationChannel() {
        val builder: Notification.Builder = Notification.Builder(this.applicationContext) //获取一个Notification构造器
        val nfIntent = Intent(this, MainActivity::class.java) //点击后跳转的界面，可以设置跳转数据
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.sym_def_app_icon
                )
            ) // 设置下拉列表中的图标(大图标)
            //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
            .setSmallIcon(R.mipmap.sym_def_app_icon) // 设置状态栏内的小图标
            .setContentText("is running......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id",
                "notification_name",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification = builder.build() // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
        startForeground(110, notification)
    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }
}