package com.android.server;

import static android.content.Context.NSD_SERVICE;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.io.IOException;
import java.net.ServerSocket;
import android.content.pm.PackageManager;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.Manifest;

import android.util.Slog;

public class AssistServiceManager {

    private static final String LOG_TAG = AssistServiceManager.class.getSimpleName();

    private static final String ASSIST_MDNS_SERVICE_TYPE = "_vzb-assist._tcp.";
    private static final String ASSIST_MDNS_SERVICE_NAME = "Android TV Second Screen Install Service";

    private AssistHttpServer mAssistHttpServer;

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    //--------
    // Service Register
    //--------

    public void registerService(Context context) throws IOException {
        Slog.v(LOG_TAG, "registerService");

        //---
        // Start http server
        //---

        // 1. get available port
        int availablePort = 0;
        availablePort = getAvailablePort();

        // 2. start the server
        Slog.i(LOG_TAG, "Starting AssistHttpServer on port " + availablePort);
        mAssistHttpServer = new AssistHttpServer(context, availablePort);

        if (mAssistHttpServer == null) {
            Slog.e(LOG_TAG, "mAssistHttpServer is Null");
        }

        try {
            mAssistHttpServer.start();
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Failed to start AssistHttpServer " + e.getMessage());
        }

        boolean isServerRunning = mAssistHttpServer.isAlive();
        Slog.e(LOG_TAG, "isAssistHttpServerRunning  = " + isServerRunning);

        //---
        // Register NsdService
        //---

        // 1. create the NsdServiceInfo
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(getASSISTServiceName(context));
        serviceInfo.setServiceType(ASSIST_MDNS_SERVICE_TYPE);
        serviceInfo.setPort(availablePort);
        Slog.i(LOG_TAG, "availablePort = " + availablePort);

        // 2. register the service
        mRegistrationListener = createRegistrationListener();
        if (context == null) {
            Slog.e(LOG_TAG, "context is Null");
        }
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        if (mNsdManager == null) {
            Slog.e(LOG_TAG, "NSDManager is Null");
        }
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void unregisterService() {

        Slog.v(LOG_TAG, "unregisterService");

        // 1. stop the server
        mAssistHttpServer.stop();

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
                Slog.i(LOG_TAG, "ASSIST Service registered " + serviceInfo);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Slog.w(LOG_TAG, "ASSIST Service registration failed " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Slog.i(LOG_TAG, "ASSIST Service unregistered " + serviceInfo);
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Slog.w(LOG_TAG, "ASSIST Service Unregistration failed " + errorCode);
            }
        };
    }

    public static int getAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private String getASSISTServiceName(Context context) {

        String friendlyName = String.format("%s %s", Build.MANUFACTURER, Build.MODEL);
        Slog.i(LOG_TAG, "ASSIST Service name using MANUFACTURER and MODEL " + friendlyName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((null != context) && (context.checkSelfPermission(
                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED)) {

                Slog.i(LOG_TAG, "ASSIST Service name using BLUETOOTH " + friendlyName);
                friendlyName = BluetoothAdapter.getDefaultAdapter().getName();
            }
        }
        return friendlyName + "'s " + ASSIST_MDNS_SERVICE_NAME;
    }
}