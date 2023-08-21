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
        Slog.d(LOG_TAG, "Assist service onStart method called");
    }

    @Override
    public void onBootPhase(int phase) {
        Slog.d(LOG_TAG, "Assist service onBootPhase called");
        if (phase == PHASE_BOOT_COMPLETED) {
            Slog.d(LOG_TAG, "Assist service PHASE_BOOT_COMPLETED");
            mAssistServiceManager = new AssistServiceManager();
            try {
                mAssistServiceManager.registerService(mContext);
                Slog.d(LOG_TAG, "Successfully registered Assist service");
            } catch (IOException e) {
                Slog.e(LOG_TAG, "Failed to register Assist service %s", e);
            }
        }
    }

}