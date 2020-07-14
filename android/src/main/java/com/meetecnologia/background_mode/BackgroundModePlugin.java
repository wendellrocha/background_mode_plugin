package com.meetecnologia.background_mode;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.json.JSONObject;

import java.util.List;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Context.BIND_AUTO_CREATE;
import static android.content.Context.KEYGUARD_SERVICE;
import static android.content.Context.POWER_SERVICE;
import static android.os.Build.VERSION.SDK_INT;
import static android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON;
import static android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

/**
 * BackgroundModePlugin
 */
public class BackgroundModePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {

    private MethodChannel channel;
    private MethodChannel start;
    private MethodChannel disable;
    private Context applicationContext;
    private Activity mainActivity;
    private boolean isBind = false;
    private PowerManager.WakeLock wakeLock;

    public static void registerWith(Registrar registrar) {
        BackgroundModePlugin instance = new BackgroundModePlugin();
        instance.setActivity(registrar.activity());
        instance.onAttachedToEngine(registrar.context(), registrar.messenger());
    }

    public void onAttachedToEngine(Context context, BinaryMessenger messenger) {
        this.applicationContext = context;
        channel = new MethodChannel(messenger,
                "background_mode.bringToForeground");
        channel.setMethodCallHandler(this);

        start = new MethodChannel(messenger, "background_mode.start");
        start.setMethodCallHandler(this);

        disable = new MethodChannel(messenger,
                "background_mode.disable");
        disable.setMethodCallHandler(this);
    }

    private void setActivity(Activity flutterActivity) {
        this.mainActivity = flutterActivity;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("background_mode.bringToForeground")) {
            Intent intent = applicationContext.getPackageManager().getLaunchIntentForPackage(applicationContext.getPackageName());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            applicationContext.startActivity(intent);
            result.success(null);
        } else if (call.method.equals("background_mode.start")) {
            startService(applicationContext);
            acquireWakeLock(applicationContext);
            disableBatteryOptimizations(applicationContext);
            clearKeyguardFlags(applicationContext);
            result.success(null);
        } else if (call.method.equals("background_mode.disable")) {
            releaseWakeLock();
            if (SDK_INT >= Build.VERSION_CODES.O) {
                clearKeyguardFlags(applicationContext);
            }
        } else {
            result.notImplemented();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void disableBatteryOptimizations(Context context) {
        Intent intent = new Intent();
        String packageName = context.getPackageName();
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } else {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
        }
    }

    /**
     * Bind the activity to a background service and put them into foreground state.
     */
    private void startService(Context context) {
        try {
            Intent intent = new Intent(context, ForegroundService.class);
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            context.startService(intent);
        } catch (Exception e) {
            Log.i("BackgroundModePlugin", e.getMessage());
        }

        isBind = true;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        releaseWakeLock();
    }

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        onAttachedToEngine(binding.getApplicationContext(), binding.getBinaryMessenger());
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        this.mainActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.mainActivity = null;
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        this.mainActivity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
        this.mainActivity = null;
    }

    private final ServiceConnection connection = new ServiceConnection() {
        private ForegroundService service;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundService.ForegroundBinder binder = (ForegroundService.ForegroundBinder) service;
            this.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void clearKeyguardFlags(Context context) {
        Window window = mainActivity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    /**
     * Acquires a wake lock to wake up the device.
     */
    private void acquireWakeLock(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);

        releaseWakeLock();

        int level = PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_DIM_WAKE_LOCK;
        wakeLock = pm.newWakeLock(level, "backgroundmode:wakelock");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(1000);
    }

    /**
     * Releases the previously acquire wake lock.
     */
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}
