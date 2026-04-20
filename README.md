# @rn-libs/react-native-wearables

A cross-platform React Native library for communicating with smartwatches — **Apple Watch** (WatchConnectivity) and **Wear OS** (Wearable MessageClient).

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![React Native](https://img.shields.io/badge/React%20Native-0.76+-green.svg)](https://reactnative.dev)
[![New Architecture](https://img.shields.io/badge/New%20Architecture-supported-brightgreen.svg)](#)

## Features

| Feature               | iOS (Apple Watch)                       | Android (Wear OS)                       |
| --------------------- | --------------------------------------- | --------------------------------------- |
| `sendMessage`         | WCSession + applicationContext fallback | MessageClient (nearby nodes)            |
| `isPaired`            | WCSession.isPaired                      | NodeClient.connectedNodes               |
| `isReachable`         | WCSession.isReachable                   | Node.isNearby filter                    |
| `isWatchAppInstalled` | WCSession.isWatchAppInstalled           | Always `false`                          |
| `onMessageReceived`   | WCSessionDelegate                       | MessageClient.OnMessageReceivedListener |

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
  - [Check Watch Status](#check-watch-status)
  - [Send Messages](#send-messages)
  - [Receive Messages](#receive-messages)
- [API Reference](#api-reference)
- [Watch-Side Code](#watch-side-code)
  - [Wear OS (Kotlin)](#wear-os-kotlin)
  - [Apple Watch (Swift)](#apple-watch-swift)
- [Watch Development Guidelines](#watch-development-guidelines)
  - [Wear OS Setup](#wear-os-setup)
  - [Apple Watch Setup](#apple-watch-setup)
- [Example App](#example-app)
- [Contributing](#contributing)
- [License](#license)

## Installation

```sh
npm install @rn-libs/react-native-wearables
# or
yarn add @rn-libs/react-native-wearables
```

### iOS

No additional steps needed — the podspec automatically links the `WatchConnectivity` framework.

```sh
cd ios && pod install
```

### Android

Add the following permissions to your app's `android/app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

    <application ...>
      <!-- your activities -->
    </application>
</manifest>
```

## Usage

### Check Watch Status

```tsx
import {
  isPaired,
  isReachable,
  isWatchAppInstalled,
} from '@rn-libs/react-native-wearables';

async function checkWatch() {
  const paired = await isPaired();
  const reachable = await isReachable();
  const installed = await isWatchAppInstalled(); // iOS only

  console.log({ paired, reachable, installed });
}
```

### Send Messages

```tsx
import { sendMessage } from '@rn-libs/react-native-wearables';

async function sendToWatch() {
  try {
    await sendMessage({
      action: 'hello',
      timestamp: Date.now(),
      greeting: 'Hello from React Native!',
    });
    console.log('Message sent successfully');
  } catch (error) {
    console.error('Send failed:', error.message);
  }
}
```

### Receive Messages

```tsx
import { useEffect } from 'react';
import { onMessageReceived } from '@rn-libs/react-native-wearables';

function App() {
  useEffect(() => {
    const unsubscribe = onMessageReceived((message) => {
      console.log('Received from watch:', message);
    });

    return () => unsubscribe();
  }, []);
}
```

## API Reference

### `sendMessage(message: Record<string, unknown>): Promise<void>`

Send a key-value message to the connected watch.

- **iOS** — Uses `WCSession.sendMessage` when the watch is reachable, and automatically falls back to `updateApplicationContext` when the watch is unreachable or the send fails. Throws if the watch is not paired or the watch app is not installed.
- **Android** — Uses `MessageClient.sendMessage` to the first nearby Bluetooth-connected node. Throws if no nearby nodes are found.

### `isPaired(): Promise<boolean>`

Check whether a watch is paired with the device.

- **iOS** — Returns `WCSession.isPaired`.
- **Android** — Verifies the Wearable API is available (Google Play Services + Wear companion app), then checks `NodeClient.getConnectedNodes()`.

### `isReachable(): Promise<boolean>`

Check whether the paired watch is currently reachable over Bluetooth.

- **iOS** — Returns `WCSession.isReachable`.
- **Android** — Filters connected nodes by `Node.isNearby` to ensure actual Bluetooth proximity.

### `isWatchAppInstalled(): Promise<boolean>`

Check whether the companion watch app is installed. **iOS only** — always returns `false` on Android.

### `onMessageReceived(listener: (message: Record<string, unknown>) => void): () => void`

Subscribe to incoming messages from the watch. Returns an unsubscribe function.

## Watch-Side Code

### Wear OS (Kotlin)

Below is the minimal code to **send messages to** and **receive messages from** your React Native app on a Wear OS watch.

> **Important:** The Wear OS app and your React Native Android app must share the **same `applicationId`** (package name) and be **signed with the same key**.

#### Send a message to the phone

```kotlin
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

// The path must match the one used in @rn-libs/react-native-wearables
private const val MESSAGE_PATH = "/wearables_message"

fun sendMessageToPhone(context: Context) {
    Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
        val node = nodes.firstOrNull { it.isNearby } ?: return@addOnSuccessListener

        val payload = JSONObject().apply {
            put("action", "watchUpdate")
            put("heartRate", 72)
            put("timestamp", System.currentTimeMillis())
        }
        val data = payload.toString().toByteArray(Charsets.UTF_8)

        Wearable.getMessageClient(context)
            .sendMessage(node.id, MESSAGE_PATH, data)
            .addOnSuccessListener { Log.d("Wear", "Sent") }
            .addOnFailureListener { e -> Log.e("Wear", "Failed", e) }
    }
}
```

#### Receive messages from the phone

Implement `MessageClient.OnMessageReceivedListener` and register it:

```kotlin
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Wearable.getMessageClient(this).addListener(this)
    }

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(this)
        super.onDestroy()
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/wearables_message") {
            val json = String(messageEvent.data, Charsets.UTF_8)
            Log.d("Wear", "Received from phone: $json")
        }
    }
}
```

#### Required Wear OS dependency

Add this to your Wear OS app's `build.gradle`:

```groovy
dependencies {
    implementation "com.google.android.gms:play-services-wearable:18.2.0"
}
```

### Apple Watch (Swift)

Below is the minimal code to **send messages to** and **receive messages from** your React Native iOS app on an Apple Watch.

#### Send a message to the phone

```swift
import WatchConnectivity

func sendMessageToPhone() {
    guard WCSession.default.isReachable else {
        print("Phone not reachable")
        return
    }

    let payload: [String: Any] = [
        "action": "watchUpdate",
        "heartRate": 72,
        "timestamp": Date().timeIntervalSince1970
    ]

    WCSession.default.sendMessage(payload, replyHandler: { reply in
        print("Sent, reply: \(reply)")
    }, errorHandler: { error in
        print("Send failed: \(error.localizedDescription)")
    })
}
```

#### Receive messages from the phone

Implement `WCSessionDelegate` and activate the session:

```swift
import WatchConnectivity

class SessionManager: NSObject, WCSessionDelegate {

    static let shared = SessionManager()

    func activate() {
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }

    func session(_ session: WCSession,
                 activationDidCompleteWith activationState: WCSessionActivationState,
                 error: Error?) {}

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        print("Received from phone: \(message)")
    }

    func session(_ session: WCSession,
                 didReceiveMessage message: [String: Any],
                 replyHandler: @escaping ([String: Any]) -> Void) {
        print("Received from phone: \(message)")
        replyHandler(["status": "received"])
    }
}
```

Call `SessionManager.shared.activate()` early in your watch app's lifecycle (e.g. in `@main` App `init`).

## Watch Development Guidelines

### Wear OS Setup

1. **Matching package name:** The Wear OS app must use the same `applicationId` as your React Native Android app (found in `android/app/build.gradle`).

2. **Matching signing key:** Both apps must be signed with the same keystore. For debug builds, copy the `debug.keystore` from your React Native project into the Wear OS project and configure `build.gradle.kts`:

   ```kotlin
   android {
       signingConfigs {
           getByName("debug") {
               storeFile = file("debug.keystore")
               storePassword = "android"
               keyAlias = "androiddebugkey"
               keyPassword = "android"
           }
       }
   }
   ```

3. **Pairing emulators:** Follow the [official instructions](https://developer.android.com/training/wearables/get-started/connect-phone) to pair a Wear OS emulator with an Android phone emulator. Both must use the **Google Play** system images.

4. **Required permissions:** The phone app needs Bluetooth permissions (`BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`) — these should already be in your manifest from the [installation step](#android).

### Apple Watch Setup

1. **Add a WatchKit target:** In Xcode, go to File → New → Target → watchOS → App. Make sure the Watch app is embedded in your main iOS app.

2. **Enable WatchConnectivity:** No separate capability is needed — the library links the `WatchConnectivity` framework via the podspec.

3. **Bundle identifier:** The Watch app bundle ID must be `<your-ios-bundle-id>.watchkitapp` (Xcode sets this automatically when you create the Watch target).

4. **Simulator pairing:** In the Simulator menu, use Device → Pair with Watch to link an iPhone simulator with a Watch simulator.

## Example App

See the [example/](example/) directory for a full working demo. Check its [README](example/README.md) for instructions on running it.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for development workflow, commit conventions, and how to send a pull request.

Please follow the [Code of Conduct](CODE_OF_CONDUCT.md) in all interactions.

## License

MIT
