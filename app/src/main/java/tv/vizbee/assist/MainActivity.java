package tv.vizbee.assist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate");
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
        getActionBar().hide();
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        Intent intent = new Intent(this, AssistMdnsService.class);
        startService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
//        Intent intent = new Intent(this, MDNSService.class);
//        stopService(intent);
    }
}
