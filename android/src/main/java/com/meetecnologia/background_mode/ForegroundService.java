package com.meetecnologia.background_mode;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.app.NotificationChannel;

import org.json.JSONObject;

import static android.os.PowerManager.PARTIAL_WAKE_LOCK;

public class ForegroundService extends Service {

    // Fixed ID for the 'foreground' notification
    public static final int NOTIFICATION_ID = -574543954;

    // Default title of the background notification
    private static final String NOTIFICATION_TITLE =
            "App is running in background";

    // Default text of the background notification
    private static final String NOTIFICATION_TEXT =
            "Doing heavy tasks.";

    // Binder given to clients
    private final IBinder binder = new ForegroundBinder();

    // Partial wake lock to prevent the app from going to sleep when locked
    private PowerManager.WakeLock wakeLock;

    /**
     * Allow clients to call on to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class ForegroundBinder extends Binder {
        ForegroundService getService() {
            // Return this instance of ForegroundService
            // so clients can call public methods
            return ForegroundService.this;
        }
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        keepAwake();
    }

    /**
     * No need to run headless on destroy.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        sleepWell();
    }

    /**
     * Prevent Android from stopping the background service automatically.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @SuppressLint("WakelockTimeout")
    private void keepAwake() {
        startForeground(NOTIFICATION_ID, makeNotification());

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

        wakeLock = pm.newWakeLock(PARTIAL_WAKE_LOCK, "backgroundmode:wakelock");

        wakeLock.acquire();
    }

    /**
     * Stop background mode.
     */
    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private void sleepWell() {
        stopForeground(true);
        getNotificationManager().cancel(NOTIFICATION_ID);

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     */
    private Notification makeNotification() {
        String CHANNEL_ID = "background_mode-id";
        if (Build.VERSION.SDK_INT >= 26) {
            CharSequence name = "background_mode";
            String description = "background_mode-moden notification";

            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);

            mChannel.setDescription(description);

            getNotificationManager().createNotificationChannel(mChannel);
        }

        String title = NOTIFICATION_TITLE;
        String text = NOTIFICATION_TEXT;

        Context context = getApplicationContext();
        String pkgName = context.getPackageName();
        Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        Notification.Builder notification = new Notification.Builder(context)
                .setContentTitle(title)
                .setContentText(text)
                .setOngoing(true)
                .setSmallIcon(android.R.color.transparent);

        if (Build.VERSION.SDK_INT >= 26) {
            notification.setChannelId(CHANNEL_ID);
        }

        notification.setPriority(Notification.PRIORITY_MAX);

        notification.setStyle(
                new Notification.BigTextStyle().bigText(text));


        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        notification.setContentIntent(contentIntent);

        return notification.build();
    }


    /**
     * Returns the shared notification service manager.
     */
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
}
