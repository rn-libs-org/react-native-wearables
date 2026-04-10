# react-native-wearables

A cross-platform React Native library for communicating with smartwatches — **Apple Watch** (WatchConnectivity) and **Wear OS** (Wearable MessageClient).

[![MIT License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![React Native](https://img.shields.io/badge/React%20Native-0.76+-green.svg)](https://reactnative.dev)
[![New Architecture](https://img.shields.io/badge/New%20Architecture-supported-brightgreen.svg)](#)

## Features

| Feature               | iOS (Apple Watch)             | Android (Wear OS)                       |
| --------------------- | ----------------------------- | --------------------------------------- |
| `sendMessage`         | WCSession                     | MessageClient (nearby nodes)            |
| `isPaired`            | WCSession.isPaired            | NodeClient.connectedNodes               |
| `isReachable`         | WCSession.isReachable         | Node.isNearby filter                    |
| `isWatchAppInstalled` | WCSession.isWatchAppInstalled | Always `false`                          |
| `onMessageReceived`   | WCSessionDelegate             | MessageClient.OnMessageReceivedListener |

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
  - [Check Watch Status](#check-watch-status)
  - [Send Messages](#send-messages)
  - [Receive Messages](#receive-messages)
- [API Reference](#api-reference)
- [Watch-Side Code](#watch-side-code)
  - [Wear OS (Jetpack Compose)](#wear-os-jetpack-compose)
  - [Apple Watch (SwiftUI)](#apple-watch-swiftui)
- [Watch Development Guidelines](#watch-development-guidelines)
  - [Wear OS Setup](#wear-os-setup)
  - [Apple Watch Setup](#apple-watch-setup)
- [Example App](#example-app)
- [Contributing](#contributing)
- [License](#license)

## Installation

```sh
npm install react-native-wearables
# or
yarn add react-native-wearables
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
} from 'react-native-wearables';

async function checkWatch() {
  const paired = await isPaired();
  const reachable = await isReachable();
  const installed = await isWatchAppInstalled(); // iOS only

  console.log({ paired, reachable, installed });
}
```

### Send Messages

```tsx
import { sendMessage } from 'react-native-wearables';

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
import { onMessageReceived } from 'react-native-wearables';

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

- **iOS** — Uses `WCSession.sendMessage`. The watch must be reachable.
- **Android** — Uses `MessageClient.sendMessage` to the first nearby Bluetooth-connected node.

Throws if the watch is not reachable or no nearby nodes are found.

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

### Wear OS (Jetpack Compose)

Below is a complete Wear OS activity that can **send messages to** and **receive messages from** your React Native app.

> **Important:** The Wear OS app and your React Native Android app must share the **same `applicationId`** (package name) and be **signed with the same key**.

```kotlin
package com.yourapp.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.*
import org.json.JSONObject

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    companion object {
        private const val TAG = "WearApp"
        // Must match the path used in react-native-wearables
        private const val MESSAGE_PATH = "/wearables_message"
    }

    private var lastMessage by mutableStateOf("No messages yet")
    private var connectedNode: Node? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the message listener
        Wearable.getMessageClient(this).addListener(this)

        // Find the connected phone node
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            connectedNode = nodes.firstOrNull { it.isNearby }
            Log.d(TAG, "Connected node: ${connectedNode?.displayName}")
        }

        setContent {
            WearApp(
                lastMessage = lastMessage,
                onSendMessage = { sendMessageToPhone() }
            )
        }
    }

    override fun onDestroy() {
        Wearable.getMessageClient(this).removeListener(this)
        super.onDestroy()
    }

    /**
     * Receives messages from the React Native mobile app.
     */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == MESSAGE_PATH) {
            val json = String(messageEvent.data, Charsets.UTF_8)
            Log.d(TAG, "Received: $json")
            lastMessage = json
        }
    }

    /**
     * Sends a JSON message to the mobile app via the data bytes.
     * The mobile app's `onMessageReceived` listener will pick this up.
     */
    private fun sendMessageToPhone() {
        val node = connectedNode
        if (node == null) {
            Log.w(TAG, "No connected phone node found")
            lastMessage = "Error: No phone connected"
            return
        }

        val payload = JSONObject().apply {
            put("action", "watchUpdate")
            put("heartRate", 72)
            put("timestamp", System.currentTimeMillis())
        }

        Wearable.getMessageClient(this)
            .sendMessage(node.id, MESSAGE_PATH, payload.toString().toByteArray(Charsets.UTF_8))
            .addOnSuccessListener {
                Log.d(TAG, "Message sent to phone")
                lastMessage = "Sent: ${payload.toString()}"
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Send failed", e)
                lastMessage = "Send failed: ${e.message}"
            }
    }
}

@Composable
fun WearApp(lastMessage: String, onSendMessage: () -> Unit) {
    MaterialTheme {
        Scaffold(
            timeText = { TimeText() }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Wearables Demo", style = MaterialTheme.typography.title3)
                Spacer(modifier = Modifier.height(8.dp))
                Text(lastMessage, style = MaterialTheme.typography.body2)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onSendMessage) {
                    Text("Send to Phone")
                }
            }
        }
    }
}
```

**Wear OS `build.gradle.kts` dependencies:**

```kotlin
dependencies {
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    implementation("androidx.wear.compose:compose-material:1.4.1")
    implementation("androidx.wear.compose:compose-foundation:1.4.1")
    implementation("androidx.activity:activity-compose:1.10.1")
}
```

### Apple Watch (SwiftUI)

Below is a WatchKit extension that communicates with your React Native iOS app.

```swift
import SwiftUI
import WatchConnectivity

class PhoneConnector: NSObject, ObservableObject, WCSessionDelegate {
    @Published var lastMessage: String = "No messages yet"

    override init() {
        super.init()
        if WCSession.isSupported() {
            let session = WCSession.default
            session.delegate = self
            session.activate()
        }
    }

    func sendMessageToPhone() {
        guard WCSession.default.isReachable else {
            lastMessage = "Phone not reachable"
            return
        }

        let payload: [String: Any] = [
            "action": "watchUpdate",
            "heartRate": 72,
            "timestamp": Date().timeIntervalSince1970
        ]

        WCSession.default.sendMessage(payload, replyHandler: { _ in
            DispatchQueue.main.async {
                self.lastMessage = "Sent: \(payload)"
            }
        }, errorHandler: { error in
            DispatchQueue.main.async {
                self.lastMessage = "Error: \(error.localizedDescription)"
            }
        })
    }

    // MARK: - WCSessionDelegate

    func session(_ session: WCSession,
                 activationDidCompleteWith activationState: WCSessionActivationState,
                 error: Error?) {
        // Session activated
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        DispatchQueue.main.async {
            self.lastMessage = "\(message)"
        }
    }

    func session(_ session: WCSession,
                 didReceiveMessage message: [String: Any],
                 replyHandler: @escaping ([String: Any]) -> Void) {
        DispatchQueue.main.async {
            self.lastMessage = "\(message)"
        }
        replyHandler(["status": "received"])
    }
}

struct ContentView: View {
    @StateObject private var connector = PhoneConnector()

    var body: some View {
        VStack(spacing: 12) {
            Text("Wearables Demo")
                .font(.headline)
            Text(connector.lastMessage)
                .font(.caption)
                .multilineTextAlignment(.center)
            Button("Send to Phone") {
                connector.sendMessageToPhone()
            }
        }
        .padding()
    }
}
```

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
