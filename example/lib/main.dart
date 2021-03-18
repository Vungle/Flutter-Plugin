import 'package:flutter/material.dart';
import 'dart:io';

import 'package:vungle/vungle.dart';

void main() {
  String appId;
  String placementId;
  if (Platform.isAndroid) {
    appId = '5adff6afb2cadf62871219ff';
    placementId = 'DEFAULT-3224603';
  } else {
    //iOS
    appId = '5aa9e7dc7db3b73d270148e7';
    placementId = 'DEFAULT-1337376';
  }

  var app = MyApp(appId, placementId);
  runApp(app);
}

class MyApp extends StatefulWidget {
  final String placementId;
  final String appId;
  MyApp(this.appId, this.placementId);

  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  bool sdkInit = false;
  bool adLoaded = false;
  String sdkVersion;

  String get appId => widget.appId;
  String get placementId => widget.placementId;

  @override
  void initState() {
    super.initState();

    Vungle.getSDKVersion().then((value) => setState(() {
          sdkVersion = value;
        }));

    Vungle.onInitilizeListener = () {
      setState(() {
        sdkInit = true;
      });
    };

    Vungle.onAdPlayableListener = (placemenId, playable) {
      if (playable) {
        setState(() {
          adLoaded = true;
        });
      }
    };

    Vungle.onAdStartedListener = (placementId) {
      print('ad started');
    };

    Vungle.onAdFinishedListener = (placementId, isCTAClicked, completedView) {
      print(
          'ad finished, isCTAClicked:($isCTAClicked), completedView:($completedView)');
      setState(() {
        adLoaded = false;
      });
    };
  }

  void onInit() {
    Vungle.init(appId);
  }

  void onLoadAd() {
    Vungle.loadAd(placementId);
  }

  void onPlayAd() async {
    if (await Vungle.isAdPlayable(placementId)) {
      Vungle.playAd(placementId);
    } else {
      print('The ad is not ready to play');
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              ElevatedButton(
                child: Text(sdkInit
                    ? 'Vungle SDK initialized - $appId'
                    : 'Init Vungle SDK - $appId'),
                onPressed: sdkInit ? null : onInit,
              ),
              ElevatedButton(
                child: Text(adLoaded
                    ? 'Ad Loaded - $placementId'
                    : 'Load Ad - $placementId'),
                onPressed: adLoaded ? null : onLoadAd,
              ),
              ElevatedButton(
                child: Text('Play Ad - $placementId'),
                onPressed: adLoaded ? onPlayAd : null,
              ),
              SizedBox(
                height: 20,
              ),
              Text('SDK Version: ${sdkVersion ?? ''}'),
            ],
          ),
        ),
      ),
    );
  }
}
