import 'package:flutter/services.dart';

class BackgroundMode {
  static const MethodChannel _bringToForeground =
      const MethodChannel('background_mode.bringToForeground');

  static const MethodChannel _start =
      const MethodChannel('background_mode.start');

  static const MethodChannel _disable =
      const MethodChannel('background_mode.disable');

  static void bringToForeground() {
    _bringToForeground.invokeMethod("background_mode.bringToForeground");
  }

  static void start() {
    _start.invokeMethod('background_mode.start');
  }

  static void disable() {
    _disable.invokeMethod('background_mode.disable');
  }
}
