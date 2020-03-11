package com.applicaster.quickbrickplayerplugin


import android.content.Context
import com.applicaster.quickbrickplayerplugin.quickbrickInterface.QuickBrickPlayer
import com.facebook.react.bridge.*

import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.annotations.ReactProp


class QuickBrickDefaultPlayerManager(context: ReactApplicationContext) : SimpleViewManager<QuickBrickDefaultPlayerView>() {
    override fun getName(): String {
        return REACT_CLASS
    }

    public override fun createViewInstance(context: ThemedReactContext): QuickBrickDefaultPlayerView {

        val view = QuickBrickDefaultPlayerView(context as Context?)
        val listener = object : LifecycleEventListener {
            override fun onHostResume() {
                view.player.takeIf { it != null }.apply { view.player?.playWhenReady = true }
            }

            override fun onHostPause() {
                view.player.takeIf { it != null }.apply { view.player?.playWhenReady = false }
            }

            override fun onHostDestroy() {}
        }
        context.addLifecycleEventListener(listener)
        return view
    }

    @ReactProp(name = "playableItem")
    fun setPlayableItem(view: QuickBrickDefaultPlayerView, source: ReadableMap) {
        source.let { view.setPlayableItem(it) }

    }


    @ReactProp(name = "onKeyChanged")
    fun onKeyChanged(view: QuickBrickDefaultPlayerView, event: ReadableMap?) {
        view.onKeyChanged(event)
    }


    @ReactProp(name = "playerState")
    fun setPlayerState(view: QuickBrickDefaultPlayerView, state: String?) {
        view.setPlayerState(state)
    }

    companion object {
        val REACT_CLASS = "QuickBrickDefaultPlayerView"
    }

    override fun getExportedCustomBubblingEventTypeConstants(): Map<String, Any> {
        return QuickBrickPlayer.registerCallbackProps()
    }

}
