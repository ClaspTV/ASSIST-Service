package tv.vizbee.assist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.ServerSocket;

public class AssistMdnsService extends Service {

    private static final String TAG = "AssistMdnsService";

    private static final String ASSIST_MDNS_SERVICE_TYPE = "_vzb-assist._tcp.";
    private static final String ASSIST_MDNS_SERVICE_NAME = "Android TV Second Screen Install Service";

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    private AssistHttpServer server;
    private int availablePort;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        try {
            availablePort = getAvailablePort();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            registerService(5353);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        Toast.makeText(getApplicationContext(), "Started ASSIST MDNS service", Toast.LENGTH_LONG).show();

        //---
        // Start of StartForegroundService
        //---

        // Starting the service as a foreground to avoid system stopping the service
        // when an activity is idle. This happens when the PlayStore is shown and the
        // app is getting installed
        //
        // This is not required when we run the service as a System service.
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

        // NanoHTTPD will use a default port number (usually 8080)
        try {
            Log.d(TAG, "starting server on port " + availablePort);
            server = new AssistHttpServer(this.getApplicationContext(), availablePort);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        server.stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void registerService(int port) throws IOException {
        Log.d(TAG, "registerService");

        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(ASSIST_MDNS_SERVICE_NAME);
        serviceInfo.setServiceType(ASSIST_MDNS_SERVICE_TYPE);
        serviceInfo.setPort(availablePort);

        mRegistrationListener = createRegistrationListener();

        mNsdManager = (NsdManager) getSystemService(NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void unregisterService() {
        Log.d(TAG, "unregisterService");
        mNsdManager.unregisterService(mRegistrationListener);
    }

    private NsdManager.RegistrationListener createRegistrationListener() {
        return new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service registered: " + serviceInfo);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Registration failed: " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Service unregistered: " + serviceInfo);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Unregistration failed: " + errorCode);
            }
        };
    }

    public static int getAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
