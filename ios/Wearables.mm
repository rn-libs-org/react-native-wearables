#import "Wearables.h"
#import <WatchConnectivity/WatchConnectivity.h>

@interface Wearables () <WCSessionDelegate>
@property (nonatomic, assign) BOOL hasListeners;
@property (nonatomic, assign) BOOL isSessionActivated;
@property (nonatomic, strong) NSDictionary *pendingMessage;
@property (nonatomic, copy) RCTPromiseResolveBlock pendingResolve;
@property (nonatomic, copy) RCTPromiseRejectBlock pendingReject;
@end

@implementation Wearables

RCT_EXPORT_MODULE()

+ (BOOL)requiresMainQueueSetup {
  return YES;
}

- (instancetype)init {
  self = [super init];
  if (self) {
    _isSessionActivated = NO;
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

  if (!self.isSessionActivated) {
    self.pendingMessage = message;
    self.pendingResolve = resolve;
    self.pendingReject = reject;
    return;
  }

  [self processMessageToWatch:message resolve:resolve reject:reject];
}

- (void)processMessageToWatch:(NSDictionary *)message
                       resolve:(RCTPromiseResolveBlock)resolve
                        reject:(RCTPromiseRejectBlock)reject {
  WCSession *session = [WCSession defaultSession];

  if (!session.isPaired) {
    reject(@"ERR_NOT_PAIRED", @"No Apple Watch is paired with this iPhone.", nil);
    return;
  }

  if (!session.isWatchAppInstalled) {
    reject(@"ERR_APP_NOT_INSTALLED", @"The watch app is not installed on the paired Apple Watch.", nil);
    return;
  }

  if (session.isReachable) {
    [session sendMessage:message
            replyHandler:^(NSDictionary<NSString *,id> * _Nonnull replyMessage) {
      resolve(nil);
    }
            errorHandler:^(NSError * _Nonnull error) {
      [self fallbackToApplicationContext:message resolve:resolve reject:reject];
    }];
  } else {
    [self fallbackToApplicationContext:message resolve:resolve reject:reject];
  }
}

- (void)fallbackToApplicationContext:(NSDictionary *)message
                              resolve:(RCTPromiseResolveBlock)resolve
                               reject:(RCTPromiseRejectBlock)reject {
  NSError *error = nil;
  [[WCSession defaultSession] updateApplicationContext:message error:&error];

  if (error) {
    reject(@"ERR_CONTEXT_UPDATE_FAILED", error.localizedDescription, error);
  } else {
    resolve(nil);
  }
}

- (void)processPendingMessage {
  if (self.pendingMessage && self.pendingResolve && self.pendingReject) {
    NSDictionary *message = self.pendingMessage;
    RCTPromiseResolveBlock resolve = self.pendingResolve;
    RCTPromiseRejectBlock reject = self.pendingReject;

    self.pendingMessage = nil;
    self.pendingResolve = nil;
    self.pendingReject = nil;

    [self processMessageToWatch:message resolve:resolve reject:reject];
  }
}

- (void)rejectPendingMessage:(NSString *)code message:(NSString *)message {
  if (self.pendingReject) {
    self.pendingReject(code, message, nil);
  }
  self.pendingMessage = nil;
  self.pendingResolve = nil;
  self.pendingReject = nil;
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
  if (error) {
    [self rejectPendingMessage:@"ERR_SESSION_ACTIVATION_FAILED"
                       message:error.localizedDescription];
    return;
  }

  if (activationState == WCSessionActivationStateActivated) {
    self.isSessionActivated = YES;
    [self processPendingMessage];
  } else {
    [self rejectPendingMessage:@"ERR_SESSION_ACTIVATION_FAILED"
                       message:@"WatchConnectivity session failed to activate."];
  }
}

- (void)sessionDidBecomeInactive:(WCSession *)session {
  // Required on iOS — called when session is about to be deactivated
}

- (void)sessionDidDeactivate:(WCSession *)session {
  self.isSessionActivated = NO;
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
