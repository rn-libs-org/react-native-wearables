import { TurboModuleRegistry, type TurboModule } from 'react-native';

export interface Spec extends TurboModule {
  sendMessage(message: Object): Promise<void>;
  updateApplicationContext(context: Object): Promise<void>;
  isPaired(): Promise<boolean>;
  isReachable(): Promise<boolean>;
  isWatchAppInstalled(): Promise<boolean>;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Wearables');
