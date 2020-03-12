package com.kaltura.player.sampleapp

import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

import android.widget.ProgressBar
import com.applicaster.quickbrickplayerplugin.QuickBrickDefaultPlayerView
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.PKPluginConfigs
import com.kaltura.playkit.PlayKitManager

import com.kaltura.tvplayer.KalturaPlayer
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), OrientationManager.OrientationListener{

    private var isOnBackground: Boolean = false
    private val log = PKLog.get("MainActivity")

    private lateinit var progressBar: ProgressBar
    private lateinit var mOrientationManager: OrientationManager
    private lateinit var player : QuickBrickDefaultPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOrientationManager = OrientationManager(this, SensorManager.SENSOR_DELAY_NORMAL, this)
        mOrientationManager.enable()
        setContentView(R.layout.activity_main)


        player = QuickBrickDefaultPlayerView(this)
        val container = activity_main
        container.addView(player?.getPlayerView())
        log.i("PlayKitManager: " + PlayKitManager.CLIENT_TAG)
    }

    fun loadKalturaPlayer(mediaPartnerId: Int?, playerType: KalturaPlayer.Type, pkPluginConfigs: PKPluginConfigs) {


    }

    private fun initProgressBar() {
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE
    }

    override fun onOrientationChange(screenOrientation: OrientationManager.ScreenOrientation) {
        when (screenOrientation) {
            OrientationManager.ScreenOrientation.PORTRAIT -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationManager.ScreenOrientation.REVERSED_PORTRAIT -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            OrientationManager.ScreenOrientation.LANDSCAPE -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationManager.ScreenOrientation.REVERSED_LANDSCAPE -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            else -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    override fun onResume() {
        super.onResume()
        if (player != null && isOnBackground)  {
            isOnBackground = false
            player?.onApplicationResume()
            player.player?.play()
        }
    }

    override fun onPause() {
        isOnBackground = true
        if (player != null && player.getCurrentTime() > 0) {
            player?.onApplicationPause()
        }
        super.onPause()
    }

     override fun onDestroy() {
        if (player != null) {
           player.onDestroy()
        }
        super.onDestroy()
    }


}
