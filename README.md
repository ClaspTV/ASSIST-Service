# AndroidTV Second Screen Install (ASSIST) Service

This repository is an open source implementation of the AndroidTV Second Screen Install (ASSIST) Service.

# Overview

Mobile-to-TV discovery, install and launch of streaming apps enables rich cross-device experiences to increase viewer acquisition, engagement and monetization on TVs. The streaming industry has created open protocols and implementations to enable such mobile-to-TV interactions.  

**DIAL**

[DIAL](https://docs.google.com/viewer?a=v&pid=sites&srcid=ZGlhbC1tdWx0aXNjcmVlbi5vcmd8ZGlhbHxneDoyNzlmNzY3YWJlMmY1MjZl) protocol is an open protocol that is implemented in some Javascript and other TV platforms. DIAL enables TV app discovery and launch from mobile apps but leaves the TV app install capability as a TV platform specific implementation detail to the TV platform developers.

**ASSIST**

ASSIST is an open protocol for TV app discovery, launch and install on Android TVs. It *assists* existing open protocols like DIAL or proprietary protocols like Chromecast. The key feature of ASSIST is that it enables a secure and privacy-friendly way to initiate Android TV app install from mobile devices as part of casting flows when using DIAL or Chromecast protocols. ASSIST significantly increases the mobile-to-TV interactions by removing the biggest hurdle, i.e., automatic Android TV app install, during initial casting from a mobile app.

# Developer

## Overview

The repository provides the core ASSIST Service and enables it to be built as a (1) System Service and (2) Demo App. The System Service can be easily incorporated into existing Android OS builds (AOSP) on your TV platform. The Demo App enables simple demonstration of the ASSIST feature and also aids in its development with a simpler test cycle.

## Code Structure

The code is organized as:
* assist/ - 
* systemService/ -
* demoApp/ - 

## Demo App

### Notes

The Demo App has been designed for rapid testing of ASSIST service updates without having to build and deploy a System Service. In this mode, 

* ASSIST service runs as part of the demo app with a transparent UI. 
* The demo app must be run first for the ASSIST service to be discoverable and act on mobile commands.

### Build and Run

???

## System Service

### Notes

The System Service is the production version that can be built and immediately use in custom Android TV platforms.

### AOSP Code Changes

You can see a version of the AOSP code changes with the ASSIST Service here: <XYZ-with-assist>

Step 1: 


### Build and Deploy

???