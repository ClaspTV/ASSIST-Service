package com.android.server;

import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.AssistServiceManager;
import java.io.IOException;


public class AssistService extends SystemService {
    private final static String LOG_TAG = AssistService.class.getSimpleName();
    private AssistServiceManager mAssistServiceManager;

    private final Context mContext;

    public AssistService(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        Slog.d(LOG_TAG, "Assist Service OnStart method Called");
        mAssistServiceManager = new AssistServiceManager();
        try {
            mAssistServiceManager.registerService(mContext);
        } catch (IOException e) {
            Slog.e(LOG_TAG, "Failed to register service %s", e);
        }
    }

    @Override
    public void onBootPhase(int phase) {
        Slog.d(LOG_TAG, "onBootPhase called");
        if (phase == PHASE_ACTIVITY_MANAGER_READY) {
            Slog.d(LOG_TAG, "PHASE_ACTIVITY_MANAGER_READY");
        }
    }

}