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

# ASSIST Development, Testing & Deployment

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
**VERY IMPORTANT**
1. Add the following to the `startOtherServices()` function.
2. Add the AssistService at the end (after all the existing services) in `startOtherServices()` function.

```
  
  try {
      t.traceBegin("AssistService");
      mSystemServiceManager.startService(AssistService.class);
      } catch (Throwable e) {
        Slog.e(TAG, "Starting AssistService failed", e);
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

You can test that your Android TV has the correct implementation of the ASSIST service with very simple CLI commands.

## ASSIST Service Discovery Test

* **Setup** - Ensure your laptop or mobile phone and the Android TV are on the same Wifi network.
* **Steps**
  * If testing from a laptop, run a CLI command to discover bonjour services of type _vzb-assist._tcp. For example, on macOS, run `dns-sd -B _vzb-assist._tcp.` You should see an output like below.
 
```
Browsing for _vzb-assist._tcp.
DATE: ---Fri 18 Aug 2023---
18:56:56.846  ...STARTING...
Timestamp     A/R    Flags  if Domain               Service Type         Instance Name
18:56:56.847  Add        2  14 local.               _vzb-assist._tcp.    Android TV Second Screen Install Service
```

  * If testing from a mobile phone, you can use any bonjour discovery app such as https://apps.apple.com/de/app/discovery-dns-sd-browser/id305441017
* **Result** - Confirm that you are able to discover the ASSIST Service on the Android TV.

## ASSIST Service Information Test

* **Setup** - Ensure your laptop and the Android TV are on the same Wifi network.
* **Steps**
  *  Run the following command on macOS to list available instances of the ASSIST service on your wifi.

```
dns-sd -B _vzb-assist._tcp.
```

  *  Next, run this command `dns-sd -L $ASSIST_INSTANCE_NAME _vzb-assist._tcp local.` to get the `hostName:portNumber` information of the instance by replacing `$ASSIST_INSTANCE_NAME` with the actual instance name you want to investigate from the list of instances in the previous command. You should see an output like below.

```
Lookup Android TV Second Screen Install Service._vzb-assist._tcp.local.
DATE: ---Fri 18 Aug 2023---
18:57:13.259  ...STARTING...
18:57:13.446  Android\032TV\032Second\032Screen\032Install\032Service._vzb-assist._tcp.local. can be reached at Android-2.local.:32789 (interface 14)
```
  
  *  In the above example, the `hostName:portNumber` is `Android-2.local.:32789`.
  *  Finally, run the command `dns-sd -G v4 $HOST_NAME` where `$HOST_NAME` is the hostName found in the previous command to get the IPv4 address of the AndroidTV where the ASSIST service is running. You should see an output like below.

```
DATE: ---Fri 18 Aug 2023---
20:03:38.527  ...STARTING...
Timestamp     A/R    Flags if Hostname                               Address                                      TTL
20:03:38.528  Add 40000002 14 Android-2.local.                       192.168.1.116                                120
```

  *  In the above example, the Android TV IP address is `192.168.1.116` and the ASSIST service port is `32789`.
  
## App Install Status Test

* **Setup**
  * Ensure your laptop and the Android TV are on the same Wifi network.
  * To execute this test, you need the `$ANDROID_TV_IP` and the `$ASSIST_SERVICE_PORT` from the previous ASSIST service information test.
  * To execute this test, you'll also need the `$APP_PACKAGE_NAME` of an Android app such as `com.fng.foxnation` for the Fox Nation app.
* **Steps**
  *  Execute the following CURL command from your laptop to get the status of the app.

```
curl http://$ANDROID_TV_IP:$ASSIST_SERVICE_PORT/appInstallationStatus?packageName=$APP_PACKAGE_NAME
```

  *  Example: `curl http://192.168.1.136:32819/appInstallationStatus?packageName=com.fng.foxnation`. You should see an output like below.
     
```
{"state":"App Not Installed"}
```

* **Result** -  Confirm that you get the correct status of the app.

## Launch PlayStore for App Test

* **Setup**
  * Ensure your laptop and the Android TV are on the same Wifi network.
  * To execute this test, you need the `$ANDROID_TV_IP` and the `$ASSIST_SERVICE_PORT` from the previous ASSIST service information test.
  * To execute this test, you'll also need the `$APP_PACKAGE_NAME` of an Android app such as `com.fng.foxnation` for the Fox Nation app.
* **Steps**
  *  Execute the CURL command from your laptop to open the Google Play Store page on the Android TV for a specific app.

```
curl -X POST -H "Content-Type: application/json" -d '{"packageName":{$APP_PACKAGE_NAME}}' http://$ANDROID_TV_IP:$ASSIST_SERVICE_PORT/launchPlayStore’
```

  *  Example: `curl -X POST -H "Content-Type: application/json" -d '{"packageName":"com.fng.foxnation"}' http://192.168.1.136:40661/launchPlayStore`. You should see an output like below.
     
```
{"state":"Success"}
```

* **Result** -  Confirm that Google Play Store is opened on the Android TV and deeplinked to the correct app page listing.
