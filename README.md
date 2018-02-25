# Spark Android SDK

This document describes integration of a native Android app with Spark player and feature library.

The SDK has two parts

## Spark Player
a full featured player based on Android ExoPlayer that can be integrated as is without requiring a Spark customer id.
It includes the following feature set:
- Ad support using [Google IMA module](https://developers.google.com/interactive-media-ads/docs/sdks/android/compatibility)
- 360 video playback with gesture and gyro control
- Customized high quality controls

**Requirements**
- Android version 4.4 and above

## Spark feature library

The Spark feature library is a rich toolset for enriching and enhancing the user exeprience of your native apps with features like Video previews, floating player, watch next suggestions, etc - [Full available feature set](https://holaspark.com) 
The library also includes:
- VPAID addon 
- [External APIs](https://docs.google.com/document/d/1Rh8TWTDyBdkLnnr4RVnRNZ1bSltT5NIn5dcNpdxxdQE/edit#heading=h.uo3s9j23kuim) for control & customization of all features

**Requirements**
- Registering with [Spark](https://holaspark.com) receiving a customer id and using it in library integration.

Note: An [iOS version](https://github.com/hola/spark_ios_sdk) is also available.

If you have any questions, email us at support@holaspark.com

