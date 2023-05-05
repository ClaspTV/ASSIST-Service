# AndroidTV Second Screen Install (ASSIST) Service

This repository is an open source implementation of the AndroidTV Second Screen Install (ASSIST) Service.

# Overview

Mobile-to-TV discovery, install and launch of streaming apps enables rich cross-device experiences to increase viewer acquisition, engagement and monetization on TVs. The streaming industry has created open & proprietary protocols to enable such mobile-to-TV interactions.  

**DIAL**

[DIAL](https://docs.google.com/viewer?a=v&pid=sites&srcid=ZGlhbC1tdWx0aXNjcmVlbi5vcmd8ZGlhbHxneDoyNzlmNzY3YWJlMmY1MjZl) is an open protocol that is implemented in some TV platforms. DIAL enables TV app discovery and launch from mobile apps but leaves the mobile-to-TV app install capability as a TV platform specific implementation detail to the TV platform developers.

**GOOGLECAST**

[GoogleCast](https://www.google.com/intl/en_us/chromecast/built-in/) is Google's proprietary protocol that is implemented on Android TVs. GoogleCast enables the TV app discovery and launch from mobile apps and does not have any support for mobile-to-TV app install.

**ASSIST**

ASSIST is an open protocol for mobile-to-TV app install on Android TVs. It *assists* existing open protocols like DIAL or proprietary protocols like GoogleCast. The key feature of ASSIST is that it enables a secure and privacy-friendly way to initiate Android TV app install from mobile devices as part of casting flows when using protocols like DIAL or Chromecast. The big contribution of ASSIST is that it significantly increases the mobile-to-TV user interactions by removing the biggest hurdle, i.e., automating Android TV app install, during initial casting from a mobile app.

# ASSIST Specification

## ASSIST MDNS Discovery

ASSIST service can be discovered by 2nd screen devices by using the MDNS target `_vzb-assist._tcp.`.

## ASSIST REST APIs

Once discovered, the ASSIST service shares a HTTPS end-point which supports the following REST APIs.

| Method Type | Method Name | Method Parameters/Body| Response Code | Response Body | Notes|
| :---        | :---   | :---   | :---   | :---   | :--- |
| GET   | appInstallationStatus | packageName = com.crackle.androidtv | 200 OK | ```{state: "App Installed"}``` | Success scenario. |
|       | | | 404 Not Found | N/A | Missing packageName parameter in the URL. |
|       | | | 500 Internal Service Error | N/A | Server execution error. |
| POST  | launchAppStore        | {}      | | | |

<method type> <method name> <response code> <response body>

# Developer

## Overview

The repository provides the core ASSIST Service and enables it to be built as a (1) System Service and/or a (2) Demo App. The System Service can be easily incorporated into existing Android OS builds (AOSP) on your streaming TV platform. The Demo App enables simple demonstration of the ASSIST feature and also aids in development with a simpler test cycle.

## Demo App

The Demo App has been designed for rapid testing of ASSIST service updates without having to build and deploy a System Service. In this mode, 

* ASSIST service runs as part of the demo app with a transparent UI. 
* The demo app must be run first for the ASSIST service to be discoverable and act on mobile commands.

### Build and Run

* Set the target to `demo-app` in Android Studio
* Select the Android TV device or simulator
* Hit the run `Run demo-app` button

## System Service

The System Service can be incorporated, built and immediately used in custom Android TV platforms.

### AOSP Code Changes

You can see a version of the AOSP code changes with the ASSIST Service here: <TODO-XYZ-with-assist>

Step 1: Copy the mentioned ServiceManager classes from `system-service` module of ASSIST project to your AOSP File location:  `frameworks/base/core/java/android/app`  
Files to be Copied:  
  `AssistServiceManager.java`  
  `lAssistServiceManager.aidl`  
  `IAssistServiceManager.java`  

Step 2: Copy the mentioned Service class from `system-service` module of ASSIST project to your AOSP File location: `frameworks/base/services/core/java/com/android/server`  
File to be Copied:  
  `AssistService.Java`

Step 3: Modify the following files to register the System Service  
  
File: `frameworks/base/core/java/android/app/SystemServiceRegistry.java`  
```
//SystemServiceRegistry.java
registerService(Context.ASSIST_SERVICE, AssistServiceManager.class,
            new CachedServiceFetcher < AssistServiceManager > () {
                @Override
                public AssistServiceManager createService(ContextImpl ctx) throws ServiceNotFoundException {
                    IBinder binder;
                    if (ctx.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O) {
                        binder = ServiceManager.getServiceOrThrow(Context.ASSIST_SERVICE);
                    } else {
                        binder = ServiceManager.getService(Context.ASSIST_SERVICE);
                    }
                    return new AssistServiceManager(ctx, IAssistServiceManager.Stub.asInterface(binder));
                }
            });
  
```
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

Step 4: Build the AOSP and run it.

Step 5: Check If the System Service Is running using Logs.

### Build and Deploy

???
