package com.spark.player.internal;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class Utils {
public static int dp2px(Context context, int dp){
    Resources r = context.getResources();
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
        r.getDisplayMetrics());
}

public static String fix_url(String url){
    return url!=null && url.startsWith("//") ? "https:"+url : url;
}
}
