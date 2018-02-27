# Spark Android SDK

##Introduction

This document describes integration of a native Android app with Spark Player and feature library.

The SDK has two parts

- [Spark Player](https://github.com/hola/spark_android_sdk/blob/master/player):
  A full featured player based on Android ExoPlayer that can be integrated as is without requiring a Spark customer id.

- [Spark Feature Library](https://github.com/hola/spark_android_sdk/blob/master/lib):
The Spark feature library is a toolset for enriching and enhancing the user exeprience of your native apps with features like Video previews, floating player, watch next suggestions, etc - see the [Full available feature set](https://holaspark.com) 

Try out the complete feature set in our [Spark Player and Feature Library demo app](https://play.google.com/store/apps/details?id=com.holaspark.holaplayerdemo)

##Basic integration

Integration of Spark Player with an app, can be done in two easy steps:

###Step 1

Add Spark Player to a layout file with required set of features and options:

```xml
<LinearLayout
    android:layout_width="match_parent" android:layout_height="wrap_content"
    android:orientation="vertical">
    <com.spark.player.SparkPlayer
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:id="@+id/spark_player"
        app:position_memory="false"/>
</LinearLayout>
```
By default, all Spark features for a &lt;customer_id&gt; are enabled. However, one can disable some features by providing "false" attribute to SparkPlayer layout record.

###Step 2

onCreate an activity of your app find Spark Player view, set customer id and queue an item:

```java
    m_spark_player = findViewById(R.id.spark_player);
    m_spark_player.set_customer("demo");
    m_spark_player.queue(new PlayItem(getString(R.string.ad_tag),
        getString(R.string.video_url),
        getString(R.string.poster_url));
```

Note: An [iOS version](https://github.com/hola/spark_ios_sdk) is also available.

If you have any questions, email us at support@holaspark.com