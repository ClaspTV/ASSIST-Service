# AndroidTV Second Screen Install (ASSIST) Service

This repository is an open source implementation of the AndroidTV Second Screen Install (ASSIST) Service.

# Overview

Mobile-to-TV discovery, install and launch of streaming apps enables rich cross-device experiences to increase viewer acquisition, engagement and monetization on TVs. The streaming industry has created open & proprietary protocols to enable such mobile-to-TV interactions.  

**DIAL**

[DIAL](https://docs.google.com/viewer?a=v&pid=sites&srcid=ZGlhbC1tdWx0aXNjcmVlbi5vcmd8ZGlhbHxneDoyNzlmNzY3YWJlMmY1MjZl) is an open protocol that is implemented in some TV platforms. DIAL enables TV app discovery and launch from mobile apps but leaves the mobile-to-TV app install capability as a TV platform specific implementation detail to the TV platform developers.

**GOOGLECAST**

[GoogleCast](https://www.google.com/intl/en_us/chromecast/built-in/) is Google's proprietary protocol that is implemented on Android TVs. GoogleCast enables the TV app discovery and launch from mobile apps and does not have any support for mobile-to-TV app install.

**ASSIST**

ASSIST is an open protocol for mobile-to-TV app install on Android TVs. It **assists** existing open protocols like DIAL or proprietary protocols like GoogleCast in an interoperable manner. The key feature of ASSIST is that it enables a secure and privacy-friendly way to initiate Android TV app install from mobile devices as part of casting flows when using protocols like DIAL or GoogleCast. ASSIST significantly increases the mobile-to-TV user interactions by removing the biggest hurdle, i.e., automating Android TV app install, during initial casting from a mobile app.

# ASSIST Specification

## ASSIST MDNS Discovery

ASSIST service can be discovered by 2nd screen devices by using the MDNS target `_vzb-assist._tcp.`.

## ASSIST REST APIs

Once discovered, the ASSIST service shares an HTTPS end-point which supports the following REST APIs.

| Method Type | Method Name | Method Parameters/Body                       | Response Code | Response Body | Notes|
| :---        | :---   |:---------------------------------------------| :---   | :---   | :--- |
| GET   | appInstallationStatus | /packageName=androidtv_app_package_name      | 200 OK        | ```{state: "App Installed"}``` or ```{state: "App Not Installed"}``` | Success scenario. |
|       |                       |                                              | 404 Not Found | N/A | Path not found or missing packageName parameter in the URL. |
|       | |                                              | 500 Internal Service Error | N/A | Server execution error. |
| POST  | launchAppStore        | {"packageName":"androidtv_app_package_name"} | 200 OK | | Success scenario. |
|       |         |                                              | 404 Not Found | N/A | Path not found or missing packageName parameter in the body |
|       | |                                              | 500 Internal Service Error | N/A | Server execution error. |

<method type> <method name> <response code> <response body>

# ASSIST Development & Deployment

## Overview

The repository provides the core ASSIST Service and enables it to be built as a (1) System Service and/or a (2) Demo App. The System Service can be easily incorporated into existing Android OS builds (AOSP) on your streaming TV platform. The Demo App enables simple demonstration of the ASSIST feature and also aids in the protocol development with a simpler test cycle.

## Demo App

The Demo App has been designed for rapid testing of ASSIST service updates without having to build and deploy a System Service. In this mode, 

* ASSIST service runs as part of the demo app with a transparent UI. 
* The demo app must be run first for the ASSIST service to be discoverable and act on mobile commands.

Here is a video showing the ASSIST Service demo app running on Verizon Stream TV.

[Verizon Demo Video](https://vimeo.com/824170547/1615c77e31)

### Build and Run

* Set the target to `demo-app` in Android Studio
* Select the Android TV device or simulator
* Hit the run `Run demo-app` button

## System Service

The System Service can be incorporated, built and immediately used in custom Android TV platforms.

### AOSP Code Changes

You can see a version of the AOSP code changes with the ASSIST Service here: <TODO-XYZ-with-assist>

**Step 1:** Add `nanohttpd` library 

  Copy `nanohttpd` folder from `system-service.nanohttpd` module of ASSIST project to your AOSP File location `prebuilts/mic/common/`

**Step 2:** Add Service files  

  Copy the following classes from `system-service.service` module of ASSIST project to your AOSP File location:  `frameworks/base/services/core/java/com/android/server`
  `AssistHttpServer.java`  
  `AssistService.java`  
  `AssistServiceManager.java`  

**Step 3:** Modify the Android.bp file to use nanohttpd library
  
File: `frameworks/base/services/core/Android.bp` 
```
  //Android.bp 
  //Add nanohttpd 
  java_library_static {
    name: "services.core.unboosted",
    defaults: ["platform_service_defaults"],
    static_libs: [   
      “nanohttpd”
    ],
  }

  ```

**Step 4:** Modify the AndroidManifest for adding AssistService and NanoHTTPD 
  
File: `frameworks/base/core/res` 
```
  //AndroidManifest 
  //Add nanohttpd 
  <uses-library android:name = "fi.iki.elonen.NanoHTTPD" />
  <service android:name="com.android.server.AssistService"
    android:exported="false" />

  ```

**Step 5:** Modify the following files as specified below to register the AssistService as as System Service  
  
File: `frameworks/base/core/java/android/content/Context.java` 
```
  // Add ASSIST_SEVICE
  
  @StringDef(
    suffix = {
      "_SERVICE"
    }, 
    value = {
      ASSIST_SERVICE,
      ACCOUNT_SERVICE,
      ACTIVITY_SERVICE,
      ALARM_SERVICE,
      NOTIFICATION_SERVICE,
      ACCESSIBILITY_SERVICE,
      CAPTIONING_SERVICE,
    }
  )
  
  public static final String ASSIST_SERVICE = "assist"; 

  ```
  
File: `frameworks/base/services/java/com/android/server/SystemServer.java`
```
  private static final String ASSIST_SERVICE = "com.android.server.AssistService";

  //add the following to the `startBootstrapServices()` function
  
  try {
      t.traceBegin("AssistService");
      mSystemServiceManager.startService(AssistService.class);
      } catch (Throwable e) {
        Slog.e(TAG, "Starting AssistService failed!!! ", e);
    }
  t.traceEnd();
        
```

File: `frameworks/base/core/api/current.txt`
  ```
    public abstract class Context {
      field public static final String ASSIST_SERVICE = "assist";
    }
  ```

**Step 6:** Build the AOSP and run it.

**Step 7:** Verify the System Service is running using Logs.

```
  adb logcat -v color AssistServiceManager:V '*:S'

```
# Testing

You can test that your Android TV has the correct implementation of the ASSIST service using the following tests.

## Discovery Test

|Test| Discovery Test|
|---|---|
|Setup| Ensure your laptop or mobile phone and the Android TV are on the same Wifi network.|
|Steps| From laptop, run a CLI command to discover bonjour services of type _vzb-assist._tcp. For example, on macOS, run `dns-sd -B _vzb-assist._tcp.` If testing from a mobile phone, you can use any bonjour discovery app such as https://apps.apple.com/de/app/discovery-dns-sd-browser/id305441017|
|Result| Confirm that you are able to discover the Android TV|

## Mobile-to-TV Interaction Test

1. Run the following command on macOS to list available instances of the service `dns-sd -B _vzb-assist._tcp.`.
2. Replace {Instance_Name} with the actual instance name you want to investigate and run this command `dns-sd -L "{Instance_Name}" _vzb-assist._tcp local.` to get the `hostName:portNumber` information. 
3. Finally, run the ping command using the hostname from the previous step to retrieve the IP address associated with the service `ping hostName`.
  
### App Status Test

|Test| App Status Test|
|---|---|
|Setup| Ensure you are able to discover Android TV|
|Steps| * Execute the CURL command from your laptop to get the status of the app.<br>  `curl ‘{Android_TV_IP}:{Port}/appInstallationStatus?packageName={App_Package}’.`<br> * Example: `curl ‘192.168.1.136:32819/appInstallationStatus?packageName=com.fng.foxnation’`<br>|
|Result| Confirm that you get the status of the app.<br> Example: `{"state":"App Not Installed"}`|

### Launch PlayStore Test

|Test| Launch PlayStore Test|
|---|---|
|Setup| Ensure you are able to discover Android TV|
|Steps| * Execute the CURL command from your laptop to open the Google Play Store page for a specific app.<br>  `curl -X POST -H "Content-Type: application/json" -d '{"packageName":{App_Package}}' {Android_TV_IP}:{Port}/launchPlayStore’.`<br> * Example: `curl -X POST -H "Content-Type: application/json" -d '{"packageName":"com.fng.foxnation"}' 192.168.1.136:40661/launchPlayStore`<br>|
|Result| Confirm that the PlayStore page is launched for the specified app.<br> Example: `{"state":"Success"}`|
