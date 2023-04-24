package tv.vizbee.assist;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class AssistHttpServer extends NanoHTTPD {

    private static final String TAG = "AliasHttpServer";

    private Context context;

    public AssistHttpServer(Context applicationContext, int availablePort) {
        super(availablePort); // TODO: change it available port

        context = applicationContext;
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (session.getMethod() == Method.GET) {

            Log.i(TAG, "Handle GET method");
            return handleGetRequest(session);

        } else if (session.getMethod() == Method.POST) {

            Log.i(TAG, "Handle POST method");
            return handlePostRequest(session);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
    }

    private Response handleGetRequest(IHTTPSession session) {

        String uri = session.getUri();
        Log.i(TAG, "path " + uri);
        Map<String, String> params = session.getParms();
        Log.i(TAG, "GET params" + params);

        if ("/info".equals(uri)) {

            // Serve the service info
            // 1. url and port where it can be reachable
            // 2. different get methods and query params that this server serves
            return newFixedLengthResponse(""); // TODO:
        } else if ("/appStatus".equals(uri)) {

            final String appPackageName = session.getParms().get("packageName");

            // Serve the app status
            // 1. AppNotInstalled
            if(!isAppInstalled(appPackageName)) {
                return newFixedLengthResponse("AppNotInstalled");
            }
            // 2. AppNotRunning
            if(!isAppRunning(appPackageName)) {
                return newFixedLengthResponse("AppNotRunning");
            }
            // 3. AppRunningInForeGround
            if(!isAppInForeground(appPackageName)) {
                return newFixedLengthResponse("AppRunningInForeGround");
            }
            // 4. AppRunningInBackGround
            return newFixedLengthResponse("AppRunningInBackGround");
        } else {
            // Serve a 404 response for unknown paths
            return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "404 Not Found");
        }
    }

    private Response handlePostRequest(IHTTPSession session) {

        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String requestBody = session.getQueryParameterString();
            Log.i(TAG, "Received post request with body " + body);

            String uri = session.getUri();
            JSONObject jsonPayload = null;
            String appPackageName = null;
            if (null != body.get("postData")) {

                // get JSON payload from request body
                try {
                    jsonPayload = new JSONObject(body.get("postData"));
                    appPackageName = jsonPayload.getString("packageName");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if ("/launchPlayStore".equals(uri)) {

                // open PlayStore page for the specified package name only
                // if the application is not installed
                if (!isAppInstalled(appPackageName)) {
                    openAppStorePageForAnApp(appPackageName);
                }
                return newFixedLengthResponse("Success");
            } else if ("/launchApp".equals(uri)) {
                launchApp(appPackageName);
            }
            return newFixedLengthResponse("Received POST request with body: " + requestBody);
        } catch (IOException | ResponseException e) {
            Log.e(TAG, "Error parsing request body", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error parsing request body");
        }
    }

    private boolean isAppInstalled(String packageName) {

        /*
         If the package is installed, the getPackageInfo() method will return a PackageInfo object,
         and the `try` block will continue to execute. If the package is not installed,
         the getPackageInfo() method will throw a NameNotFoundException, and the code inside the catch block will execute.
         Note that the GET_ACTIVITIES flag passed to getPackageInfo() is used to retrieve information
         about the activities defined in the package. We can use other flags, such as GET_SERVICES,
         GET_RECEIVERS, and GET_PROVIDERS, to retrieve information about other components defined in the package.
         NOTE: https://developer.android.com/training/package-visibility
         */
//        PackageManager pm = context.getPackageManager();
//        try {
//            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
//            Log.i(TAG, "App " + packageName + " installed");
//           return true;
//        } catch (PackageManager.NameNotFoundException e) {
//
//            // the package is not installed
//            Log.i(TAG, "App " + packageName + " not installed");
//            return false;
//        }

        PackageManager pm = context.getPackageManager();
        List<PackageInfo> packages = pm.getInstalledPackages(0);

        for (PackageInfo packageInfo : packages) {
            Log.d(TAG, "Package name: " + packageInfo.packageName);
            if (packageInfo.packageName.equals(packageName)) {
                Log.i(TAG, "App " + packageName + " installed");
                return true;
            }
        }

        Log.i(TAG, "App " + packageName + " not installed");
        return false;
    }

    private void openAppStorePageForAnApp(String packageName) {

       try {

           Log.i(TAG, "Opening playstore page with market:// for the package " + packageName);
           Uri uri = Uri.parse("market://details?id=" + packageName);
           Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
           appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           context.startActivity(appStoreIntent);
       } catch (android.content.ActivityNotFoundException anfe) {

            Log.i(TAG, "Opening playstore page with https:// for the package " + packageName);
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
            appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appStoreIntent);
       }
    }

    private void sendInstallConfirmationKeyEvent() {
        InputManager mInputManager = (InputManager) context.getSystemService(Context.INPUT_SERVICE);
        long now = SystemClock.uptimeMillis();
        KeyEvent downEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0);
        KeyEvent upEvent = new KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0);
    }

    private void launchApp(String packageName) {

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            context.startActivity(launchIntent); // Launch the app
        }
    }

    /*
    The getRunningAppProcesses() method returns a list of RunningAppProcessInfo objects,
    each of which represents a currently running process on the device.
    Iterate over this list and check the processName field of each
    RunningAppProcessInfo object to determine whether the app is running or not.

    NOTE: Accessing information about other apps' tasks and processes without requiring
    the user's permission is not possible on Android for security and privacy reasons.
    Android's permission model is designed to protect user data and ensure
    that users have control over which apps can access their data.
     */
    // TODO: This is not working due to Android restrictions, find any alternative or hacky way.
    private boolean isAppRunning(String packageName) {

        boolean isAppRunning = false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (processInfo.processName.equals(packageName)) {

                // The app is running
                Log.i(TAG, "App " + packageName + " running");
                isAppRunning = true;
                break;
            }
        }
        Log.i(TAG, "App " + packageName + " not running");
        return isAppRunning;
    }

    /*
    The `importance` field of a RunningAppProcessInfo object represents the importance of the process
    relative to other processes on the device. If the importance is IMPORTANCE_FOREGROUND,
    it means that the app is currently running in the foreground
    (i.e., the user is interacting with the app or the app is displaying something on the screen).
    If the importance is any other value, it means that the app is running in the background
    (i.e., the app is not visible to the user or not currently being used).
     */
    // TODO: This is not working due to Android restrictions, find any alternative or hacky way.
    private boolean isAppInForeground(String packageName) {

        // app is running in the background
        boolean isAppInFG = false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (processInfo.processName.equals(packageName)) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {

                    // The app is running in the foreground
                    isAppInFG = true;
                }
                break;
            }
        }
        return isAppInFG;
    }
}

