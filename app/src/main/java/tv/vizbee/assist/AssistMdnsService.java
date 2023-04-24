package tv.vizbee.assist;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.ServerSocket;

public class AssistMdnsService extends Service {

    private static final String TAG = "AliasMdnsServ";

    private static final String ALIAS_MDNS_SERVICE_TYPE = "_vizbee-alias._tcp.";
    private static final String ALIAS_MDNS_SERVICE_NAME = "AndroidTV Launch Install Service";

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    private AssistHttpServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        try {
            registerService(5353);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        Toast.makeText(getApplicationContext(), "Started MDNS service", Toast.LENGTH_LONG).show();

        // NanoHTTPD will use a default port number (usually 8080)
        try {
            int port = getAvailablePort();
            Log.d(TAG, "starting server on port " + port);
            server = new AssistHttpServer(this.getApplicationContext(), port);
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

        ServerSocket serverSocket = new ServerSocket(port);
        int localPort = serverSocket.getLocalPort();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(ALIAS_MDNS_SERVICE_NAME);
        serviceInfo.setServiceType(ALIAS_MDNS_SERVICE_TYPE);
        serviceInfo.setPort(localPort);

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
