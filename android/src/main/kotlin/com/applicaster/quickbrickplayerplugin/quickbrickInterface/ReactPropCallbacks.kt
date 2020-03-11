package com.applicaster.quickbrickplayerplugin.quickbrickInterface


import com.applicaster.quickbrickplayerplugin.helper.ReactPropCallback
import com.facebook.react.common.MapBuilder

interface ReactPropCallbacks {

    val reactCallbackPropNames: Array<String>
    val reactCallbackProps: MutableMap<String, ReactPropCallback>

    fun registerCallbackProps(): MutableMap<String, Any> {
        val builder = MapBuilder.builder<String, Any>()

        reactCallbackPropNames.forEach {
            val reactPropCallback = ReactPropCallback(it)
            reactCallbackProps.put(it, reactPropCallback)

            builder.put(
                    it,
                    MapBuilder.of<String, Any>(
                            "phasedRegistrationNames",
                            MapBuilder.of<String, String>(
                                    "bubbled",
                                    it
                            )
                    )
            )
        }

        return builder.build()
    }

    fun getReactCallback(name: String): ReactPropCallback? {
        return reactCallbackProps.get(name)
    }
}