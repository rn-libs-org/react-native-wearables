package com.wearables

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

class WearablesModule(reactContext: ReactApplicationContext) :
  NativeWearablesSpec(reactContext), MessageClient.OnMessageReceivedListener {

  private var messageClient: MessageClient? = null
  private var nodeClient: NodeClient? = null
  private var listenerCount = 0

  companion object {
    const val NAME = NativeWearablesSpec.NAME
    private const val MESSAGE_PATH = "/wearables_message"
    private const val EVENT_MESSAGE_RECEIVED = "onMessageReceived"
  }

  override fun initialize() {
    super.initialize()
    try {
      messageClient = Wearable.getMessageClient(reactApplicationContext)
      nodeClient = Wearable.getNodeClient(reactApplicationContext)
      messageClient?.addListener(this)
    } catch (e: Exception) {
      // Wearable API not available (e.g., device without Google Play Services)
    }
  }

  override fun invalidate() {
    try {
      messageClient?.removeListener(this)
    } catch (e: Exception) {
      // Ignore cleanup errors
    }
    super.invalidate()
  }

  // Wearable Module Methods

  override fun sendMessage(message: ReadableMap, promise: Promise) {
    val nc = nodeClient
    val mc = messageClient

    if (nc == null || mc == null) {
      promise.reject("ERR_UNSUPPORTED", "Wearable API is not available on this device")
      return
    }

    nc.connectedNodes.addOnSuccessListener { nodes ->
      if (nodes.isEmpty()) {
        promise.reject("ERR_NO_NODES", "No connected wearable nodes found")
        return@addOnSuccessListener
      }

      val jsonObject = JSONObject(message.toHashMap())
      val data = jsonObject.toString().toByteArray(Charsets.UTF_8)

      val totalNodes = nodes.size
      var completedCount = 0
      var hasError = false

      for (node in nodes) {
        mc.sendMessage(node.id, MESSAGE_PATH, data)
          .addOnSuccessListener {
            synchronized(this) {
              completedCount++
              if (completedCount == totalNodes && !hasError) {
                promise.resolve(null)
              }
            }
          }
          .addOnFailureListener { e ->
            synchronized(this) {
              if (!hasError) {
                hasError = true
                promise.reject("ERR_SEND_FAILED", e.message, e)
              }
            }
          }
      }
    }.addOnFailureListener { e ->
      promise.reject("ERR_NODE_ERROR", e.message, e)
    }
  }

  override fun isPaired(promise: Promise) {
    val nc = nodeClient
    if (nc == null) {
      promise.resolve(false)
      return
    }

    nc.connectedNodes
      .addOnSuccessListener { nodes ->
        promise.resolve(nodes.isNotEmpty())
      }
      .addOnFailureListener {
        promise.resolve(false)
      }
  }

  override fun isReachable(promise: Promise) {
    val nc = nodeClient
    if (nc == null) {
      promise.resolve(false)
      return
    }

    nc.connectedNodes
      .addOnSuccessListener { nodes ->
        promise.resolve(nodes.isNotEmpty())
      }
      .addOnFailureListener {
        promise.resolve(false)
      }
  }

  override fun isWatchAppInstalled(promise: Promise) {
    // iOS-only feature — always resolve false on Android
    promise.resolve(false)
  }

  override fun addListener(eventName: String) {
    listenerCount++
  }

  override fun removeListeners(count: Double) {
    listenerCount = (listenerCount - count.toInt()).coerceAtLeast(0)
  }

  // MessageClient.OnMessageReceivedListener

  override fun onMessageReceived(messageEvent: MessageEvent) {
    if (listenerCount <= 0 || messageEvent.path != MESSAGE_PATH) return

    val data = String(messageEvent.data, Charsets.UTF_8)
    try {
      val jsonObject = JSONObject(data)
      val params = convertJsonToWritableMap(jsonObject)
      sendEvent(EVENT_MESSAGE_RECEIVED, params)
    } catch (e: Exception) {
      // If not valid JSON, wrap raw string
      val params = Arguments.createMap().apply {
        putString("data", data)
      }
      sendEvent(EVENT_MESSAGE_RECEIVED, params)
    }
  }

  private fun convertJsonToWritableMap(jsonObject: JSONObject): WritableMap {
    val map = Arguments.createMap()
    val iterator = jsonObject.keys()
    while (iterator.hasNext()) {
      val key = iterator.next()
      when (val value = jsonObject.get(key)) {
        is String -> map.putString(key, value)
        is Int -> map.putInt(key, value)
        is Long -> map.putDouble(key, value.toDouble())
        is Double -> map.putDouble(key, value)
        is Boolean -> map.putBoolean(key, value)
        is JSONObject -> map.putMap(key, convertJsonToWritableMap(value))
        JSONObject.NULL -> map.putNull(key)
        else -> map.putString(key, value.toString())
      }
    }
    return map
  }

  private fun sendEvent(eventName: String, params: WritableMap?) {
    reactApplicationContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }
}
