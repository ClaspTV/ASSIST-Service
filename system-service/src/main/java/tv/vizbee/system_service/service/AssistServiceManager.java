package tv.vizbee.system_service.service;

import static android.content.Context.NSD_SERVICE;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.io.IOException;
import java.net.ServerSocket;

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
        mAssistHttpServer.start();

        //---
        // Register NsdService
        //---

        // 1. create the NsdServiceInfo
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(ASSIST_MDNS_SERVICE_NAME);
        serviceInfo.setServiceType(ASSIST_MDNS_SERVICE_TYPE);
        serviceInfo.setPort(availablePort);

        // 2. register the service
        mRegistrationListener = createRegistrationListener();
        mNsdManager = (NsdManager) context.getSystemService(NSD_SERVICE);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void unregisterService() {

        Slog.v(LOG_TAG, "unregisterService");

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
}
