package tv.vizbee.assist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import tv.vizbee.assist.utils.Logger;

public class AssistHttpServer extends NanoHTTPD {

    private static final String TAG = "AssistHttpServer";

    private final Context context;

    private Boolean isAppReadyForUse = true; // TODO: Need to optmize

    public AssistHttpServer(Context applicationContext, int availablePort) {
        super(availablePort);
        Toast.makeText(applicationContext, "Started AssistHttpServer on port" + availablePort, Toast.LENGTH_LONG).show();
        context = applicationContext;
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (session.getMethod() == Method.GET) {

            Logger.i(TAG, "Handle GET method");
            return handleGetRequest(session);

        } else if (session.getMethod() == Method.POST) {

            Logger.i(TAG, "Handle POST method");
            return handlePostRequest(session);
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
    }

    private Response handleGetRequest(IHTTPSession session) {

        String uri = session.getUri();
        Logger.i(TAG, "path " + uri);
        Map<String, String> params = session.getParms();
        Logger.i(TAG, "GET params" + params);

        if ("/info".equals(uri)) {

            // Serve the service info

            // different get methods and query params that this server serves
            return newFixedLengthResponse(""); // TODO:
        } else if ("/appStatus".equals(uri)) {

            final String appPackageName = session.getParms().get("packageName");

            // Serve the app installation status

            // app not installed
            if(!isAppInstalledAndReadyForUse(appPackageName)) {
                return newFixedLengthResponse("AppNotInstalled");
            }

            // app installed
            return newFixedLengthResponse("AppInstalled");
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
            Logger.i(TAG, "Received post request with body " + body);

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
            Logger.e(TAG, "Error parsing request body", e);
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

        // use PackageManager to get a list of installed applications
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_SERVICES);

        // check if the app is installed
        boolean appInstalled = false;
        for (ApplicationInfo appInfo : installedApplications) {
            if (appInfo.packageName.equals(packageName)) {

                // The app is installed
                appInstalled = true;
                break;
            }
        }

        Logger.d(TAG, "AppInstalled " + appInstalled + " isAppReadyForUse " + isAppReadyForUse);
        return (appInstalled && isAppReadyForUse);
    }

    private void registerForActionPackageAdded(String packageName) throws IOException {

        // register a BroadcastReceiver to listen for package installation events
        BroadcastReceiver packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String addedPackageName = intent.getData().getSchemeSpecificPart();
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {

                    // Package added
                    Logger.d(TAG, "Package added: " + addedPackageName);
                    if (addedPackageName.equals(packageName)){
                        isAppReadyForUse = true;
                    }
                } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {

                    // Package removed
                    Logger.d(TAG, "Package removed: " + addedPackageName);
                    if (addedPackageName.equals(packageName)){
                        isAppReadyForUse = false;
                    }
                }
            }
        };

        // update the isAppReadyForUse flag
        Logger.d(TAG, "Setting isAppReadyForUse to false");
        isAppReadyForUse = false;

        // register the BroadcastReceiver
        IntentFilter packageFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addDataScheme("package");
        context.registerReceiver(packageReceiver, packageFilter);
    }

    private void openAppStorePageForAnApp(String packageName) {

       try {

           Logger.i(TAG, "Opening playstore page with market:// for the package " + packageName);
           Uri uri = Uri.parse("market://details?id=" + packageName);
           Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
           appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
           context.startActivity(appStoreIntent);
       } catch (android.content.ActivityNotFoundException anfe) {

           Logger.i(TAG, "Opening playstore page with https:// for the package " + packageName);
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
            appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appStoreIntent);
       }
    }

    private void launchApp(String packageName) {

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            context.startActivity(launchIntent); // Launch the app
        }
    }
}

