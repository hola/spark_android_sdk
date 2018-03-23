package com.spark.player;

public interface SparkModule {
Const.feature_status get_status();
void set_status(Const.feature_status new_status);
Object get_state(String param);
void set_state(Object obj);
String module_cb(String fn, String value);
}
