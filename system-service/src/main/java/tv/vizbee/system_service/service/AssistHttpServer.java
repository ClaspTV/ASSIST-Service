package tv.vizbee.system_service.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Slog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class AssistHttpServer extends NanoHTTPD {

    private static final String TAG = AssistHttpServer.class.getName();

    private final Context context;

    private Boolean isAppReadyForUse = true;

    public AssistHttpServer(Context applicationContext, int availablePort) {
        super(availablePort);

        context = applicationContext;
    }

    @Override
    public Response serve(IHTTPSession session) {

        if (session.getMethod() == Method.GET) {

            Slog.i(TAG, "Handle GET method");
            return handleGetRequest(session);

        } else if (session.getMethod() == Method.POST) {

            Slog.i(TAG, "Handle POST method");
            return handlePostRequest(session);
        }

        Slog.i(TAG, "Got a request for unsupported method " + session.getMethod());
        return getJsonResponse(
                Response.Status.METHOD_NOT_ALLOWED,
                "Requested method " + session.getMethod().name() + " not supported");
    }

    private Response handleGetRequest(IHTTPSession session) {

        String uri = session.getUri();
        Slog.v(TAG, "GET Request path " + uri);

        // Serve the app installation status
        if ("/appInstallationStatus".equals(uri)) {

            // get application package name
            Map<String, List<String>> params = session.getParameters();
            String appPackageName = "";
            List<String> nameValues = params.get("packageName");
            if (null != nameValues && !nameValues.isEmpty()) {
                appPackageName = nameValues.get(0);
            }

            // return not found when the package name is missing
            if (appPackageName.isEmpty()) {
                Slog.i(TAG, "Package name not found, returning NOT_FOUND");
                return getJsonResponse(Response.Status.NOT_FOUND,
                        "Missing Package Name");
            }

            Slog.v(TAG, "Checking app installation status for package " + appPackageName);

            // app installed
            if(isAppInstalledAndReadyForUse(appPackageName)) {
                return getJsonResponse(Response.Status.OK, "App Installed");
            }

            // app not installed
            return getJsonResponse(Response.Status.OK, "App Not Installed");
        }

        // default, return 404 path not found
        return getJsonResponse(Response.Status.NOT_FOUND, "Path Not Found");
    }

    private Response handlePostRequest(IHTTPSession session) {

        // return not found when application package name is missing
        String appPackageName = "";
        try {
            appPackageName = getAppPackageNameFromSession(session);
        } catch (ResponseException | IOException | JSONException e) {
            Slog.e(TAG, "Got an exception when reading the post request data " + e);
            return getJsonResponse(Response.Status.INTERNAL_ERROR,
                    "Internal Error");
        }

        if (appPackageName.isEmpty()) {
            return getJsonResponse(Response.Status.NOT_FOUND,
                    "Missing Package Name");
        }

        String uri = session.getUri();
        Slog.v(TAG, "POST Request path " + uri);

        if ("/launchPlayStore".equals(uri)) {

            // open PlayStore page for the specified package name
            // if the application is not installed
            if (!isAppInstalledAndReadyForUse(appPackageName)) {

                registerForActionPackageAdded(appPackageName);
                openAppStorePageForAnApp(appPackageName);

                // return 200, OK
                return getJsonResponse(Response.Status.OK, "Success");
            }

            return getJsonResponse(Response.Status.OK, "App Already Installed");
        } else if ("/launchApp".equals(uri)) {

            launchApp(appPackageName);

            return getJsonResponse(Response.Status.OK, "Success");
        }

        // default, return 404 path not found
        return getJsonResponse(Response.Status.NOT_FOUND, "Path Not Found");
    }

    private String getAppPackageNameFromSession(IHTTPSession session)
            throws ResponseException, IOException, JSONException {

        Map<String, String> body = new HashMap<>();
        String appPackageName = null;
        session.parseBody(body);
        Slog.i(TAG, "Post request body " + body);

        JSONObject jsonPayload;
        String postData = body.get("postData");
        if (null != postData) {

            // get JSON payload from request body
            jsonPayload = new JSONObject(postData);
            appPackageName = jsonPayload.getString("packageName");
        }

        return appPackageName;
    }

    private Response getJsonResponse(Response.Status statusCode, String message) {

        JSONObject jsonResponse = new JSONObject();
        try {
            jsonResponse.put("state", message);
        } catch (JSONException e) {
            Slog.e(TAG, "Error while adding a state", e);
        }

        return newFixedLengthResponse(
                statusCode,
                "application/json",
                jsonResponse.toString());
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
        the package name of the app. If it does, we can assume that the app has been fully
        installed and is ready to be used.
        */

        // use PackageManager to get a list of installed applications
        PackageManager packageManager = context.getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.
                getInstalledApplications(PackageManager.GET_META_DATA);

        // check if the app is installed
        boolean appInstalled = false;
        for (ApplicationInfo appInfo : installedApplications) {
            if (appInfo.packageName.equals(packageName)) {

                // The app is installed
                appInstalled = true;
                break;
            }
        }

        Slog.d(TAG, "App installation status - AppInstalled " +
                appInstalled + " AppReadyForUse " + isAppReadyForUse);

        return (appInstalled && isAppReadyForUse);
    }

    private void registerForActionPackageAdded(String packageName) {

        // register a BroadcastReceiver to listen for package installation events
        BroadcastReceiver packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String addedPackageName = intent.getData().getSchemeSpecificPart();
                if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {

                    // Package added
                    Slog.d(TAG, "Package added " + addedPackageName);
                    if (addedPackageName.equals(packageName)){
                        isAppReadyForUse = true;
                    }
                } else if (intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED)) {

                    // Package removed
                    Slog.d(TAG, "Package removed " + addedPackageName);
                    if (addedPackageName.equals(packageName)){
                        isAppReadyForUse = false;
                    }
                }
            }
        };

        // update the isAppReadyForUse flag
        Slog.d(TAG, "Setting isAppReadyForUse to false");
        isAppReadyForUse = false;

        // register the BroadcastReceiver
        IntentFilter packageFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addDataScheme("package");
        context.registerReceiver(packageReceiver, packageFilter);
    }

    private void openAppStorePageForAnApp(String packageName) {

        try {

            Slog.i(TAG, "Opening playstore page with market:// " +
                    "for the package " + packageName);
            Uri uri = Uri.parse("market://details?id=" + packageName);
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
            appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appStoreIntent);
        } catch (android.content.ActivityNotFoundException anfe) {

            Slog.i(TAG, "Opening playstore page with https:// " +
                    "for the package " + packageName);
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=" + packageName);
            Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, uri);
            appStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appStoreIntent);
        }
    }

    private void launchApp(String packageName) {

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent); // Launch the app
        }
    }
}

