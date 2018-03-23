package com.spark.player.internal;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.AdaptiveMediaSourceEventListener;
import com.google.android.exoplayer2.source.BehindLiveWindowException;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.hls.playlist.HlsPlaylistTracker;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.FixedTrackSelection;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.spark.player.BuildConfig;
import com.spark.player.Const;
import com.spark.player.SparkPlayer;
import com.spark.player.SparkPlayerCallback;
import com.spark.player.PlayItem;
import net.protyposis.android.spectaculum.InputSurfaceHolder;
import net.protyposis.android.spectaculum.SpectaculumView;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
public class ExoPlayerController {
private static final TrackSelection.Factory FIXED_FACTORY = new FixedTrackSelection.Factory();
private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
private final SparkPlayer m_player;
private Context m_context;
private ExoPlayer m_exoplayer;
private Listener m_listener;
private Renderer[] m_renderers;
private DefaultTrackSelector m_trackselector;
private Handler m_handler;
private DefaultDataSourceFactory m_datasource;
private List<VideoEventListener> m_clientlistener = new LinkedList<>();
private ImaAdsLoader m_ads_loader;
private ViewGroup m_overlay;
private Surface m_surface;
private PlayerControlView m_controlbar;
private boolean m_surface_own;
private String m_customer;
private boolean m_render_first = false;
final private thread_t m_timeupdate = new thread_t();
final private Queue<String> m_msg_queue = new LinkedList<>();
private String m_state = "NONE";
private String m_media_url = "";
private SparkPlayerCallback m_playlist_cb;
private View m_video_view;
private boolean m_js_attach_ready = false;
// XXX pavelki/andrey: TODO
public ExoPlayerController(SparkPlayer spark_player){
    m_player = spark_player;
    m_customer = SparkPlayer.get_customer();
}
public SparkPlayer get_player(){ return m_player; }
public boolean init(Context context, ViewGroup overlay)
{
    m_context = context;
    m_overlay = overlay;
    RenderersFactory factory = new DefaultRenderersFactory(context);
     m_handler = new Handler(Looper.myLooper() != null ?
        Looper.myLooper() : Looper.getMainLooper());
    m_listener = new Listener();
    m_renderers = factory
        .createRenderers(m_handler, m_listener, m_listener, m_listener,
            m_listener);
    TrackSelection.Factory adaptive_selection_factory =
        new AdaptiveTrackSelection.Factory(BANDWIDTH_METER);
    m_trackselector = new DefaultTrackSelector(adaptive_selection_factory);
    LoadControl loadcontrol = new DefaultLoadControl();
    m_exoplayer = ExoPlayerFactory
        .newInstance(m_renderers, m_trackselector, loadcontrol);
    m_datasource = new DefaultDataSourceFactory(m_context, BANDWIDTH_METER,
        new DefaultHttpDataSourceFactory(
            Util.getUserAgent(m_context, Const.PLAYER_NAME), BANDWIDTH_METER));
    m_state = "IDLE";
    WebViewController.register(this);
    if (!WebViewController.m_js_inited || !m_js_attach_ready)
        check_hola();
    set_customer(m_customer);
    return true;
}
private void set_customer(String customer){
    if (customer==null)
        return;
    m_customer = customer;
    m_exoplayer.addListener(m_listener);
    send_msg("attach", null);
}
public void play(){ m_exoplayer.setPlayWhenReady(true); }
public void pause(){ m_exoplayer.setPlayWhenReady(false); }
void seek(long position){ m_exoplayer.seekTo(position); }
public void load(String uri){ queue(new PlayItem(null, uri)); }
public void queue(PlayItem item){
    // XXX pavelki: no real queue at the moment
    MediaSource media_source = null;
    String ad_tag = item.get_ad_tag();
    Uri uri = Uri.parse(item.get_media());
    int media_type = ContentTypeDetector.detect_source(uri);
    m_media_url = uri.toString();
    for (VideoEventListener listener:  ExoPlayerController.this.m_clientlistener)
        listener.on_new_video(m_media_url);
    switch (media_type)
    {
        case C.TYPE_HLS:
            media_source =
                new HlsMediaSource(uri, m_datasource, m_handler, m_listener);
            break;
        case C.TYPE_DASH:
            media_source = new DashMediaSource(uri, m_datasource, new DefaultDashChunkSource.Factory(m_datasource), m_handler,
                m_listener);
            break;
        case C.TYPE_OTHER:
            media_source = new ExtractorMediaSource(uri, m_datasource, new DefaultExtractorsFactory(), m_handler, m_listener);
            break;
    }
    if (media_source == null)
        return;
    if (ad_tag != null && m_overlay != null)
    {
        release_ads_loader();
        m_ads_loader = new ImaAdsLoader(m_context, Uri.parse(ad_tag));
        media_source =
            new AdsMediaSource(media_source, m_datasource, m_ads_loader, m_overlay);
        m_ads_loader.addCallback(m_listener);
    }
    update_state("STARTING");
    m_exoplayer.prepare(media_source);
    update_state(m_exoplayer.getPlayWhenReady() ? "PLAYING" : "PAUSED");
    m_render_first = false;
}
public String get_url(){ return m_media_url; }
private void remove_video_view_callback(){
    if (m_video_view==null)
        return;
    if (m_video_view instanceof SpectaculumView)
    {
        ((SpectaculumView) m_video_view).getInputHolder()
            .removeCallback(m_listener);
    }
    else if (m_video_view instanceof TextureView)
        ((TextureView)m_video_view).setSurfaceTextureListener(null);
}
public void set_view(SpectaculumView spectaculum){
    if (m_video_view==spectaculum)
        return;
    remove_video_view_callback();
    m_video_view = spectaculum;
    spectaculum.getInputHolder().addCallback(m_listener);
}
public void set_view(TextureView texture_view){
    if (m_video_view==texture_view)
        return;
    Log.d(Const.TAG, "set texture view "+texture_view);
    remove_video_view_callback();
    m_video_view = texture_view;
    SurfaceTexture video_holder = null;
    if (texture_view != null)
    {
        texture_view.setSurfaceTextureListener(m_listener);
        video_holder =
            texture_view.isAvailable() ? texture_view.getSurfaceTexture() : null;
    }
    set_texture(video_holder);
}
public void set_texture(SurfaceTexture surface_texture){
    set_surface(surface_texture == null ? null : new Surface(surface_texture));
    m_surface_own = true;
}
private void set_surface(Surface surface){
    Surface surf = surface == null || !surface.isValid() ? null : surface;
    Log.d(Const.TAG, "set surface "+surf);
    if (m_surface != null && m_surface_own)
        m_surface.release();
    Vector<ExoPlayer.ExoPlayerMessage> messages = new Vector<>();
    for (Renderer renderer : m_renderers)
    {
        if (renderer.getTrackType() != C.TRACK_TYPE_VIDEO)
            continue;
        messages.add(
            new ExoPlayer.ExoPlayerMessage(renderer, C.MSG_SET_SURFACE, surf));
    }
    m_exoplayer.blockingSendMessages(messages.toArray(new ExoPlayer.ExoPlayerMessage[messages.size()]));
    m_surface = surface;
}
public float get_aspect(){ return m_listener.m_video_aspect; }
public int get_video_width(){ return m_listener.m_video_width; }
public int get_video_height(){ return m_listener.m_video_height; }
public boolean has_video_track(){
    MappingTrackSelector.MappedTrackInfo info =
        m_trackselector.getCurrentMappedTrackInfo();
    if (info==null)
        return false;
    for (int i = 0; i<info.length; i++)
    {
        if (m_exoplayer.getRendererType(i)!=C.TRACK_TYPE_VIDEO)
            continue;
        return info.getTrackGroups(i).length>0;
    }
    return false;
}
public ExoPlayer get_exoplayer(){ return m_exoplayer; }
List<QualityItem> get_quality_items(){
    List<QualityItem> res = new ArrayList<>();
    MappingTrackSelector.MappedTrackInfo info =
        m_trackselector.getCurrentMappedTrackInfo();
    if (info == null)
        return res;
    TrackGroupArray groups = null;
    int renderer_index = 0;
    for (int i = 0; i<info.length; i++)
    {
        if (m_exoplayer.getRendererType(i) != C.TRACK_TYPE_VIDEO)
            continue;
        groups = info.getTrackGroups(i);
        if (groups.length>0)
        {
            renderer_index = i;
            break;
        }
    }
    if (groups == null)
        return res;
    for (int i = 0; i<groups.length; i++)
    {
        TrackGroup group = groups.get(i);
        for (int j = 0; j<group.length; j++)
            res.add(new QualityItem(groups, renderer_index, i, j));
    }
    return res;
}
void set_quality(QualityItem item){
    Log.d(Const.TAG, "set quality "+(item == null ? "auto" : item));
    if (item == null)
    {
        m_trackselector.clearSelectionOverrides();
        return;
    }
    MappingTrackSelector.SelectionOverride override =
        new MappingTrackSelector.SelectionOverride(FIXED_FACTORY, item.m_group_index, item.m_track_index);
    m_trackselector
        .setSelectionOverride(item.m_renderer_index, item.m_groups, override);
}
public void set_controlbar(PlayerControlView controlbar){
    m_controlbar = controlbar;
    m_controlbar.setPlayer(m_exoplayer);
    m_controlbar.set_player_controller(this);
}
public PlayerControlView get_controlbar(){
    return m_controlbar; }
public void add_event_listener(VideoEventListener listener){
    m_clientlistener.add(listener); }
public void remove_event_listener(VideoEventListener listener){
    m_clientlistener.remove(listener); }
private void release_ads_loader(){
    if (m_ads_loader==null)
        return;
    m_ads_loader.release();
    m_ads_loader = null;
    View web_view = null;
    if (m_overlay!=null && m_overlay.getChildCount()>0)
    {
        web_view = m_overlay.getChildAt(0);
        m_overlay.removeAllViews();
    }
    if (web_view!=null && web_view instanceof WebView)
        ((WebView)web_view).destroy();
}
public void uninit(){
    remove_video_view_callback();
    m_video_view = null;
    set_surface(null);
    m_exoplayer.release();
    release_ads_loader();
    WebViewController.unregister(this);
}
public boolean is_playing(){
    return m_exoplayer.getPlayWhenReady() &&
        m_exoplayer.getPlaybackState()==Player.STATE_READY;
}
public boolean is_paused(){ return !m_exoplayer.getPlayWhenReady(); }
public boolean is_playing_ad(){ return m_exoplayer.isPlayingAd(); }
public void load_playlists(final SparkPlayerCallback cb){
    if (!WebViewController.m_js_inited || m_playlist_cb!=null)
    {
        m_handler.postDelayed(new Runnable() {
            @Override
            public void run(){ load_playlists(cb); }
        }, 300);
        return;
    }
    m_playlist_cb = cb;
    String script = "javascript:window.hola_cdn && hola_cdn.api.get_spark()"
        +".fetch_from_map_cdns.call(hola_cdn.api.get_spark(), 'get_playlists',"
        +"{customer: '"+m_customer+"', last: '1w', vinfo: 1, hits: 6, ext: 1}, "
        +"{cb: function(res){ window.hola_java_proxy.playlist_cb("
        +"JSON.stringify(res.res), "+this.hashCode()+"); }})";
    WebViewController.evaluate(script, null);
}
public void send_spark_message(String type, final String subtype,
    final String param1, final int param2, final SparkPlayerCallback cb)
{
    // XXX andrey/pavelki: future reserve
    if (BuildConfig.DEBUG && !type.equals("stats"))
        throw new AssertionError();
    if (!WebViewController.m_js_inited)
        return;
    m_handler.post(new Runnable() {
        @Override
        public void run(){
            String script = "javascript:window.hola_cdn && hola_cdn.api.get_spark()"
                +".stats."+subtype+"('"+param1+"',"+param2+")";
            WebViewController.evaluate(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s){
                    if (cb!=null)
                        cb.done(s);
                }
            });
        }
    });
}
void js_inited(){ m_player.setup_spark_modules(m_player.get_config()); }
void js_attach_ready(){ m_js_attach_ready = true; }
private class thread_t implements Runnable {
    private int cur_pos = -1;
    private volatile Thread executor;
    public void start(){
        executor = new Thread(this);
        executor.start();
    }
    void stop(){ executor = null; }
    @Override
    public void run(){
        Thread _this = Thread.currentThread();
        while (_this==executor)
        {
            int new_pos = (int)m_exoplayer.getContentPosition();
            if (new_pos!=cur_pos)
            {
                cur_pos = new_pos;
                for (VideoEventListener listener:  ExoPlayerController.this.m_clientlistener)
                    listener.time_update(cur_pos);
                send_msg("time", "\"pos\":"+cur_pos);
            }
            try { Thread.sleep(250); }
            catch(InterruptedException e){}
        }
    }
}
public interface VideoEventListener {
    void on_video_size(int width, int height);
    void on_rendered_first();
    void on_ad_start();
    void on_ad_end();
    void time_update(int cur_pos);
    void on_new_video(String url);
}
public abstract static class DefaultVideoEventListener implements VideoEventListener {
    public void on_video_size(int width, int height){}
    public void on_rendered_first(){}
    public void on_ad_start(){}
    public void on_ad_end(){}
    public void time_update(int cur_pos){}
    public void on_new_video(String url){}
}
private final class Listener implements VideoRendererEventListener,
    AudioRendererEventListener, TextOutput, MetadataOutput,
    TransferListener<DataSource>, AdaptiveMediaSourceEventListener,
    ExtractorMediaSource.EventListener, TextureView.SurfaceTextureListener,
    SurfaceHolder.Callback, InputSurfaceHolder.Callback,
    VideoAdPlayer.VideoAdPlayerCallback, Player.EventListener
{
    private float m_video_aspect = 0;
    private int m_video_width = 0;
    private int m_video_height = 0;
    @Override
    public void onVideoEnabled(DecoderCounters counters){
        Log.d(Const.TAG, "on video enabled");
    }
    @Override
    public void onVideoDecoderInitialized(String decoder_name, long timestamp,
        long duration)
    {
        Log.d(Const.TAG, "on video decoder init");
    }
    @Override
    public void onVideoInputFormatChanged(Format format){}
    @Override
    public void onDroppedFrames(int count, long elapsed){}
    @Override
    public void onVideoSizeChanged(int width, int height, int rotation,
        float pixel_aspect)
    {
        Log.d(Const.TAG, "on video size changed w "+width+" h "+height);
        m_video_width = width;
        m_video_height = height;
        float new_aspect = height == 0 ? 1 : (width*pixel_aspect)/height;
        if (new_aspect != m_video_aspect)
        {
            m_video_aspect = new_aspect;
            for (VideoEventListener listener:  ExoPlayerController.this.m_clientlistener)
                listener.on_video_size(width, height);
        }
    }
    @Override
    public void onRenderedFirstFrame(Surface surface){
        for (VideoEventListener listener:  ExoPlayerController.this.m_clientlistener)
            listener.on_rendered_first();
        if (WebViewController.get_instance()==null || m_render_first ||
            m_exoplayer.isPlayingAd())
        {
            return;
        }
        m_render_first = true;
    }
    @Override
    public void onVideoDisabled(DecoderCounters counters){}
    @Override
    public void onAudioEnabled(DecoderCounters counters){}
    @Override
    public void onAudioSessionId(int audioSessionId){}
    @Override
    public void onAudioDecoderInitialized(String decoder_name, long timestamp,
        long duration)
    {
    }
    @Override
    public void onAudioInputFormatChanged(Format format){}
    @Override
    public void onAudioSinkUnderrun(int buffer_size, long buffer_size_ms,
        long elapsed)
    {
    }
    @Override
    public void onAudioDisabled(DecoderCounters counters){}
    @Override
    public void onMetadata(Metadata metadata){
        Log.d(Const.TAG, "metadata "+metadata);
    }
    @Override
    public void onCues(List<Cue> cues){}
    @Override
    public void onTransferStart(DataSource source, DataSpec data_spec){}
    @Override
    public void onBytesTransferred(DataSource source, int transferred){}
    @Override
    public void onTransferEnd(DataSource source){}
    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs)
    {
    }
    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs, long bytesLoaded)
    {
    }
    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs, long bytesLoaded)
    {
    }
    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType,
        Format trackFormat, int trackSelectionReason, Object trackSelectionData,
        long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs,
        long loadDurationMs, long bytesLoaded, IOException error,
        boolean wasCanceled)
    {
    }
    @Override
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs,
        long mediaEndTimeMs)
    {
    }
    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat,
        int trackSelectionReason, Object trackSelectionData, long mediaTimeMs)
    {
        Log.d(Const.TAG, "downstream format "+trackFormat);
    }
    @Override
    public void onLoadError(IOException error){}
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
        int height)
    {
        set_surface(new Surface(surface));
        m_surface_own = true;
    }
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
        int height)
    {
    }
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){
        set_surface(null);
        return true;
    }
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface){}
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder){
        set_surface(surfaceHolder.getSurface());
        m_surface_own = false;
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1,
        int i2)
    {
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder){
        set_surface(null);
    }
    @Override
    public void surfaceCreated(InputSurfaceHolder holder){
        set_texture(holder.getSurfaceTexture());
        m_surface_own = true;
    }
    @Override
    public void surfaceDestroyed(InputSurfaceHolder holder){
        set_texture(null);
    }
    @Override
    public void onPlay(){
        for (VideoEventListener listener:  ExoPlayerController.this.m_clientlistener)
            listener.on_ad_start();
    }
    @Override
    public void onVolumeChanged(int i){}
    @Override
    public void onPause(){}
    @Override
    public void onResume(){}
    @Override
    public void onEnded(){
        for (VideoEventListener listener:  ExoPlayerController.this.m_clientlistener)
            listener.on_ad_end();
    }
    @Override
    public void onError(){}
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest){}
    @Override
    public void onTracksChanged(TrackGroupArray trackGroups,
        TrackSelectionArray trackSelections)
    {
    }
    @Override
    public void onLoadingChanged(boolean isLoading){}
    @Override
    public void onPlayerStateChanged(boolean play, int state){
        String newstate = play ? "PLAYING" : "PAUSED";
        if (state==Player.STATE_ENDED)
            newstate = "IDLE";
        if (!newstate.equals(m_state))
            update_state(newstate);
    }
    @Override
    public void onRepeatModeChanged(int repeatMode){}
    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled){}
    @Override
    public void onPlayerError(ExoPlaybackException e){
        Log.e(Const.TAG, "Player error "+e);
        if (e.type!=ExoPlaybackException.TYPE_SOURCE)
            return;
        Throwable cause = e.getSourceException();
        while (cause!=null)
        {
            if (cause instanceof BehindLiveWindowException ||
                cause instanceof HlsPlaylistTracker.PlaylistResetException ||
                cause instanceof HlsPlaylistTracker.PlaylistStuckException)
            {
                Log.d(Const.TAG, "Restart playback");
                load(m_media_url);
                return;
            }
            cause = cause.getCause();
        }
    }
    @Override
    public void onPositionDiscontinuity(int reason){}
    @Override
    public void onPlaybackParametersChanged(PlaybackParameters
        playbackParameters)
    {
    }
    @Override
    public void onSeekProcessed(){}
}
private void update_state(String new_state){
    if (new_state.equals(m_state) || m_media_url.equals(""))
        return;
    Log.d(Const.TAG, "State changed: from "+m_state+" to "+new_state);
    if (new_state.equals("PLAYING"))
        m_timeupdate.start();
    if (new_state.equals("IDLE")||new_state.equals("PAUSED"))
        m_timeupdate.stop();
    send_msg("state", "\"data\":\""+new_state+'|'+m_state+"\"");
    m_state = new_state;
}
private void send_msg(String cmd, String data){
    final String msg = "{\"cmd\":\""+cmd+"\""+",\"hash\":"+this.hashCode()+
        (data!=null ? ","+data : "")+"}";
    m_handler.post(new Runnable(){
        @Override
        public void run(){
            synchronized(m_msg_queue){
                if (!m_js_attach_ready || m_msg_queue.size()>0)
                {
                    m_msg_queue.add(msg);
                    return;
                }
            }
            send_string(msg);
        }
    });
}
private void send_string(String msg){
    Log.d(Const.TAG, "send message "+msg);
    WebViewController.evaluate("javascript:window.hola_cdn && hola_cdn"+
        ".android_message('"+msg+"')", null);
}
private void check_hola(){
    if (!WebViewController.m_js_inited || !m_js_attach_ready)
    {
        m_handler.postDelayed(new Runnable() {
            @Override
            public void run(){ check_hola(); }
        }, 300);
    }
    if (WebViewController.get_instance()==null || !WebViewController.is_ready())
        return;
    m_handler.post(new Runnable() {
        @Override
        public void run(){
            WebViewController.evaluate("javascript:window.hola_cdn && "+
                "typeof hola_cdn.android_message", new ValueCallback<String>()
            {
                @Override
                public void onReceiveValue(String s){
                    if (!s.equals("\"function\""))
                        return;
                    Log.d(Const.TAG, "JS engine is inited");
                    WebViewController.trigger_js();
                    if (!m_js_attach_ready || m_msg_queue.size()<0)
                        return;
                    synchronized(m_msg_queue){
                        Log.d(Const.TAG, "flush msg queue");
                        if (m_msg_queue.size()>0)
                        {
                            for (String msg: m_msg_queue)
                                send_string(msg);
                            m_msg_queue.clear();
                        }
                    }
                }
            });
        }
    });
}
int get_duration(){ return (int) m_exoplayer.getDuration(); }
int get_pos(){ return (int) m_exoplayer.getCurrentPosition(); }
void seek(int ms){ this.seek((long)ms); }
boolean is_live_stream(){ return m_exoplayer.isCurrentWindowDynamic(); }
boolean is_prepared(){
    int state = m_exoplayer.getPlaybackState();
    return state != Player.STATE_IDLE;
}
String get_app_label(){
    PackageManager pm = m_context.getPackageManager();
    return (String) pm.getApplicationLabel(m_context.getApplicationInfo());
}
void playlist_cb(final String res){
    if (m_playlist_cb == null)
        return;
    m_handler.post(new Runnable() {
        @Override
        public void run(){
            m_playlist_cb.done(res);
            m_playlist_cb = null;
        }
    });
}
String module_cb(String module, String fn, String value){
    return m_player.module_cb(module, fn, value);
}
}
