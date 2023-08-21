package tv.vizbee.assist;

import static android.content.Context.NSD_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;

import java.io.IOException;
import java.net.ServerSocket;

import tv.vizbee.assist.utils.Logger;

public class AssistServiceManager {

    private static final String LOG_TAG = AssistServiceManager.class.getSimpleName();

    private static final String ASSIST_MDNS_SERVICE_TYPE = "_vzb-assist._tcp.";
    private static final String ASSIST_MDNS_SERVICE_NAME = "ASSIST Service";

    private AssistHttpServer mAssistHttpServer;

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    //--------
    // Service Register
    //--------

    public void registerService(Context context, String launchMode) throws IOException {
        Logger.v(LOG_TAG, "registerService");

        //---
        // Start http server
        //---

        // 1. get available port
        int availablePort = 0;
        availablePort = getAvailablePort();

        // 2. start the server
        Logger.i(LOG_TAG, "Starting AssistHttpServer on port " + availablePort);
        mAssistHttpServer = new AssistHttpServer(context, availablePort, launchMode);
        mAssistHttpServer.start();

        //---
        // Register NsdService
        //---

        // 1. create the NsdServiceInfo
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(getASSISTServiceName(context));
        serviceInfo.setServiceType(ASSIST_MDNS_SERVICE_TYPE);
        serviceInfo.setPort(availablePort);

        // 2. register the service
        mRegistrationListener = createRegistrationListener();
        mNsdManager = (NsdManager) context.getSystemService(NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void unregisterService() {

        Logger.v(LOG_TAG, "unregisterService");

        // 1. stop the server
        mAssistHttpServer. stop();

        // 2. un-register service
        mNsdManager.unregisterService(mRegistrationListener);
    }

    //--------
    // Service RegistrationListener
    //--------

    private NsdManager.RegistrationListener createRegistrationListener() {
        return new NsdManager.RegistrationListener() {
            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Logger.i(LOG_TAG, "ASSIST Service registered " + serviceInfo);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Logger.w(LOG_TAG, "ASSIST Service registration failed " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Logger.i(LOG_TAG, "ASSIST Service unregistered " + serviceInfo);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Logger.w(LOG_TAG, "ASSIST Service Unregistration failed " + errorCode);
            }
        };
    }

    //--------
    // Helper Methods
    //--------

    private int getAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @SuppressLint("MissingPermission")
    private String getASSISTServiceName(Context context) {

        String friendlyName = String.format("%s %s", Build.MANUFACTURER, Build.MODEL);
        Logger.i(LOG_TAG, "ASSIST Service name using MANUFACTURER and MODEL " + friendlyName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((null != context) && (context.checkSelfPermission(
                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)) {

                Logger.i(LOG_TAG, "ASSIST Service name using BLUETOOTH " + friendlyName);
                friendlyName = BluetoothAdapter.getDefaultAdapter().getName();
            }
        }
        return friendlyName + "'s " + ASSIST_MDNS_SERVICE_NAME;
    }
}
