package com.applicaster.quickbrickplayerplugin

import android.content.Context
import android.graphics.Color
import android.net.Uri
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
import com.applicaster.plugin_manager.dependencyplugin.playerplugin.PlayerSenderPlugin
import com.applicaster.quickbrickplayerplugin.helper.PlayerSessionCallback
import com.applicaster.quickbrickplayerplugin.helper.ZappFonts
import com.applicaster.quickbrickplayerplugin.helper.setFocusableBg
import com.applicaster.quickbrickplayerplugin.helper.setFocusableIcon
import com.applicaster.quickbrickplayerplugin.quickbrickInterface.QuickBrickPlayer
import com.applicaster.reactnative.utils.DataUtils
import com.applicaster.session.SessionStorage
import com.applicaster.util.OSUtil
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import kotlinx.android.synthetic.main.player_control_view.view.*
import kotlinx.android.synthetic.main.video_view.view.*
import java.util.*


class QuickBrickDefaultPlayerView(context: Context?) :
    FrameLayout(context),
    Player.EventListener,
    QuickBrickPlayer,
    Callback,
    PlayerSenderPlugin {

    var videoType: String? = null
    var videoSrc: String? = null // "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8"//http://playertest.longtailvideo.com/adaptive/bbbfull/bbbfull.m3u8"
    var player: SimpleExoPlayer? = null
    var playerView: PlayerView
    var view: View
    val timer = Timer()
    var currentState: String? = null
    var trackSelector = DefaultTrackSelector()
    var drmExtension: HashMap<String, Any>? = null
    var licenseAcquisitionUrl: String? = null
    private val URL_KEY_NAME: String = "ksm_server_url"
    private val WIDEVINE_PLAYER: String = "Widevine"
    private val PLAYER_READY: String = "playReady"
    private val keyTitle = "title"
    private val keySummary = "summary"
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

        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.setOnClickListener { player?.playWhenReady = !player?.playWhenReady!! }
        playerView.findViewById<ImageButton>(R.id.back_button)?.setOnClickListener { backButtonPressed() }

        playerView.findViewById<ImageButton>(R.id.subtitles_button)
            ?.setOnClickListener { keyboard ->
                subtitles_off = !subtitles_off
                trackSelector.parameters = DefaultTrackSelector.ParametersBuilder()
                    .setRendererDisabled(C.TRACK_TYPE_VIDEO, subtitles_off)
                    .clearSelectionOverrides()
                    .build()
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
        player?.playWhenReady = false
        player?.seekTo(time)
        player?.playWhenReady = true
    }

    private fun setDefaultFont(playerItem: TextView, montserratBold: Int, float: Float) {
        playerItem.typeface = ResourcesCompat.getFont(view.context, montserratBold)
        playerItem.setTextColor(Color.parseColor(("#EFEFEF")))
        playerItem.setTextSize(TypedValue.COMPLEX_UNIT_SP, float)

    }

    override fun getPlayerView(): View {
        return this
    }

    private fun initializePlayer() {
        // Create player instance
        player = SimpleExoPlayer.Builder(view.context).build()
        // default subtitles on
        trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(view.context)
            .setRendererDisabled(C.TRACK_TYPE_VIDEO, subtitles_off)
            .clearSelectionOverrides()
            .build()
        player?.addListener(this)
        playerView.player = player
        player?.playWhenReady = true

        //controller?.setPlayer(player)
    }

    fun updateState(state: String?) {
        state?.let { playerState ->
            when (playerState) {
                "PAUSE" -> {
                    player?.playWhenReady = false
                    player?.playbackState
                    return
                }
                "START" -> {
                    player?.playWhenReady = false
                    player?.playWhenReady = true
                    player?.playbackState
                    return
                }
                "STOP" -> {
                    player?.seekTo(0)
                    player?.playWhenReady = false
                    player?.playbackState
                    return
                }
                "FF" -> {
                    player?.playWhenReady = true
                    var seek = (player?.currentPosition ?: 0) + 1000
                    if (seek > player?.duration ?: 0)
                        seek = player?.duration ?: 0
                    player?.seekTo(seek)
                    playerView.invalidate()
                    player?.playWhenReady = false
                    player?.playbackState

                    return
                }
                "RW" -> {
                    player?.playWhenReady = true
                    var seek = (player?.currentPosition ?: 1000) - 1000
                    if (seek < 0)
                        seek = 0
                    player?.seekTo(seek)
                    player?.playWhenReady = false
                    player?.playbackState
                    return
                }
                else -> return
            }
        }
    }

    override fun setPlayerState(state: String?) {
        this.currentState = state
    }

    fun prepare(videoSrc: String?) {

        player?.let { this.playerObject = it }

        this.pluginPlayerContainer = video_view

        this.senderView = view

        this.senderAdsUrl = ""

        val uri = Uri.parse(videoSrc!!)//("http://www.html5videoplayer.net/videos/toystory.mp4")

        val mediaSource = buildMediaSource(uri)

        senderMediaSource = mediaSource

        player?.prepare(mediaSource, true, false)
        onLoadStart()

        hideSystemUi()
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        return when (videoType) {
            "video/hls" -> HlsMediaSource.Factory(DefaultHttpDataSourceFactory(OSUtil.getApplicationName())).createMediaSource(uri)
            "video/dash" -> DashMediaSource.Factory(DefaultHttpDataSourceFactory((OSUtil.getApplicationName()))).createMediaSource(uri)
            else -> ProgressiveMediaSource.Factory(DefaultHttpDataSourceFactory(OSUtil.getApplicationName())).createMediaSource(uri)
        }
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
        playerView.videoSurfaceView?.layout(l, t, r, b)
        hideSystemUi()
    }

    override fun setPlayableItem(source: ReadableMap) {
        source.getMap("content")?.getString("src")?.let {
            videoSrc = it

            videoType = if (it.contains(".m3u8", true)) {
                "video/hls"
            } else if (it.contains(".mpd", true)) {
                "video/dash"
            } else {
                "video"
            }
        }

        setVideoTitleSummary(source.toHashMap())

        if (hasDrm(source)) {
            createDrm(source)
        } else {
            prepare(this.videoSrc)
        }
        hideSystemUi()
    }

    /**
     *  Method get value from hashMap
     *  Check if value for null
     *  Set value for Title and Summary
     */
    private fun setVideoTitleSummary(toHashMap: HashMap<String, Any>) {
        val title: String? = toHashMap[keyTitle].toString()
        val summary: String? = toHashMap[keySummary].toString()

        player_title.text = title ?: ""
        player_description.text = summary ?: ""

    }

    private fun hasDrm(source: ReadableMap): Boolean {
        val isDRM = source.takeIf { it.hasKey("extensions") }
            ?.getMap("extensions")
        if (isDRM != null) {
            return isDRM.hasKey("drm")
        }
        return false
    }

    private fun createDrm(source: ReadableMap) {
        source.takeIf { it.hasKey("extensions") }
            ?.getMap("extensions")
            ?.takeIf { it.hasKey("drm") }
            ?.getMap("drm")
            ?.toHashMap()?.let { hashMap ->

                if (hashMap.containsKey("Widevine")) {
                    initializeDrmPlayer(C.WIDEVINE_UUID)
                } else if (hashMap.containsKey("playReady")) {
                    initializeDrmPlayer(C.PLAYREADY_UUID)
                }
            }
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
        player?.release()
        timer.cancel()
        myhandler.removeCallbacks(runnable)
    }

    private fun initializeDrmPlayer(playerUuid: UUID) {
        player = newSimpleInstance(setupSessionManager(playerUuid))
        prepare(this.licenseAcquisitionUrl)
        player?.addListener(this)
        player?.playWhenReady = true
    }

    private fun newSimpleInstance(drmSessionManager: DefaultDrmSessionManager<FrameworkMediaCrypto>?): SimpleExoPlayer? {
        drmSessionManager?.let {
            return ExoPlayerFactory.newSimpleInstance(context,
                DefaultRenderersFactory(context),
                trackSelector,
                it
            )
        }
        return ExoPlayerFactory.newSimpleInstance(context,
            DefaultRenderersFactory(context),
            trackSelector
        )
    }

    private fun setupSessionManager(uuid: UUID): DefaultDrmSessionManager<FrameworkMediaCrypto>? {
        val playerKey = if (uuid == C.WIDEVINE_UUID) WIDEVINE_PLAYER else PLAYER_READY
        val videoObject = drmExtension?.getValue(playerKey) as HashMap<String, Any?>?

        videoObject?.getValue(URL_KEY_NAME)?.toString()?.let {
            this.licenseAcquisitionUrl = it
            val drmCallback = HttpMediaDrmCallback(this.licenseAcquisitionUrl!!, DefaultHttpDataSourceFactory(OSUtil.getApplicationName()))
            return DefaultDrmSessionManager(uuid, FrameworkMediaDrm.newInstance(uuid),
                drmCallback, null, false)
        }
        return null
    }


    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d(this.javaClass.simpleName, "playbackState: " + playbackState + " playWhenReady " + playWhenReady)

        if (playWhenReady) {
            playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                exo_play_pause.setFocusableIcon(context!!, R.drawable.pause, R.color.focused_icon_color, R.color.unfocused_icon_color)
            }
            onPlaybackRateChange(player?.currentPosition!!)
        } else {
            playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
                exo_play_pause.setFocusableIcon(context!!, R.drawable.play, R.color.focused_icon_color, R.color.unfocused_icon_color)
            }
        }
        when (playbackState) {
            Player.STATE_IDLE -> videoIdle()
            Player.STATE_BUFFERING -> videoBuffering()
            Player.STATE_READY -> videoReady()
            Player.STATE_ENDED -> videoFinished()
        }
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
        player?.playWhenReady = false
        playerView.showController()
        playerView.findViewById<ImageButton>(R.id.exo_play_pause)?.let { exo_play_pause ->
            exo_play_pause.requestFocus()
        }
        onEnd()
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {
        Log.d(this.javaClass.simpleName, "timeline: " + timeline?.periodCount)
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        Log.d(this.javaClass.simpleName, "trackGroups: " + trackGroups?.length)
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        Log.d(this.javaClass.simpleName, "isLoading: " + isLoading)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        Log.d(this.javaClass.simpleName, "repeatMode: " + repeatMode)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        Log.d(this.javaClass.simpleName, "shuffleModeEnabled: " + shuffleModeEnabled)
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.d("Error", error.toString())
        Log.d(this.javaClass.simpleName, "error: " + error?.localizedMessage)
        onError(error?.localizedMessage!!, error)
    }

    override fun onPositionDiscontinuity(reason: Int) {
        Log.d(this.javaClass.simpleName, "reason: " + reason)
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        Log.d(this.javaClass.simpleName, "playbackParameters: " + playbackParameters?.speed)
    }

    override fun onSeekProcessed() {
        Log.d(this.javaClass.simpleName, "onSeekProcessed: " + player?.contentPosition)
    }

    override fun getCurrentTime() {
//        val event = Arguments.createMap()
//        event.putInt("currentTime", player?.currentPosition?.toInt() ?: 0)
//        event.putInt("bufferedPosition", player?.bufferedPosition?.toInt() ?: 0)
//        event.putInt("contentDuration", player?.duration?.toInt() ?: 0)
//        val reactContext = context as ReactContext
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
        if (!playerView.isControllerVisible) {
            playerView.hideController()
        } else {
            playerView.showController()
        }
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
        if (!OSUtil.isTv()) {
            video_view.controllerShowTimeoutMs = 5000
            hideOnMobile()
        }
    }

    private fun hideOnMobile() {
        back_button.visibility = View.INVISIBLE
        back_button.visibility = View.GONE
    }

}
