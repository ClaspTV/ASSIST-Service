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

| Method Type | Method Name | Method Parameters/Body| Response Code | Response Body | Notes|
| :---        | :---   | :---   | :---   | :---   | :--- |
| GET   | appInstallationStatus | packageName = androidtv_app_package_name | 200 OK        | ```{state: "App Installed"}``` or ```{state: "App Not Installed"}``` | Success scenario. |
|       |                       |                                          | 404 Not Found | N/A | Path not found or missing packageName parameter in the URL. |
|       | | | 500 Internal Service Error | N/A | Server execution error. |
| POST  | launchAppStore        | {"packageName":"androidtv_app_package_name"} | 200 OK | | Success scenario. |
|       |         | | 404 Not Found | N/A | Path not found or missing packageName parameter in the body |
|       | | | 500 Internal Service Error | N/A | Server execution error. |

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

Step 1: Add nanohttpd library by copying nanohttpd folder from `system-service.nanohttpd` module of ASSIST project to your AOSP File location `prebuilts/mic/common/`

Folder to be Copied: 
  `nanohttpd`

Step 2: Copy the mentioned ServiceManager classes from `system-service.service` module of ASSIST project to your AOSP File location:  `frameworks/base/core/java/android/app`  
Files to be Copied:  
  `AssistHttpServer.java`  
  `AssistService.java`  
  `AssistServiceManager.java`  

Step 3: Modify the Android.bp file to use nanohttpd library
  
File: `frameworks/base/services/core/Android.bp` 
```
  //Android.bp 
  //Add nanohttpd 
   java_library_static {
    name: "services.core.unboosted",
    defaults: ["platform_service_defaults"],
    static_libs: [   
+	    “nanohttpd”
    ],
  }

  ```

Step 4: Modify the AndroidManifest for adding AssistService and NanoHTTPD 
  
File: `frameworks/base/core/res` 
```
  //AndroidManifest 
  //Add nanohttpd 
  <uses-library android:name = "fi.iki.elonen.NanoHTTPD" />
  <service android:name="com.android.server.AssistService"
    android:exported="false" />

  ```

Step 5: Modify the following files to register the System Service  
  
File: `frameworks/base/core/java/android/content/Context.java` 
```
  //Context.java 
  //Add ASSIST_SEVICE 
   @StringDef(suffix = {
            "_SERVICE"
        }, value = {
+           ASSIST_SERVICE,
            ACCOUNT_SERVICE,
            ACTIVITY_SERVICE,
            ALARM_SERVICE,
            NOTIFICATION_SERVICE,
            ACCESSIBILITY_SERVICE,
            CAPTIONING_SERVICE,
        })
  
+  public static final String ASSIST_SERVICE = "assist"; 

  ```
  
File: `frameworks/base/services/java/com/android/server/SystemServer.java`
  ```
  //SystemServer.java
        private static final String ASSIST_SERVICE = "com.android.server.AssistService";

        //added in the startBootstrapServices() function 
        AssistService assistservice = null;
        try {
            traceBeginAndSlog("AssistService");
            assistservice = new AssistService(mSystemContext);
            ServiceManager.addService(Context.ASSIST_SERVICE, assistservice);
        } catch (Throwable e) {
            Slog.e(TAG, "Starting AssistService failed!!! ", e);
        }
        traceEnd();
        
  ```

File: `frameworks/base/core/api/current.txt`
  ```

        public abstract class Context {
        field public static final String ASSIST_SERVICE = "assist";
        }
    
  ```

Step 6: Build the AOSP and run it.

Step 7: Check If the System Service Is running using Logs.

### Build and Deploy

