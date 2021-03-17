package com.vungle.plugin.flutter.vungle;

import android.text.TextUtils;
import android.util.Log;

import com.vungle.warren.AdConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** VunglePlugin */
public class VunglePlugin implements MethodCallHandler {

  private static final String TAG = "VunglePlugin";
  //here not way to get the SDK version through SDK's API, so hard coded the version.
  //need to make it matched to the version in the build.gradle
  private static final String SDK_VERSION = "6.9.1";

  private final Registrar registrar;
  private final MethodChannel channel;
  private static final Map<String, Vungle.Consent> strToConsentStatus = new HashMap<>();
  private static final Map<Vungle.Consent, String> consentStatusToStr = new HashMap<>();
  static {
    strToConsentStatus.put("Accepted", Vungle.Consent.OPTED_IN);
    strToConsentStatus.put("Denied", Vungle.Consent.OPTED_OUT);
    consentStatusToStr.put(Vungle.Consent.OPTED_IN, "Accepted");
    consentStatusToStr.put(Vungle.Consent.OPTED_OUT, "Denied");
  }


  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_vungle");
    channel.setMethodCallHandler(new VunglePlugin(registrar, channel));
  }

  private VunglePlugin(Registrar registrar, MethodChannel channel) {
    this.registrar = registrar;
    this.channel = channel;
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if(call.method.equals("init")) {
      callInit(call, result);
    } else if(call.method.equals("loadAd")) {
      callLoadAd(call, result);
    } else if(call.method.equals("playAd")) {
      callPlayAd(call, result);
    } else if(call.method.equals("isAdPlayable")) {
      callIsAdPlayable(call, result);
    } else if(call.method.equals("updateConsentStatus")) {
      callUpdateConsentStatus(call, result);
    } else if (call.method.equals("sdkVersion")) {
      result.success(SDK_VERSION);
    } else if (call.method.equals("enableBackgroundDownload")) {
      // no op for this method.
    } else {
      result.notImplemented();
    }
  }

  private void callInit(MethodCall call, Result result) {
    //init
    final String appId = call.argument("appId");
    if (TextUtils.isEmpty(appId)) {
      result.error("no_app_id", "a null or empty Vungle appId was provided", null);
      return;
    }
    Vungle.init(appId, registrar.context(), new InitCallback() {
      @Override
      public void onSuccess() {
        Log.d(TAG, "Vungle SDK init success");
        channel.invokeMethod("onInitialize", argumentsMap());
      }

      @Override
      public void onError(VungleException exception) {
        Log.e(TAG, "Vungle SDK init failed, ", exception);
      }

      @Override
      public void onAutoCacheAdAvailable(String s) {
        Log.d(TAG, "onAutoCacheAdAvailable, " + s);
      }
    });
    result.success(Boolean.TRUE);
  }

  private void callLoadAd(MethodCall call, Result result) {
    final String placementId = getAndValidatePlacementId(call, result);
    if(placementId == null) {
      return;
    }

    Vungle.loadAd(placementId, new LoadAdCallback() {
      @Override
      public void onAdLoad(String s) {
        Log.d(TAG, "Vungle ad loaded, " + s);
        channel.invokeMethod("onAdPlayable", argumentsMap("placementId", s, "playable", Boolean.TRUE));
      }

      @Override
      public void onError(String s, VungleException exception) {
        Log.e(TAG, "Vungle ad load failed, " + s + ", ", exception);
        channel.invokeMethod("onAdPlayable", argumentsMap("placementId", s, "playable", Boolean.FALSE));
      }
    });

    result.success(Boolean.TRUE);
  }

  private void callPlayAd(MethodCall call, Result result) {
    final String placementId = getAndValidatePlacementId(call, result);
    if(placementId == null) {
      return;
    }

    Vungle.playAd(placementId, new AdConfig(), new PlayAdCallback() {
      @Override
      public void onAdStart(String s) {
        Log.d(TAG, "Vungle ad started, " + s);
        channel.invokeMethod("onAdStarted", argumentsMap("placementId", s));
      }

      @Override
      public void onAdEnd(String s, boolean b, boolean b1) {
        Log.d(TAG, "Vungle ad finished, " + b + ", " + b1);
        channel.invokeMethod("onAdFinished",
                argumentsMap("placementId", s, "isCTAClicked", b, "isCompletedView", b1));
      }

      @Override
      public void onError(String s, VungleException exception) {
        Log.e(TAG, "Vungle ad play failed, " + s + ", ", exception);
      }

      @Override
      public void onAdEnd(String id) {

      }

      @Override
      public void onAdClick(String id) {

      }

      @Override
      public void onAdRewarded(String id) {

      }

      @Override
      public void onAdLeftApplication(String id) {

      }

      @Override
      public void onAdViewed(String id) {

      }
    });
    result.success(Boolean.TRUE);
  }

  private void callIsAdPlayable(MethodCall call, Result result) {
    String placementId = getAndValidatePlacementId(call, result);
    if(placementId != null) {
      result.success(Vungle.canPlayAd(placementId));
    }
  }

  private void callGetConsentStatus(MethodCall call, Result result) {
    
  }

  private void callUpdateConsentStatus(MethodCall call, Result result) {
    String consentStatus = call.argument("consentStatus");
    String consentMessageVersion = call.argument("consentMessageVersion");
    if(TextUtils.isEmpty(consentStatus) || TextUtils.isEmpty(consentMessageVersion)) {
      result.error("no_consent_status", "Null or empty consent status / message version was provided", null);
      return;
    }
    Vungle.Consent consent = strToConsentStatus.get(consentStatus);
    if(consent == null) {
      result.error("invalid_consent_status", "Invalid consent status was provided", null);
      return;
    }
    Vungle.updateConsentStatus(consent, consentMessageVersion);
  }

  private Map<String, Object> argumentsMap(Object... args) {
    Map<String, Object> arguments = new HashMap<>();
    for (int i = 0; i < args.length; i += 2) arguments.put(args[i].toString(), args[i + 1]);
    return arguments;
  }

  private String getAndValidatePlacementId(MethodCall call, Result result) {
    final String placementId = call.argument("placementId");
    if (TextUtils.isEmpty(placementId)) {
      result.error("no_placement_id", "null or empty Vungle placementId was provided", null);
      return null;
    }
    return placementId;
  }
}
