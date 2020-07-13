#import "BackgroundModePlugin.h"
#import "AppMethodMagic.h"

@implementation BackgroundModePlugin

#pragma mark -
#pragma mark Constants

NSString* const kAPPBackgroundNamespace = @"flutter.plugins.backgroundMode";
NSString* const kAPPBackgroundEventActivate = @"activate";
NSString* const kAPPBackgroundEventDeactivate = @"deactivate";

#pragma mark -
#pragma mark Life Cycle

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"background_mode"
            binaryMessenger:[registrar messenger]];
  BackgroundModePlugin* instance = [[BackgroundModePlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  } else {
    result(FlutterMethodNotImplemented);
  }
}

/**
 * Called by runtime once the Class has been loaded.
 * Exchange method implementations to hook into their execution.
 */
+ (void) load
{
    [self swizzleWKWebViewEngine];
}

/**
 * Initialize the plugin.
 */
- (void) pluginInitialize
{
    enabled = NO;
    [self configureAudioPlayer];
    [self configureAudioSession];
    [self observeLifeCycle];
}

/**
 * Register the listener for pause and resume events.
 */
- (void) observeLifeCycle
{
    NSNotificationCenter* listener = [NSNotificationCenter
                                      defaultCenter];

        [listener addObserver:self
                     selector:@selector(keepAwake)
                         name:UIApplicationDidEnterBackgroundNotification
                       object:nil];

        [listener addObserver:self
                     selector:@selector(stopKeepingAwake)
                         name:UIApplicationWillEnterForegroundNotification
                       object:nil];

        [listener addObserver:self
                     selector:@selector(handleAudioSessionInterruption:)
                         name:AVAudioSessionInterruptionNotification
                       object:nil];
}

#pragma mark -
#pragma mark Interface

/**
 * Enable the mode to stay awake
 * when switching to background for the next time.
 */
- (void) enable:(CDVInvokedUrlCommand*)command
{
    if (enabled)
        return;

    enabled = YES;
    [self execCallback:command];
}

/**
 * Disable the background mode
 * and stop being active in background.
 */
- (void) disable:(CDVInvokedUrlCommand*)command
{
    if (!enabled)
        return;

    enabled = NO;
    [self stopKeepingAwake];
    [self execCallback:command];
}

#pragma mark -
#pragma mark Core

/**
 * Keep the app awake.
 */
- (void) keepAwake
{
    if (!enabled)
        return;

    [audioPlayer play];
    [self fireEvent:kAPPBackgroundEventActivate];
}

/**
 * Let the app going to sleep.
 */
- (void) stopKeepingAwake
{
    if (TARGET_IPHONE_SIMULATOR) {
        NSLog(@"BackgroundMode: On simulator apps never pause in background!");
    }

    if (audioPlayer.isPlaying) {
        [self fireEvent:kAPPBackgroundEventDeactivate];
    }

    [audioPlayer pause];
}

/**
 * Configure the audio player.
 */
- (void) configureAudioPlayer
{
    NSString* path = [[NSBundle mainBundle]
                      pathForResource:@"appbeep" ofType:@"wav"];

    NSURL* url = [NSURL fileURLWithPath:path];


    audioPlayer = [[AVAudioPlayer alloc]
                   initWithContentsOfURL:url error:NULL];

    audioPlayer.volume        = 0;
    audioPlayer.numberOfLoops = -1;
};

/**
 * Configure the audio session.
 */
- (void) configureAudioSession
{
    AVAudioSession* session = [AVAudioSession
                               sharedInstance];

    // Don't activate the audio session yet
    [session setActive:NO error:NULL];

    // Play music even in background and dont stop playing music
    // even another app starts playing sound
    [session setCategory:AVAudioSessionCategoryPlayback
                   error:NULL];

    // Active the audio session
    [session setActive:YES error:NULL];
};

#pragma mark -
#pragma mark Helper

/**
 * Simply invokes the callback without any parameter.
 */
- (void) execCallback:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult *result = [CDVPluginResult
                               resultWithStatus:CDVCommandStatus_OK];

    [self.commandDelegate sendPluginResult:result
                                callbackId:command.callbackId];
}

/**
 * Restart playing sound when interrupted by phone calls.
 */
- (void) handleAudioSessionInterruption:(NSNotification*)notification
{
    [self fireEvent:kAPPBackgroundEventDeactivate];
    [self keepAwake];
}

/**
 * Method to fire an event with some parameters in the browser.
 */
- (void) fireEvent:(NSString*)event
{
    NSString* active =
    [event isEqualToString:kAPPBackgroundEventActivate] ? @"true" : @"false";

    NSString* flag = [NSString stringWithFormat:@"%@._isActive=%@;",
                      kAPPBackgroundNamespace, active];

    NSString* depFn = [NSString stringWithFormat:@"%@.on('%@');",
                       kAPPBackgroundNamespace, event];

    NSString* fn = [NSString stringWithFormat:@"%@.fireEvent('%@');",
                    kAPPBackgroundNamespace, event];

    NSString* js = [NSString stringWithFormat:@"%@%@%@", flag, depFn, fn];

    [self.commandDelegate evalJs:js];
}

#pragma mark -
#pragma mark Swizzling

/**
 * Method to swizzle.
 */
+ (NSString*) wkProperty
{
    NSString* str = @"YWx3YXlzUnVuc0F0Rm9yZWdyb3VuZFByaW9yaXR5";
    NSData* data  = [[NSData alloc] initWithBase64EncodedString:str options:0];

    return [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding];
}


@end
