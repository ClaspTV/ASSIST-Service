package tv.vizbee.androidtvinstallservice;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;

public class HttpServer extends NanoHTTPD {

    private static final String TAG = "HttpServer";

    private Context context = null;

    public HttpServer(Context applicationContext, int availablePort) {
        super(availablePort);

        context = applicationContext;
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (session.getMethod() == Method.GET) {

            Log.i(TAG, "Handle GET method");

            // Handle GET request
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
                // 3. AppInForeGround
                if(!isAppInForeground(appPackageName)) {
                    return newFixedLengthResponse("AppInForeGround");
                }
                // 4. AppInForeGround
                return newFixedLengthResponse("AppInBackGround");
            } else {
                // Serve a 404 response for unknown paths
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "404 Not Found");
            }

//
//            // open PlayStore page for the specified package name only
//            // if the application is not installed
//            if (!isAppInstalled(appPackageName)) {
//                openAppStorePageForAnApp(appPackageName);
//            }
//
//            return newFixedLengthResponse("Received GET request");
        } else if (session.getMethod() == Method.POST) {
            // Handle POST request
//            try {
//                session.parseBody();
//                String requestBody = session.getQueryParameterString();
//                // ...
//                return newFixedLengthResponse("Received POST request with body: " + requestBody);
//            } catch (IOException | ResponseException e) {
//                Log.e(TAG, "Error parsing request body", e);
//                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error parsing request body");
//            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
    }

    private boolean isAppInstalled(String packageName) {

        /*
         If the package is installed, the getPackageInfo() method will return a PackageInfo object,
         and the the `try` block will continue to execute. If the package is not installed,
         the getPackageInfo() method will throw a NameNotFoundException, and the code inside the catch block will execute.
         Note that the GET_ACTIVITIES flag passed to getPackageInfo() is used to retrieve information
         about the activities defined in the package. We can use other flags, such as GET_SERVICES,
         GET_RECEIVERS, and GET_PROVIDERS, to retrieve information about other components defined in the package.
         */
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            Log.i(TAG, "AppInstalled for the package " + packageName);
           return true;
        } catch (PackageManager.NameNotFoundException e) {

            // the package is not installed
            Log.i(TAG, "App not installed for the package " + packageName);
            return false;
        }
    }

    private void openAppStorePageForAnApp(String packageName) {
        try {
            Uri uri = Uri.parse("market://details?id=" + packageName);
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
            appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appStoreIntent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
            appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appStoreIntent);
        }
    }

    /*
    The getRunningAppProcesses() method returns a list of RunningAppProcessInfo objects,
    each of which represents a currently running process on the device.
    Iterate over this list and check the processName field of each
    RunningAppProcessInfo object to determine whether the app is running or not.
     */
    private boolean isAppRunning(String packageName) {

        boolean isAppRunning = false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
            if (processInfo.processName.equals(packageName)) {

                // The app is running
                isAppRunning = true;
                break;
            }
        }
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

