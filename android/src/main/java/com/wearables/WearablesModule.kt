package com.wearables

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
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

  private fun isWearableApiAvailable(): Boolean {
    val context = reactApplicationContext
    val nc = nodeClient ?: return false

    val gpStatus = GoogleApiAvailability.getInstance()
      .isGooglePlayServicesAvailable(context)
    if (gpStatus != ConnectionResult.SUCCESS) return false

    return try {
      Tasks.await(
        GoogleApiAvailability.getInstance().checkApiAvailability(nc)
      )
      true
    } catch (_: Exception) {
      false
    }
  }

  private fun getConnectedNodes(): List<Node>? {
    if (!isWearableApiAvailable()) return null
    val nc = nodeClient ?: return null
    return try {
      Tasks.await(nc.connectedNodes)
    } catch (_: Exception) {
      null
    }
  }

  private fun getNearbyNodes(): List<Node> {
    return getConnectedNodes()?.filter { it.isNearby } ?: emptyList()
  }

  // Wearable Module Methods
  override fun sendMessage(message: ReadableMap, promise: Promise) {
    val mc = messageClient
    if (mc == null) {
      promise.reject("ERR_UNSUPPORTED", "Wearable API is not available on this device")
      return
    }

    try {
      val nearbyNodes = getNearbyNodes()
      if (nearbyNodes.isEmpty()) {
        promise.reject("ERR_NO_NODES", "No connected wearable nodes found")
        return
      }

      val jsonObject = JSONObject(message.toHashMap())
      val data = jsonObject.toString().toByteArray(Charsets.UTF_8)

      val targetNode = nearbyNodes.first()

      Tasks.await(mc.sendMessage(targetNode.id, MESSAGE_PATH, data))
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject("ERR_SEND_FAILED", e.message, e)
    }
  }

  override fun isPaired(promise: Promise) {
    try {
      val nodes = getConnectedNodes()
      promise.resolve(nodes != null && nodes.isNotEmpty())
    } catch (e: Exception) {
      promise.resolve(false)
    }
  }

  override fun isReachable(promise: Promise) {
    try {
      val nearbyNodes = getNearbyNodes()
      promise.resolve(nearbyNodes.isNotEmpty())
    } catch (e: Exception) {
      promise.resolve(false)
    }
  }

  override fun isWatchAppInstalled(promise: Promise) {
    // iOS-only feature — always resolve false on Android
    promise.resolve(false)
  }

  override fun updateApplicationContext(context: ReadableMap, promise: Promise) {
    // iOS-only feature — always resolve null on Android
    promise.resolve(null)
  }

  override fun getApplicationContext(promise: Promise) {
    // iOS-only feature — always resolve null on Android
    promise.resolve(null)
  }

  override fun addListener(eventName: String) {
    listenerCount++
  }

  override fun removeListeners(count: Double) {
    listenerCount = (listenerCount - count.toInt()).coerceAtLeast(0)
  }

  override fun onMessageReceived(messageEvent: MessageEvent) {
    if (listenerCount <= 0) return

    if (messageEvent.path == MESSAGE_PATH) {
      val data = String(messageEvent.data, Charsets.UTF_8)
      try {
        val jsonObject = JSONObject(data)
        val params = convertJsonToWritableMap(jsonObject)
        sendEvent(EVENT_MESSAGE_RECEIVED, params)
      } catch (e: Exception) {
        val params = Arguments.createMap().apply {
          putString("data", data)
        }
        sendEvent(EVENT_MESSAGE_RECEIVED, params)
      }
    } else {
      try {
        val jsonObject = JSONObject(messageEvent.path)
        val params = convertJsonToWritableMap(jsonObject)
        sendEvent(EVENT_MESSAGE_RECEIVED, params)
      } catch (e: Exception) {
        val params = Arguments.createMap().apply {
          putString("path", messageEvent.path)
          if (messageEvent.data != null && messageEvent.data.isNotEmpty()) {
            putString("data", String(messageEvent.data, Charsets.UTF_8))
          }
        }
        sendEvent(EVENT_MESSAGE_RECEIVED, params)
      }
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
