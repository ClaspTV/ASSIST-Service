package tv.vizbee.assist;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class AssistHttpServer extends NanoHTTPD {

    private static final String TAG = "AssistHttpServer";

    private Context context;

    private Boolean isAppReadyForUse = true;

    public AssistHttpServer(Context applicationContext, int availablePort) {
        super(availablePort); // TODO: change it available port
        Toast.makeText(applicationContext, "Started AssistHttpServer on port" + availablePort, Toast.LENGTH_LONG).show();
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
            if(!isAppInstalledAndReadyForUse(appPackageName)) {
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
                if (!isAppInstalledAndReadyForUse(appPackageName)) {

                    registerForActionPackageAdded(appPackageName);
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

    private boolean isAppInstalledAndReadyForUse(String packageName) {

        /*
        NOTE: The installation process of an Android app involves 2 stages
        1. Downloading the package
        2. Installing the package
        As soon as the package is downloaded, the package name will be added to the PackageManager
        and then installation process will initiate the installation. So, we can't solely depend
        on PackageManger to confirm that the is fully installed and ready to be used.

        To determine if the app is fully installed and ready to be used,
        we can register a BroadcastReceiver to listen for the ACTION_PACKAGE_ADDED intent,
        which is broadcasted by the system when a new package is added.
        When the ACTION_PACKAGE_ADDED intent is received, we can check if the added package matches
        the package name of the app. If it does, we can assume that the app has been fully installed and is ready to be used.
        */

        // Use PackageManager to get a list of installed applications
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_SERVICES);

        // Check if the app is installed
        boolean appInstalled = false;
        for (ApplicationInfo appInfo : installedApplications) {
            if (appInfo.packageName.equals(packageName)) {

                // The app is installed
                appInstalled = true;
                break;
            }
        }

        Log.d(TAG, "App is installed: " + packageName);
        return (appInstalled && isAppReadyForUse);
    }

    private void registerForActionPackageAdded(String packageName) throws IOException {

        // Register a BroadcastReceiver to listen for package installation events
        BroadcastReceiver packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String addedPackageName = intent.getData().getSchemeSpecificPart();
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {

                    // Package added
                    Log.d(TAG, "Package added: " + addedPackageName);
                    if (addedPackageName.equals(packageName)){
                        isAppReadyForUse = true;
                    }
                } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {

                    // Package removed
                    Log.d(TAG, "Package removed: " + addedPackageName);
                    if (addedPackageName.equals(packageName)){
                        isAppReadyForUse = false;
                    }
                }
            }
        };

        // Register the BroadcastReceiver

        isAppReadyForUse = false;

        IntentFilter packageFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addDataScheme(packageName);
        context.registerReceiver(packageReceiver, packageFilter);

        // Create a PackageInstaller instance
//        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();

// Create a session parameters object
//        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
//                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

// Set the package name of the app you want to install
//        params.setAppPackageName(packageName);

// Register a session callback to monitor the installation progress
//        packageInstaller.registerSessionCallback(new PackageInstaller.SessionCallback() {
//            @Override
//            public void onCreated(int sessionId) {
//                // Session created
//            }
//
//            @Override
//            public void onBadgingChanged(int sessionId) {
//                // Badging changed
//            }
//
//            @Override
//            public void onActiveChanged(int sessionId, boolean active) {
//                // Active changed
//            }
//
//            @Override
//            public void onProgressChanged(int sessionId, float progress) {
//                // Progress changed
//            }
//
//            @Override
//            public void onFinished(int sessionId, boolean success) {
//                // Installation finished
//                if (success) {
//                    // The app is installed and ready to use
//                    Log.d(TAG, "App installed successfully");
//                    isAppReadyForUse = true;
//                } else {
//                    // The installation failed
//                    Log.d(TAG, "App installation failed");
//                }
//            }
//        });

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

