"use strict";

import { NativeEventEmitter } from 'react-native';
import NativeWearables from "./NativeWearables.js";
const WearablesEmitter = new NativeEventEmitter(NativeWearables);
/**
 * Send a message to the connected watch app.
 *
 * - **iOS**: Uses `WCSession.sendMessage`. Watch must be reachable.
 * - **Android**: Uses Wear OS `MessageClient`. At least one node must be connected.
 *
 * @param message - Dictionary of key-value pairs to send.
 * @throws If the watch is not reachable or sending fails.
 */
export function sendMessage(message) {
  return NativeWearables.sendMessage(message);
}

/**
 * Check if a watch is paired with this device.
 *
 * - **iOS**: Uses `WCSession.isPaired`.
 * - **Android**: Checks for connected Wear OS nodes.
 */
export function isPaired() {
  return NativeWearables.isPaired();
}

/**
 * Check if the paired watch is currently reachable.
 *
 * - **iOS**: Uses `WCSession.isReachable`.
 * - **Android**: Checks for connected Wear OS nodes.
 */
export function isReachable() {
  return NativeWearables.isReachable();
}

/**
 * Check if the companion watch app is installed on the paired watch. **(iOS only)**
 *
 * On Android, this always resolves to `false`.
 */
export function isWatchAppInstalled() {
  return NativeWearables.isWatchAppInstalled();
}

/**
 * Subscribe to messages received from the watch.
 *
 * @param listener - Callback invoked with the received message dictionary.
 * @returns A function that removes the subscription when called.
 *
 */
export function onMessageReceived(listener) {
  const subscription = WearablesEmitter.addListener('onMessageReceived', listener);
  return () => subscription.remove();
}
//# sourceMappingURL=index.js.map