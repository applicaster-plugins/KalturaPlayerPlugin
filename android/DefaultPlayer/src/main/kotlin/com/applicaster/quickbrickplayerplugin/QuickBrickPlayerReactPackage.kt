package com.applicaster.quickbrickplayerplugin

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext

class QuickBrickPlayerReactPackage : ReactPackage {
    var repository = ViewManagerRepository()

    override fun createNativeModules(reactContext: ReactApplicationContext)= emptyList<NativeModule>()
    override fun createViewManagers(reactContext: ReactApplicationContext)= repository.getViewManagers(reactContext)

}