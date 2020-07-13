#import <Flutter/Flutter.h>
#import <AVFoundation/AVFoundation.h>

@interface BackgroundModePlugin : NSObject <FlutterPlugin>
{
    AVAudioPlayer *audioPlayer;
    BOOL enabled;
}

// Activate the background mode
- (void)enable:(CDVInvokedUrlCommand *)command;
// Deactivate the background mode
- (void)disable:(CDVInvokedUrlCommand *)command;

@end
