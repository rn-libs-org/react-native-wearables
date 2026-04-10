#import "Wearables.h"
#import <WatchConnectivity/WatchConnectivity.h>

@interface Wearables () <WCSessionDelegate>
@property (nonatomic, assign) BOOL hasListeners;
@end

@implementation Wearables

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (instancetype)init {
  self = [super init];
  if (self) {
    if ([WCSession isSupported]) {
      WCSession *session = [WCSession defaultSession];
      session.delegate = self;
      [session activateSession];
    }
  }
  return self;
}

- (NSArray<NSString *> *)supportedEvents {
  return @[@"onMessageReceived"];
}

- (void)startObserving {
  _hasListeners = YES;
}

- (void)stopObserving {
  _hasListeners = NO;
}

- (void)sendMessage:(NSDictionary *)message
            resolve:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject {
  if (![WCSession isSupported]) {
    reject(@"ERR_UNSUPPORTED", @"WatchConnectivity is not supported on this device", nil);
    return;
  }

  WCSession *session = [WCSession defaultSession];

  if (!session.isReachable) {
    reject(@"ERR_UNREACHABLE", @"Watch is not reachable", nil);
    return;
  }

  [session sendMessage:message
          replyHandler:^(NSDictionary<NSString *,id> * _Nonnull replyMessage) {
    resolve(nil);
  }
          errorHandler:^(NSError * _Nonnull error) {
    reject(@"ERR_SEND_FAILED", error.localizedDescription, error);
  }];
}

- (void)isPaired:(RCTPromiseResolveBlock)resolve
          reject:(RCTPromiseRejectBlock)reject {
  if (![WCSession isSupported]) {
    resolve(@(NO));
    return;
  }
  resolve(@([WCSession defaultSession].isPaired));
}

- (void)isReachable:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject {
  if (![WCSession isSupported]) {
    resolve(@(NO));
    return;
  }
  resolve(@([WCSession defaultSession].isReachable));
}

- (void)isWatchAppInstalled:(RCTPromiseResolveBlock)resolve
                     reject:(RCTPromiseRejectBlock)reject {
  if (![WCSession isSupported]) {
    resolve(@(NO));
    return;
  }
  resolve(@([WCSession defaultSession].isWatchAppInstalled));
}

- (void)session:(WCSession *)session
    activationDidCompleteWithState:(WCSessionActivationState)activationState
                             error:(NSError *)error {
  // Required delegate method — session activation completed
}

- (void)sessionDidBecomeInactive:(WCSession *)session {
  // Required on iOS — called when session is about to be deactivated
}

- (void)sessionDidDeactivate:(WCSession *)session {
  // Required on iOS — reactivate session to support watch switching
  [[WCSession defaultSession] activateSession];
}

- (void)session:(WCSession *)session
    didReceiveMessage:(NSDictionary<NSString *, id> *)message {
  if (_hasListeners) {
    [self sendEventWithName:@"onMessageReceived" body:message];
  }
}

- (void)session:(WCSession *)session
    didReceiveMessage:(NSDictionary<NSString *, id> *)message
         replyHandler:(void (^)(NSDictionary<NSString *, id> * _Nonnull))replyHandler {
  if (_hasListeners) {
    [self sendEventWithName:@"onMessageReceived" body:message];
  }
  replyHandler(@{});
}

- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params {
  return std::make_shared<facebook::react::NativeWearablesSpecJSI>(params);
}

@end
