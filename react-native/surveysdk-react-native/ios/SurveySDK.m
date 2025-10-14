#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(SurveySDK, NSObject)

RCT_EXTERN_METHOD(initialize:(NSString *)apiKey
        resolver:(RCTPromiseResolveBlock)resolve
        rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(showSurvey:(RCTPromiseResolveBlock)resolve
        rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(setUserProperty:(NSString *)key
        value:(NSString *)value
        resolver:(RCTPromiseResolveBlock)resolve
        rejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(trackEvent:(NSString *)eventName
        properties:(NSDictionary *)properties
        resolver:(RCTPromiseResolveBlock)resolve
        rejecter:(RCTPromiseRejectBlock)reject)

@end