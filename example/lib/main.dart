import 'dart:async';

import 'package:flutter/material.dart';
import 'package:background_mode/background_mode.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  void initPlatformState() async {
    BackgroundMode.start();

    Timer.periodic(Duration(seconds: 10), (timer) {
      BackgroundMode.disable();
      BackgroundMode.bringToForeground();
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Example'),
        ),
      ),
    );
  }
}
