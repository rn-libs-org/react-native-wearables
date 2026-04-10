import { type TurboModule } from 'react-native';
export interface Spec extends TurboModule {
    sendMessage(message: Object): Promise<void>;
    isPaired(): Promise<boolean>;
    isReachable(): Promise<boolean>;
    isWatchAppInstalled(): Promise<boolean>;
    addListener(eventName: string): void;
    removeListeners(count: number): void;
}
declare const _default: Spec;
export default _default;
//# sourceMappingURL=NativeWearables.d.ts.map