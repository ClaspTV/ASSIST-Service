package tv.vizbee.androidtvinstallservice;

import android.app.Service;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class MDNSService extends Service {

    private static final String TAG = "MDNSService";

    private static final String SERVICE_TYPE = "_vizbee._tcp.";

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public MDNSService getService() {
            return MDNSService.this;
        }
    }

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
        Log.d(TAG, "onDestroy");

        // NanoHTTPD will use a default port number (usually 8080)
        HttpServer server = null;
        try {
            int port = getAvailablePort();
            Log.d(TAG, "starting server on port " + port);
            server = new HttpServer(this.getApplicationContext(), port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public void registerService(int port) throws IOException {
        Log.d(TAG, "registerService");

        ServerSocket serverSocket = new ServerSocket(port);
        int localPort = serverSocket.getLocalPort();
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("Vizbee Android TV Install Service");
        serviceInfo.setServiceType(SERVICE_TYPE);
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
        ServerSocket socket = new ServerSocket(0);
        try {
            return socket.getLocalPort();
        } finally {
            socket.close();
        }
    }
}
