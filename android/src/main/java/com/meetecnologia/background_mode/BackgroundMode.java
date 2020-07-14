package com.meetecnologia.background_mode;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.json.JSONObject;

import static android.content.Context.BIND_AUTO_CREATE;

public class BackgroundMode {

    private static final String NAMESPACE = "flutter.plugins.backgroundMode";

    // Flag indicates if the service is bind
    private boolean isBind = false;

    // Service that keeps the app awake
    private ForegroundService service;

    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundService.ForegroundBinder binder = (ForegroundService.ForegroundBinder) service;
            BackgroundMode.this.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
}
