package tv.vizbee.assist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Toast.makeText(context, "Received Broadcast message", Toast.LENGTH_LONG).show();

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {

            Log.d(TAG, "onReceive ACTION_BOOT_COMPLETED");

            Toast.makeText(context, "Received Broadcast message with intent action ACTION_BOOT_COMPLETED", Toast.LENGTH_LONG).show();

            Log.d(TAG, "onReceive ACTION_BOOT_COMPLETED - Starting AssistMdns Service");
            Intent serviceIntent = new Intent(context, AssistMdnsService.class);
            context.startService(serviceIntent);
        }
    }
}
