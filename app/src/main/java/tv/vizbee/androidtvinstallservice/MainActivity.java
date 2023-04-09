package tv.vizbee.androidtvinstallservice;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.v(TAG, "onCreate");

        Intent intent = new Intent(this, MDNSService.class);
        startService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "onStop");
        Intent intent = new Intent(this, MDNSService.class);
        stopService(intent);
    }
}
