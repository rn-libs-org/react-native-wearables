import { TurboModuleRegistry, type TurboModule } from 'react-native';
import type { WearableMessage } from '.';

export interface Spec extends TurboModule {
  sendMessage(message: Object): Promise<void>;
  updateApplicationContext(context: Object): Promise<void>;
  getApplicationContext(): Promise<WearableMessage | null>;
  isPaired(): Promise<boolean>;
  isReachable(): Promise<boolean>;
  isWatchAppInstalled(): Promise<boolean>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Wearables');
