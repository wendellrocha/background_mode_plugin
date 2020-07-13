import 'dart:async';

import 'package:flutter/services.dart';

class BackgroundMode {
  static const MethodChannel _channel =
      const MethodChannel('background_mode');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
