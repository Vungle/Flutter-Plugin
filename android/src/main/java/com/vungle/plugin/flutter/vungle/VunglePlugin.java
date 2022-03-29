package com.vungle.plugin.flutter.vungle;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.vungle.warren.AdConfig;
import com.vungle.warren.BuildConfig;
import com.vungle.warren.InitCallback;
import com.vungle.warren.LoadAdCallback;
import com.vungle.warren.PlayAdCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.error.VungleException;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** VunglePlugin */
public class VunglePlugin implements FlutterPlugin, MethodCallHandler {
  private static final String TAG = "VunglePlugin";
  private static final String channelName = "flutter_vungle";

  private Context context;
  private MethodChannel channel;
  private static final Map<String, Vungle.Consent> strToConsentStatus = new HashMap<>();
  private static final Map<Vungle.Consent, String> consentStatusToStr = new HashMap<>();
  static {
    strToConsentStatus.put("Accepted", Vungle.Consent.OPTED_IN);
    strToConsentStatus.put("Denied", Vungle.Consent.OPTED_OUT);
    consentStatusToStr.put(Vungle.Consent.OPTED_IN, "Accepted");
    consentStatusToStr.put(Vungle.Consent.OPTED_OUT, "Denied");
  }

  /** v1 Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), channelName);
    channel.setMethodCallHandler(new VunglePlugin(registrar.context(), channel));
  }

  /** v2 Plugin registration */
  private static void setup(VunglePlugin plugin, BinaryMessenger binaryMessenger) {
    plugin.channel = new MethodChannel(binaryMessenger, channelName);
    plugin.channel.setMethodCallHandler(plugin);
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    this.context = binding.getApplicationContext();
    setup(this, binding.getBinaryMessenger());
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    this.context = null;
  }

  public VunglePlugin() {
    // All Android plugin classes must support a no-args 
    // constructor for v2.
  }

  private VunglePlugin(Context context, MethodChannel channel) {
    this.context = context;
    this.channel = channel;
  }

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
      result.success(BuildConfig.VERSION_NAME);
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
    Vungle.init(appId, this.context, new InitCallback() {
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
        Log.d(TAG, "Vungle ad end, " + id);
        channel.invokeMethod("onAdEnd", argumentsMap("placementId", id));
      }

      @Override
      public void onAdClick(String id) {
        Log.d(TAG, "Vungle ad clicked, " + id);
        channel.invokeMethod("onAdClicked", argumentsMap("placementId", id));
      }

      @Override
      public void onAdRewarded(String id) {
        Log.d(TAG, "Vungle ad rewarded, " + id);
        channel.invokeMethod("onAdRewarded", argumentsMap("placementId", id));
      }

      @Override
      public void onAdLeftApplication(String id) {
        Log.d(TAG, "Vungle ad left application, " + id);
        channel.invokeMethod("onAdLeftApplication", argumentsMap("placementId", id));
      }

      @Override
      public void onAdViewed(String id) {
        Log.d(TAG, "Vungle ad viewed, " + id);
        channel.invokeMethod("onAdViewed", argumentsMap("placementId", id));
      }

      @Override
      public void creativeId(String id) {
        Log.d(TAG, "Vungle creative id, " + id);
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
