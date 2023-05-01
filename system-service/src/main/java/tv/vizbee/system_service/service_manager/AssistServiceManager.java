package android.app;

import android.os.RemoteException;
import android.annotation.SystemService;
import android.content.Context;

@SystemService(Context.ASSIST_SERVICE)
public final class AssistServiceManager {
    private final IAssistServiceManager mService;
    private Context mContext;

    /**
     * @hide to prevent subclassing from outside of the framework
     */
    AssistServiceManager(Context context, IAssistServiceManager service) {
        mContext = context;
        mService = service;
    }

    public void printAssistLog() {
        try {
            mService.printAssistLog();
        } catch (RemoteException ex) {
        }
    }
}
