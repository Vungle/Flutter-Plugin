#import <Foundation/Foundation.h>
#import "VunglePlugin.h"
#import <VungleSDK/VungleSDK.h>

@interface VunglePlugin() <VungleSDKDelegate>
@property(nonatomic, strong) FlutterMethodChannel *channel;
@property(nonatomic, weak) VungleSDK *sdk;
@end

@implementation VunglePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"flutter_vungle"
                                     binaryMessenger:[registrar messenger]];
    VunglePlugin* instance = [[VunglePlugin alloc] init];
    instance.channel = channel;
    [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    if ([@"init" isEqualToString:call.method]) {
        [self callInit:call result:result];
    } else if([@"loadAd" isEqualToString:call.method]) {
        [self callLoadAd:call result:result];
    } else if([@"playAd" isEqualToString:call.method]) {
        [self callPlayAd:call result:result];
    } else if([@"isAdPlayable" isEqualToString:call.method]) {
        [self callIsAdPlayable:call result:result];
    } else if([@"sdkVersion" isEqualToString:call.method]) {
        result(VungleSDKVersion);
    } else if([@"enableBackgroundDownload" isEqualToString:call.method]) {
        [self callEnableBackgroundDownload:call result:result];
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (void)callInit:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString* appId = (NSString*)call.arguments[@"appId"];
    if(appId.length == 0) {
        result([FlutterError errorWithCode:@"no_app_id"
                                   message:@"a nil or empty Vungle appId was provided"
                                   details:nil]);
    }
    NSError *error = nil;
    _sdk = [VungleSDK sharedSDK];
    _sdk.delegate = self;
    [VungleSDK enabledBackgroundDownload:YES];
    [_sdk startWithAppId:appId error:&error];
}

- (void)callLoadAd:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString *placementId = [self getAndValidatePlacementId:call result:result];
    if(placementId != nil) {
        NSError *error = nil;
        [_sdk loadPlacementWithID:placementId error:&error];
    }
    result([NSNumber numberWithBool:YES]);
}

- (void)callPlayAd:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString *placementId = [self getAndValidatePlacementId:call result:result];
    if(placementId != nil) {
        NSError *error = nil;
        UIViewController *rootVC = [UIApplication sharedApplication].delegate.window.rootViewController;
        [_sdk playAd:rootVC options:nil placementID:placementId error:&error];
    }
    result([NSNumber numberWithBool:YES]);
}

- (void)callIsAdPlayable:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString *placementId = [self getAndValidatePlacementId:call result:result];
    if(placementId != nil) {
        result([NSNumber numberWithBool:[_sdk isAdCachedForPlacementID:placementId]]);
    }
}

- (void)callEnableBackgroundDownload:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSNumber *enabled = call.arguments[@"enabled"];
    [VungleSDK enableBackgroundDownload:enabled.boolValue];
}

- (NSString *)getAndValidatePlacementId:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSString *placementId = call.arguments[@"placementId"];
    if(placementId.length == 0) {
        result([FlutterError errorWithCode:@"no_placement_id"
                                   message:@"a nil or empty Vungle placementId was provided"
                                   details:nil]);
        return nil;
    }
    return placementId;
}

#pragma mark - VungleSDKDelegate methods
- (void)vungleSDKDidInitialize {
    NSLog(@"vungleSDKDidInitialize");
    [_channel invokeMethod:@"onInitialize" arguments:@{}];
}

- (void)vungleSDKFailedToInitializeWithError:(NSError *)error {
    NSLog(@"vungleSDKFailedToInitializeWithError, %@", error);
}

- (void)vungleAdPlayabilityUpdate:(BOOL)isAdPlayable placementID:(nullable NSString *)placementID error:(nullable NSError *)error {
    NSLog(@"vungleAdPlayabilityUpdate:%@ placementID:%@ error:%@", @(isAdPlayable), placementID, error);
    [_channel invokeMethod:@"onAdPlayable"
                 arguments:@{@"placementId":placementID != nil ? placementID : @"",
                             @"playable": @(isAdPlayable),}];
    
}

- (void)vungleWillShowAdForPlacementID:(nullable NSString *)placementID {
    NSLog(@"vungleWillShowAdForPlacementID:%@", placementID);
    [_channel invokeMethod:@"onAdStarted"
                 arguments:@{@"placementId":placementID != nil ? placementID : @""}];
}

- (void)vungleDidCloseAdWithViewInfo:(nonnull VungleViewInfo *)info placementID:(nonnull NSString *)placementID {
    NSLog(@"vungleDidCloseAdWithViewInfo:%@, placementId:%@", info, placementID);
    [_channel invokeMethod:@"onAdFinished"
                 arguments:@{@"placementId": placementID != nil ? placementID : @"",
                             @"isCTAClicked": info.didDownload,
                             @"isCompletedView": info.completedView,}];
}





@end
