#import "SurveySDKPlugin.h"
#import <surveysdk/surveysdk-Swift.h>

@implementation SurveySDKPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
            methodChannelWithName:@"surveysdk"
                  binaryMessenger:[registrar messenger]];
    SurveySDKPlugin* instance = [[SurveySDKPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([@"initialize" isEqualToString:call.method]) {
        NSString* apiKey = call.arguments[@"apiKey"];
        // Initialize SDK - you'll need to create iOS implementation
        result(nil);
    } else if ([@"showSurvey" isEqualToString:call.method]) {
        // Show survey - you'll need to create iOS implementation
        result(nil);
    } else if ([@"setUserProperty" isEqualToString:call.method]) {
        NSString* key = call.arguments[@"key"];
        NSString* value = call.arguments[@"value"];
        // Set user property
        result(nil);
    } else if ([@"trackEvent" isEqualToString:call.method]) {
        NSString* eventName = call.arguments[@"eventName"];
        NSDictionary* properties = call.arguments[@"properties"];
        // Track event
        result(nil);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

@end