package com.wearables

import com.facebook.react.bridge.ReactApplicationContext

class WearablesModule(reactContext: ReactApplicationContext) :
  NativeWearablesSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeWearablesSpec.NAME
  }
}
