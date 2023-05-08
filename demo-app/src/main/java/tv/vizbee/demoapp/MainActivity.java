package tv.vizbee.demoapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;

import tv.vizbee.assist.utils.Logger;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Logger.setLogLevel(Logger.TYPE.VERBOSE);

        Logger.v(TAG, "onCreate");

        // remove the action bar as the activity is a Translucent activity
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().hide();
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // get extra string and pass it on
        Intent mainIntent = getIntent();
        String launchMode = null;
        if (null != mainIntent) {
            launchMode = mainIntent.getStringExtra("launch_mode");
        }

        // start a foreground service
        Intent intent = new Intent(this, AssistMdnsService.class);
        if (null != launchMode) {
            intent.putExtra("launch_mode", launchMode);
        }
        startForegroundService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();

        Logger.d(TAG, "onStop");
    }
}
