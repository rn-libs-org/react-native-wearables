# Wearables Example App

A demo React Native app that exercises every feature of `@rn-libs/react-native-wearables` — checking watch status, sending messages, and receiving messages from a paired watch.

## Prerequisites

| Tool             | Version                                    |
| ---------------- | ------------------------------------------ |
| Node.js          | See root `.nvmrc`                          |
| Yarn             | v1 classic                                 |
| React Native CLI | Included via `@react-native-community/cli` |
| Xcode            | 15+ (for iOS)                              |
| Android Studio   | Latest stable (for Android)                |
| CocoaPods        | `gem install cocoapods`                    |
| Ruby Bundler     | `gem install bundler` (for iOS)            |

## Setup

From the **repository root**:

```sh
yarn          # install all workspace dependencies
```

## Running on Android

```sh
# From the repo root
yarn example android
```

This starts Metro and builds/installs the app on the connected Android device or emulator.

### Wear OS Testing

To test watch communication on Android:

1. Create a **Wear OS emulator** in Android Studio (use a Google Play system image).
2. Create an **Android phone emulator** (also Google Play image).
3. Pair them following the [official pairing guide](https://developer.android.com/training/wearables/get-started/connect-phone).
4. Build and install a Wear OS companion app with the **same `applicationId`** and **signing key** as this example app. See the [Watch-Side Code](../README.md#wear-os-jetpack-compose) section in the main README for the full source.

## Running on iOS

Install CocoaPods dependencies first:

```sh
# From the repo root (one-time Ruby setup)
cd example
bundle install
bundle exec pod install --project-directory=ios
cd ..
```

Then run:

```sh
yarn example ios
```

### Apple Watch Testing

1. In Xcode, add a **watchOS App** target to the `WearablesExample.xcworkspace`.
2. The Watch app bundle ID must be `<your-ios-bundle-id>.watchkitapp`.
3. In the Simulator menu, pair the iPhone simulator with a Watch simulator (Device → Pair with Watch).
4. See the [Watch-Side Code](../README.md#apple-watch-swiftui) section in the main README for the SwiftUI source.

## Running on Web

```sh
yarn example web
```

> Note: Watch communication APIs are not available on Web — the app will display UI but native calls will not function.

## What the App Does

| Feature                   | Description                                                                                    |
| ------------------------- | ---------------------------------------------------------------------------------------------- |
| **Check Watch Status**    | Calls `isPaired()`, `isReachable()`, `isWatchAppInstalled()`, and `updateApplicationContext()` |
| **Send Message to Watch** | Sends a JSON payload via `sendMessage()`                                                       |
| **Receive Messages**      | Listens with `onMessageReceived()` and displays the last received message                      |

## Project Structure

```
example/
├── src/
│   └── App.tsx           # Main demo screen
├── index.js              # Entry point
├── android/              # Android project
├── ios/                  # iOS project + Pods
├── package.json
└── metro.config.js
```

## Troubleshooting

- **Android build fails:** Run `cd example/android && ./gradlew clean` then try again.
- **iOS pod errors:** Run `cd example/ios && pod install --repo-update`.
- **"Wearable API not available":** Ensure the Android emulator has Google Play Services and the Wear OS companion app is installed.
- **Messages not received:** Verify both apps share the same `applicationId`/bundle ID and signing key.
