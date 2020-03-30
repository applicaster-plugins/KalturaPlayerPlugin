package com.applicaster.quickbrickplayerplugin

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.applicaster.atom.model.APAtomEntry
import com.applicaster.atom.model.APAtomEntry.APAtomEntryPlayable
import com.applicaster.model.APURLPlayable
import com.applicaster.model.APVodItem
import com.applicaster.plugin_manager.dependencyplugin.playerplugin.PlayerSenderPlugin
import com.applicaster.plugin_manager.playersmanager.Playable
import com.applicaster.quickbrickplayerplugin.helper.PlayerSessionCallback
import com.applicaster.quickbrickplayerplugin.helper.ZappFonts
import com.applicaster.quickbrickplayerplugin.helper.setFocusableBg
import com.applicaster.quickbrickplayerplugin.helper.setFocusableIcon
import com.applicaster.quickbrickplayerplugin.quickbrickInterface.QuickBrickPlayer
import com.applicaster.reactnative.utils.DataUtils
import com.applicaster.session.SessionStorage
import com.applicaster.util.AppData
import com.applicaster.util.OSUtil
import com.applicaster.util.serialization.SerializationUtils
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.security.ProviderInstaller
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.kaltura.android.exoplayer2.ui.DefaultTimeBar
import com.kaltura.android.exoplayer2.ui.TimeBar
import com.kaltura.playkit.PKLog
import com.kaltura.playkit.PlayerEvent
import com.kaltura.playkit.PlayerEvent.*
import com.kaltura.playkit.player.MediaSupport
import com.kaltura.tvplayer.KalturaOvpPlayer
import com.kaltura.tvplayer.KalturaPlayer
import com.kaltura.tvplayer.OVPMediaOptions
import com.kaltura.tvplayer.PlayerInitOptions
import com.kaltura.tvplayer.config.TVPlayerParams
import kotlinx.android.synthetic.main.player_control_view.view.*
import org.json.JSONException
import org.json.JSONObject
import java.util.*


class QuickBrickDefaultPlayerView(context: Context?) :
        FrameLayout(context),
        QuickBrickPlayer,
        Callback,
        PlayerSenderPlugin {
    private val log = PKLog.get("QuickBrickDefaultPlayerView")

    var player: KalturaPlayer? = null
    var playerView: com.kaltura.android.exoplayer2.ui.PlayerView
    var view: View
    val timer = Timer()
    private var currentState: String? = null

    var subtitles_off = false
    var isDelayRunning = false
    private lateinit var mediaSession: MediaSessionCompat
    private var myhandler: Handler
    private lateinit var runnable: Runnable

    private val playerSeekTime = 10000.toLong()
    private val delayInMs = 5000

    override lateinit var entry: Map<String, Any>
    override var pluginPlayerContainer: Any? = null
    override lateinit var playerObject: Any
    override lateinit var senderView: View
    override lateinit var senderMediaSource: Any
    override lateinit var senderAdsUrl: String

    override fun pluggablePlayerPause() {}
    override fun pluggablePlayerResume() {}
    override fun playbackPosition(): Long {
        return playerSeekTime
    }

    override fun playbackDuration(): Long {
        return playerSeekTime
    }

    init {
        view = OSUtil.getLayoutInflater(context).inflate(R.layout.video_view, null)
        this.addView(view)
        playerView = findViewById(R.id.video_view)

        myhandler = Handler()

        runnable = Runnable {
            kotlin.run {
                if (OSUtil.isTv()) {
                    playerView.player_controller.alpha = 0f
                }
                isDelayRunning = true
            }
        }

        senderView = view

        initializePlayer()
        initializeMediaSession(view)

        val btnRewind = playerView.findViewById<ImageButton>(R.id.exo_rew)
        val btnForward = playerView.findViewById<ImageButton>(R.id.exo_ffwd)

        playerView.findViewById<ImageButton>(R.id.back_button)?.setFocusableIcon(context!!, R.drawable.back, R.color.focused_icon_color, R.color.unfocused_icon_color)
        btnRewind.setFocusableIcon(context!!, R.drawable.rewind, R.color.focused_icon_color, R.color.unfocused_icon_color)
        btnForward.setFocusableIcon(context!!, R.drawable.forwards, R.color.focused_icon_color, R.color.unfocused_icon_color)
        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.setFocusableIcon(context!!, R.drawable.pause, R.color.focused_icon_color, R.color.unfocused_icon_color)
        playerView.findViewById<ImageButton>(R.id.subtitles_button)?.setFocusableIcon(context!!, if (subtitles_off) {
            R.drawable.subtitles_off
        } else {
            R.drawable.subtitles
        }, R.color.focused_icon_color, R.color.unfocused_icon_color)

        playerView.findViewById<ImageButton>(R.id.back_button)?.setFocusableBg(context!!, R.drawable.selected, R.drawable.unselected, R.color.focused_bg_color, R.color.unfocused_bg_color)
        btnRewind.setFocusableBg(context!!, R.drawable.selected, R.drawable.unselected, R.color.focused_bg_color, R.color.unfocused_bg_color)
        btnForward.setFocusableBg(context!!, R.drawable.selected, R.drawable.unselected, R.color.focused_bg_color, R.color.unfocused_bg_color)
        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.setFocusableBg(context!!, R.drawable.selected, R.drawable.unselected, R.color.focused_bg_color, R.color.unfocused_bg_color)
        playerView.findViewById<ImageButton>(R.id.subtitles_button)?.setFocusableBg(context!!, R.drawable.selected, R.drawable.unselected, R.color.focused_bg_color, R.color.unfocused_bg_color)

        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.setOnClickListener {
            val isPlaying = player?.isPlaying ?: false;
            if (isPlaying) {
                player?.pause()
            } else {
                player?.play()
            }
        }
        playerView.findViewById<ImageButton>(R.id.back_button)?.setOnClickListener { backButtonPressed() }

        playerView.findViewById<ImageButton>(R.id.subtitles_button)
                ?.setOnClickListener { keyboard ->
                    subtitles_off = !subtitles_off
                    //player.changeTrack()
                    playerView.findViewById<ImageButton>(R.id.subtitles_button)?.setFocusableIcon(context!!, if (subtitles_off) {
                        R.drawable.subtitles_off
                    } else {
                        R.drawable.subtitles
                    }, R.color.focused_icon_color, R.color.unfocused_icon_color)
                }
        playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)?.addListener(object : TimeBar.OnScrubListener {
            override fun onScrubMove(timeBar: TimeBar, position: Long) {
                playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)?.exo_position?.text = position.toString()
            }

            override fun onScrubStart(timeBar: TimeBar, position: Long) {
                playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)?.exo_position?.text = position.toString()
            }

            override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
                playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)?.exo_position?.text = position.toString()
            }

        })

        if (!SessionStorage.getAll("QuickBrickPlayerPlugin").isNullOrBlank()) {
            ZappFonts(playerView, view.context)
        } else {
            setDefaultFont(playerView.player_title, R.font.roboto_bold, 60.0f)
            setDefaultFont(playerView.player_description, R.font.roboto, 25.0f)
        }


        btnRewind.setOnClickListener {
            val data = player?.currentPosition?.minus(playerSeekTime)
            if (data != null) {
                playerSeekTo(data)
            }

        }

        btnForward.setOnClickListener {
            val data = player?.currentPosition?.plus(playerSeekTime)
            if (data != null) {
                playerSeekTo(data)
            }
        }

        setPlayerViewPlayFastRewind()
    }

    private fun playerSeekTo(time: Long) {
        onSeek(player?.currentPosition?.toDouble(), time)
        //player?.pause()
        player?.seekTo(time)
        //player?.play()
    }

    private fun setDefaultFont(playerItem: TextView, montserratBold: Int, float: Float) {
        playerItem.typeface = ResourcesCompat.getFont(view.context, montserratBold)
        playerItem.setTextColor(Color.parseColor(("#EFEFEF")))
        playerItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, float)

    }

    override fun getPlayerView(): View {
        return this
    }

    private fun initDrm() {
        MediaSupport.initializeDrm(context) { supportedDrmSchemes, provisionPerformed, provisionError ->
            if (provisionPerformed) {
                if (provisionError != null) {
                    log.e("DRM Provisioning failed", provisionError)
                } else {
                    log.d("DRM Provisioning succeeded")
                }
            }
            log.d("DRM initialized; supported: $supportedDrmSchemes")

            // Now it's safe to look at `supportedDrmSchemes`
        }

        try {
            ProviderInstaller.installIfNeeded(context)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }

    public fun initializePlayer() {
        initDrm()
        val playerInitOptions = PlayerInitOptions(PARTNER_ID)
        playerInitOptions.setAutoPlay(true)

        if (PARTNER_ID == 2222401  /*2657331*/) {
            val ovpTVPlayerParams = TVPlayerParams()
            ovpTVPlayerParams.analyticsUrl = "https://analytics.kaltura.com"
            ovpTVPlayerParams.partnerId = 2222401 //2657331
            ovpTVPlayerParams.serviceUrl = "https://cdnapisec.kaltura.com"
            playerInitOptions?.tvPlayerParams = ovpTVPlayerParams
        }

        player = KalturaOvpPlayer.create(context, playerInitOptions)
        player?.setPlayerView(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        val container = playerView
        container.addView(player?.playerView)
        playerView.showController()

        addPlayerStateListener()
        val ovpMediaOptions = buildOvpMediaOptions()
        player?.loadMedia(ovpMediaOptions) { entry, loadError ->
            if (loadError != null) {
                Snackbar.make(findViewById(android.R.id.content), loadError.message, Snackbar.LENGTH_LONG).show()
            } else {
                log.d("OVPMedia onEntryLoadComplete  entry = " + entry.id)
            }
        }
    }

    private fun addPlayerStateListener() {
        player?.addListener("Plugin", PlayerEvent.stateChanged) { event ->
            log.d("State changed from " + event.oldState + " to " + event.newState)
            currentState = event.newState.name
            val isPlaying = player?.isPlaying ?: false
            if (isPlaying) {
                playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                    exo_play_pause.setFocusableIcon(context!!, R.drawable.pause, R.color.focused_icon_color, R.color.unfocused_icon_color)
                }
                //onPlaybackRateChange(player?.currentPosition!!)
            } else {
                playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                    exo_play_pause.setFocusableIcon(context!!, R.drawable.play, R.color.focused_icon_color, R.color.unfocused_icon_color)
                }
            }
//            when (event.newState) {
//                PlayerState.IDLE -> videoIdle()
//                PlayerState.BUFFERING -> videoBuffering()
//                PlayerState.READY -> videoReady()
//            }
        }


        player?.addListener("Plugin", PlayerEvent.play) { event ->
            log.d("play")
            playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                exo_play_pause.setFocusableIcon(context!!, R.drawable.pause, R.color.focused_icon_color, R.color.unfocused_icon_color)
            }
        }


        player?.addListener("Plugin", PlayerEvent.pause) { event ->
            log.d("pause")
            playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                exo_play_pause.setFocusableIcon(context!!, R.drawable.play, R.color.focused_icon_color, R.color.unfocused_icon_color)
            }
        }
        player?.addListener("Plugin", PlayerEvent.sourceSelected) { event ->
            log.d("sourceSelected = " + event.source)
        }

        player?.addListener("Plugin", PlayerEvent.playing) { event ->
            log.d("playing")
            //videoReady()
            isBuffering(false)
        }

        player?.addListener("Plugin", PlayerEvent.loadedMetadata) { event ->
            log.d("loadedMetadata")
        }

        player?.addListener("Plugin", PlayerEvent.ended) { event ->
            log.d("ended")
            videoFinished()
        }

        player?.addListener("Plugin", PlayerEvent.seeked) { event ->
            var currPos = player?.currentPosition ?: 0
            log.d("seeked: " + currPos)
        }

        player?.addListener("Plugin", PlayerEvent.playbackInfoUpdated) { event ->
            log.d("playbackInfoUpdated: videoBitrate = " + event.playbackInfo.videoBitrate)
        }

        player?.addListener("Plugin", PlayerEvent.error) { event ->
            log.d("error: " + event.error.message)
            onError(event.error.message.toString(), Exception(event.error.exception))
        }

        player?.addListener("Plugin", tracksAvailable) { event: TracksAvailable ->
            //When the track data available, this event occurs. It brings the info object with it.
            var tracksInfo = event.tracksInfo
//            if (player != null) {
//                player!!.changeTrack(tracksInfo.getVideoTracks().get(0).getUniqueId())
//            }
        }

        player?.addListener("Plugin", videoTrackChanged) { event: VideoTrackChanged ->
            //When the track data available, this event occurs. It brings the info object with it.
            val track = event.newTrack
            log.d("videoTrackChanged getBitrate= " + track.bitrate)
        }

        player?.addListener("Plugin", textTrackChanged) { event: TextTrackChanged ->
            //When the track data available, this event occurs. It brings the info object with it.
            val track = event.newTrack
            log.d("textTrackChanged " + track.language + "-" + track.label)
        }

        player?.addListener("Plugin", audioTrackChanged) { event: AudioTrackChanged ->
            //When the track data available, this event occurs. It brings the info object with it.
            val track = event.newTrack
            log.d("audioTrackChanged " + track.language + "-" + track.label)
        }

    }

    private fun buildOvpMediaOptions(): OVPMediaOptions {
        val ovpMediaOptions = OVPMediaOptions()
        ovpMediaOptions.entryId = ENTRY_ID
        ovpMediaOptions.ks = null //"djJ8MjY1NzMzMXyx46jHBnPfPXQsQA0WfymgkxMsXnJdSU1ihXbo9FsLUQX5DbnnIUAdTBZ-4d9Zc3C6Vg_1Qh3b3EpJMaMf5-_rTRlKC4nUvCdoHVOkTHewFFzCYOWCB0xH2ZgKNaKf4jvtaTi7A9sujFQ4o0OKaoYP"
        ovpMediaOptions.startPosition = 0L

        return ovpMediaOptions
    }

    companion object {
        val PARTNER_ID = 2222401 //2657331 //2215841
        val ENTRY_ID = "1_f93tepsn "//"1_w5gbe2za"
        val SERVER_URL = "https://cdnapisec.kaltura.com"
    }

    override fun getCurrentTime() :Long {
        return player?.currentPosition ?: 0
    }

    override fun setPlayableItem(source: ReadableMap) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        Log.d("avitest","avittest")

        val playable = getPlayableFromMap(source)
    }

    override fun setPlayerState(state: String?) {
        this.currentState = state
    }

    private fun hideSystemUi() {
        playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        view.layout(l, t, r, b)

        playerView.layout(l, t, r, b)
        for (i in 0..playerView.childCount) {
            playerView.getChildAt(i)?.layout(l, t, r, b)
        }
        playerView?.layout(l, t, r, b)
        hideSystemUi()
    }

    fun onKeyChanged(event: ReadableMap?) {
        var moveFocus = !playerView.isControllerVisible

        playerView.showController()
        if (moveFocus) {
            playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                exo_play_pause.requestFocus()
            }
        }
        val scanCode = event?.getInt("keyCode")
        when (scanCode) {
            KEYCODE_MEDIA_REWIND -> playerView.findViewById<ImageButton>(R.id.exo_ffwd).performClick()
            KEYCODE_MEDIA_FAST_FORWARD -> playerView.findViewById<ImageButton>(R.id.exo_rew).performClick()
            KEYCODE_MEDIA_PLAY_PAUSE -> playerView.findViewById<ImageButton>(R.id.exo_play_pause).performClick()
            KEYCODE_MEDIA_PREVIOUS -> playerView.findViewById<ImageButton>(R.id.exo_prev)
            KEYCODE_MEDIA_NEXT -> playerView.findViewById<ImageButton>(R.id.exo_next)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        player?.stop()
        //player?.removeListeners("Plugin")
        player?.destroy()

        timer.cancel()
        myhandler.removeCallbacks(runnable)
    }

    private fun videoIdle() {
        isBuffering(true)
        onPlaybackStalled()
        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
            exo_play_pause.requestFocus()
        }
        onPause()
    }

    private fun videoBuffering() {
        isBuffering(true)
    }

    private fun videoReady() {
        onReadyForDisplay()
        isBuffering(false)
        onPlay()
    }

    private fun videoFinished() {
        isBuffering(true)
        player?.seekTo(0)
        player?.pause()
        playerView.showController()
        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
            exo_play_pause.requestFocus()
        }
        onEnd()
    }

    fun backButtonPressed() {
        val resultMap = WritableNativeMap()
        DataUtils.pushToReactMap(resultMap, "keyCode", KEYCODE_BACK)
        DataUtils.pushToReactMap(resultMap, "code", "")
        val reactContext = context as ReactContext
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit("onTvKeyDown", resultMap)
    }

    fun restartTimer() {
        myhandler.removeCallbacks(runnable)
        hideController()
    }

    fun showController() {
        playerView.player_controller.alpha = 1f
    }


    fun hideController() {
        myhandler.postDelayed(runnable, delayInMs.toLong())
    }

     override fun onApplicationPause() {
         player?.onApplicationPaused()
     }

     override fun onApplicationResume() {
         player?.onApplicationResumed()
     }

     override fun onDestroy() {
         //player?.removeListeners("Plugin")
         player?.destroy()
     }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.action == ACTION_UP && event.keyCode == KEYCODE_BACK) {
            this.view.back_button.performClick()
            return true
        }

        if (OSUtil.isTv()) {
            setTvControlLogic()
        } else {
            setMobileLogic()
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     *  Media session to handle remote events
     */
    private fun initializeMediaSession(view: View?) {

        val TAG = "MediaSession"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val stateBuilder = PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY or
                                    PlaybackStateCompat.ACTION_PAUSE or
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                    PlaybackStateCompat.ACTION_FAST_FORWARD or
                                    PlaybackStateCompat.ACTION_REWIND or
                                    PlaybackStateCompat.ACTION_SEEK_TO
                    )

            mediaSession = MediaSessionCompat(view!!.context, TAG).apply {
                setFlags(
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or
                                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                )

                setMediaButtonReceiver(null)

                setPlaybackState(stateBuilder.build())

                setCallback(PlayerSessionCallback(
                        player!!,
                        playerView
                ) { hideController() })

                isActive = true

            }
        }
    }

    private fun isBuffering(boolean: Boolean) {
        when (boolean) {
            true -> {
                onBuffer()
                exo_player_progress_bar.visibility = View.VISIBLE
            }
            else -> {
                exo_player_progress_bar.visibility = View.GONE
            }
        }
    }

    private fun setMobileLogic(): Boolean {
//        if (!playerView.isControllerVisible) {
//            playerView.hideController()
//        } else {
//            playerView.showController()
//        }
        return true
    }


    private fun setTvControlLogic(): Boolean {
        if (playerView.player_controller.alpha == 1f && isDelayRunning) {
            restartTimer()
        } else if (playerView.player_controller.alpha == 0f) {
            this.showController()
        } else {
            hideController()
        }
        return true
    }

    private fun setPlayerViewPlayFastRewind() {
//        if (!OSUtil.isTv()) {
//            video_view.controllerShowTimeoutMs = 5000
//            hideOnMobile()
//        }
    }

    private fun hideOnMobile() {
        back_button.visibility = View.INVISIBLE
        back_button.visibility = View.GONE
    }

    private fun getPlayableFromMap(playableMap: ReadableMap?): Playable? {
        var playable: Playable? = null // default channel
        val type = if (playableMap != null) playableMap.getString("type") else ""
        if ("channel".equals(type, ignoreCase = true)) {
            val channelId = playableMap!!.getInt("id").toString()
            playable = AppData.getAPAccount().getChannel(channelId)
        } else if ("vod".equals(type, ignoreCase = true)) {
            val vodDictionary = playableMap!!.getString("object")
            playable = Gson().fromJson(vodDictionary, APVodItem::class.java)
        } else if ("atom_entry".equals(type, ignoreCase = true)) {
            val atomEntryJson = playableMap!!.getString("model")
            val atomEntry = SerializationUtils.fromJson(atomEntryJson, APAtomEntry::class.java)
            playable = APAtomEntryPlayable(atomEntry)
        } else if ("url".equals(type, ignoreCase = true)) {
            try {
                val `object` = JSONObject(playableMap!!.getString("object"))
                playable = APURLPlayable()
                playable.setContentVideoUrl(`object`.getString("stream_url"))
                playable.setPlayableId(`object`.getString("id"))
                playable.isLive = `object`.getBoolean("isLive")
                playable.isFree = `object`.getBoolean("isFree")
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        if (playable != null) {
            Log.d("playing", playable.toString())
        }
        return playable
    }


    //----------------------------------------- Examples -----------------------------------------//
//    @ReactProp(name = PROP_RESIZE_MODE)
//    public void setResizeMode(final ReactVideoView videoView, final String resizeModeOrdinalString) {
//        videoView.setResizeModeModifier(ScalableType.values()[Integer.parseInt(resizeModeOrdinalString)]);
//    }
//    @ReactProp(name = "paused", defaultBoolean = false)
//    public void setPaused(final ReactVideoView videoView, final boolean paused) {
//        videoView.setPausedModifier(paused);
//    }
//
//    @ReactProp(name = "muted", defaultBoolean = false)
//    public void setMuted(final ReactVideoView videoView, final boolean muted) {
//        videoView.setMutedModifier(muted);
//    }
//----------------------------------------- Private -----------------------------------------//
}