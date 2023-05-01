package tv.vizbee.demoapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

import tv.vizbee.assist.AssistServiceManager;
import tv.vizbee.assist.utils.Logger;

public class AssistMdnsService extends Service {

    private static final String TAG = "AssistMdnsService";

    private AssistServiceManager mAssistServiceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "onCreate");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "onStartCommand");

        Toast.makeText(getApplicationContext(), "Started ASSIST MDNS service", Toast.LENGTH_LONG).show();

        mAssistServiceManager = new AssistServiceManager();
        try {
            mAssistServiceManager.registerService(this.getApplicationContext());
        } catch (IOException e) {
            Logger.e(TAG, "Failed to register service %s", e);
        }

        //---
        // Start of StartForegroundService
        //---

        /*
        Starting the service as a foreground to avoid system stopping the service when an
        activity is idle. This happens when the PlayStore is shown and the  app is getting installed.
        This is not required when we run the service as a System service.
         */

        NotificationChannel chan = new NotificationChannel(
                getApplicationContext().getPackageName(),
                "My Foreground Service",
                NotificationManager.IMPORTANCE_LOW);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, getPackageName());
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running on foreground")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setChannelId(getPackageName())
                .build();
        startForeground(1, notification);

        //---
        // End of StartForegroundService
        //---

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d(TAG, "onDestroy");

        mAssistServiceManager.unregisterService();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
