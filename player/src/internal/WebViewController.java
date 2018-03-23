package com.spark.player.internal;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.spark.player.BuildConfig;
import com.spark.player.Const;
public final class WebViewController {
static boolean m_js_inited = false;
private static String[] m_features = new String[]{};
private WebViewController(){}
private static Handler m_handler;
private static WebViewProxy m_proxy;
private static boolean m_ready = false;
private static void set_features(String[] features){
    Log.d(Const.TAG, "found spark features: "+
        TextUtils.join(",", features));
    WebViewController.m_features = features;
}
public static boolean check_feature(String name){
    for (String feature: m_features)
    {
        if (feature.equals(name))
            return true;
    }
    return false;
}
static synchronized void trigger_js(){
    if (m_js_inited)
        return;
    m_js_inited = true;
    m_proxy.trigger_js();
}
private static class WebViewHolder {
    private static WebView m_webview;
}
static WebView get_instance(){ return WebViewHolder.m_webview; }
public static synchronized void init(Context ctx, String customer_id,
    boolean force)
{
    WebView webview = WebViewHolder.m_webview;
    if (webview!=null)
    {
        if (!force)
            return;
        WebViewHolder.m_webview = null;
        m_js_inited = false;
        if (m_proxy!=null)
            m_proxy.unregister_all();
        webview.clearHistory();
        webview.clearCache(false);
        webview.loadUrl("about:blank");
        webview.destroy();
    }
    m_features = new String[]{};
    m_handler = new Handler(Looper.myLooper() != null ? Looper.myLooper() :
        Looper.getMainLooper());
    webview = new WebView(ctx);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
    {
        WebView.setWebContentsDebuggingEnabled(true);
        webview.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }
    final WebSettings settings = webview.getSettings();
    settings.setJavaScriptEnabled(true);
    settings.setSupportZoom(false);
    settings.setUserAgentString(settings.getUserAgentString()+" "+
        Const.PLAYER_NAME+"/"+BuildConfig.VERSION_NAME);
    settings.setDomStorageEnabled(true);
    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
    webview.setVerticalScrollBarEnabled(false);
    webview.setWebChromeClient(new ConsoleClient());
    webview.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    webview.setVisibility(View.GONE);
    m_proxy = new WebViewProxy();
    webview.addJavascriptInterface(m_proxy, "hola_java_proxy");
    String webview_url = "http://player.h-cdn.com/webview?customer="+
        customer_id+"&full=1";
    Log.d(Const.TAG, "Webview - started loading of "+webview_url);
    webview.setWebViewClient(new WebViewClient(){
        @Override
        public void onPageFinished(WebView view, String url){
            super.onPageFinished(view, url);
            Log.d(Const.TAG, "finished loading of "+url);
            check_ready();
        }
    });
    webview.loadUrl(webview_url);
    WebViewHolder.m_webview = webview;
}
private static void check_features(){
    evaluate("window.hola_cdn.api.get_spark().get_features()",
        new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s){
                set_features(s.substring(1, s.length()-1).split(","));
            }
        });
}
// XXX andrey/pavelki: add no_small param instead of checking ready
private static void check_ready(){
    WebViewController.evaluate("javascript:window.hola_cdn",
        new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s){
                if (s!=null && !s.equals("null")&& !s.equals("undefined"))
                {
                    m_ready = true;
                    Log.d(Const.TAG, "Webview ready");
                    check_features();
                    return;
                }
                m_handler.postDelayed(new Runnable() {
                    @Override
                    public void run(){ check_ready(); }
                }, 300);
            }
        });
}
static boolean is_ready(){ return m_ready; }
static void register(ExoPlayerController exoPlayerController){
    if (m_proxy==null)
        return;
    m_proxy.register_proxy(exoPlayerController);
}
static void unregister(ExoPlayerController exoPlayerController){
    if (m_proxy==null)
        return;
    m_proxy.unregister_proxy(exoPlayerController);
}
static void evaluate(String script, ValueCallback<String> cb){
    if (get_instance()==null)
        return;
    get_instance().evaluateJavascript(script, cb);
}
private final static class ConsoleClient extends WebChromeClient {
    @Override
    public boolean onConsoleMessage(ConsoleMessage msg){
        String source = msg.sourceId();
        source = Uri.parse(source).getLastPathSegment();
        Log.i(Const.TAG+"/JS", msg.messageLevel().name()+":"+source+
            ":"+msg.lineNumber()+" "+msg.message());
        return true;
    }
}
}
