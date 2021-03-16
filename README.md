# vungle

A plugin for [Flutter](https://fluter.io) that supports loading and displaying interstitial and rewarded video ads using the [Vungle SDK API](https://vungle.com/vungle-sdk/).

Note: This plugin is in beta, and may still have a few issues and missing APIs. Feedback and Pull Requests are welcome.

## Getting Started

Please go to the [Vungle](https://www.vungle.com) website to create account for your apps at first. You need add your apps there and you will get the ```app id``` and ```placement ids```, then use this plugin in your flutter app to do the monetization.

### Initialize the plugin

```dart
if (Platfrom.isAndrod) {
  Vungle.init('[vungle_android_app_id]');
} else {
  //for iOS
  Vungle.init('[vungle_ios_app_id]');
}

//You need wait until the plugin initialized to load and show ads
Vungle.onInitializeListener = () {
  //The plugin initialized, can load ads for now
}

```

### Load Interstitial or rewarded video ads
```dart
Vungle.loadAd('[vungle_placement_id]');

//To know if the ad loaded
Vungle.onAdPlayableListener = (placementId, playable) {
  if(playable) {
    //The ad has been loaded, could play it for now.
  }
}
```

### Play Interstitial or rewarded video ads
```dart
if(Vungle.isAdPlayable('[your_placement_id]') {
  Vungle.playAd(placementId);
}

Vungle.onAdStatedListener = (placementId) {
  //Ad started to play  
}

Vungle.onAdFinishedListener = (placementId, isCTAClicked, isCompletedView) {
  //Ad finished to play
  //isCTAClicked - User has clicked the `download` button
  //isCompletedView - User has viewed the video ad completely
  
}
```
