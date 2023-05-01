package tv.vizbee.system_service.service;

import android.content.Context;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.SystemService;

public class AssistService extends IAssistServiceManager.Stub {
    private final static String LOG_TAG = "AssistService";
 
    private final Object mLock = new Object();
 
    private final Context mContext;
    
    AssistService(Context context) {
        mContext = context;
    }   
    
    @Override
    public void printAnAssist() throws RemoteException {
        Slog.i(LOG_TAG,"Assist Running!");
    }   
}
