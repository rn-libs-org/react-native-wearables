export type WearableMessage = Record<string, unknown>;
export type MessageListener = (message: WearableMessage) => void;
/**
 * Send a message to the connected watch app.
 *
 * - **iOS**: Uses `WCSession.sendMessage`. Watch must be reachable.
 * - **Android**: Uses Wear OS `MessageClient`. At least one node must be connected.
 *
 * @param message - Dictionary of key-value pairs to send.
 * @throws If the watch is not reachable or sending fails.
 */
export declare function sendMessage(message: WearableMessage): Promise<void>;
/**
 * Check if a watch is paired with this device.
 *
 * - **iOS**: Uses `WCSession.isPaired`.
 * - **Android**: Checks for connected Wear OS nodes.
 */
export declare function isPaired(): Promise<boolean>;
/**
 * Check if the paired watch is currently reachable.
 *
 * - **iOS**: Uses `WCSession.isReachable`.
 * - **Android**: Checks for connected Wear OS nodes.
 */
export declare function isReachable(): Promise<boolean>;
/**
 * Check if the companion watch app is installed on the paired watch. **(iOS only)**
 *
 * On Android, this always resolves to `false`.
 */
export declare function isWatchAppInstalled(): Promise<boolean>;
/**
 * Subscribe to messages received from the watch.
 *
 * @param listener - Callback invoked with the received message dictionary.
 * @returns A function that removes the subscription when called.
 *
 */
export declare function onMessageReceived(listener: MessageListener): () => void;
//# sourceMappingURL=index.d.ts.map