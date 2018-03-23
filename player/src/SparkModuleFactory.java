package com.spark.player;
import com.spark.player.internal.ExoPlayerController;
public interface SparkModuleFactory {
com.spark.player.SparkModule init(SparkPlayer player, ExoPlayerController exoplayer);
String get_name();
}
