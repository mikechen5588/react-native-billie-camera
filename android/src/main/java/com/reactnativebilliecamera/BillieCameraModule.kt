package com.reactnativebilliecamera

import com.billiecamera.camera.CameraActivity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise

class BillieCameraModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "BillieCamera"
    }

    // Example method
    // See https://reactnative.dev/docs/native-modules-android
    @ReactMethod
    fun startCamera(type:Int, promise: Promise) {
      val activity = currentActivity ?: return
      CameraActivity.startCamera(activity, type) {
        // return value
        promise.resolve(it)
      }
    }

}
