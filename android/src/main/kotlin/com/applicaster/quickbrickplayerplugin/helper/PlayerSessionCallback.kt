package com.applicaster.quickbrickplayerplugin.helper

import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.ImageButton
import com.applicaster.quickbrickplayerplugin.R
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView


/**
 * Allows interaction with media controllers, volume keys, media buttons, and
 * transport controls.
 */

class PlayerSessionCallback(
    val player: SimpleExoPlayer,
    val playerView: PlayerView,
    val hideController:() -> Unit
) : MediaSessionCompat.Callback() {

    private val TAG: String = "MediaReceiver"
    private val tenSecondTime = 10000.toLong()

    override fun onStop() {
        super.onStop()
        player.release()
    }

    override fun onPlay() {
        super.onPlay()
        when (player.playWhenReady) {
            true -> player.playWhenReady = false
            else -> player.playWhenReady = true
        }
        setFocus(R.id.exo_play_pause)
        Log.d(TAG, "onPlay " + player.playWhenReady.toString())
    }

    override fun onPause() {
        super.onPause()
        player.playWhenReady = true
        Log.d(TAG, "onPause")
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        Log.d(TAG, "onSkipToPrevious")
    }

    override fun onRewind() {
        super.onRewind()
        setFocus(R.id.exo_rew)
        val data = player.currentPosition.minus(tenSecondTime)
        player.seekTo(data)
        Log.d(TAG, "onRewind")
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        Log.d(TAG, "onSeekTo")
    }

    /***
     *   Issue with Fast Forward
     */
    //TODO: find out if video is able to fast forward
    override fun onFastForward() {
        super.onFastForward()
        setFocus(R.id.exo_ffwd)
        val data = player.currentPosition.plus(tenSecondTime)
        if (data < player.duration) {
            player.seekTo(data)
        }
        Log.d(TAG, "onFastForward")
    }

    /**
     *  method to set focus on play, rewind, and fast forward
     */
    private fun setFocus(exoPlayPause: Int) {
        playerView.findViewById<ImageButton>(exoPlayPause)?.requestFocus()
        hideController()
    }
}