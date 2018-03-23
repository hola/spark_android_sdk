package com.spark.demo;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.util.Util;
import com.spark.player.SparkPlayer;
import com.spark.player.PlayItem;

public class MainDemo extends Activity {
private SparkPlayer m_spark_player = null;
private SampleListener m_listener;
private String m_video_url;
private String m_poster_url;
private boolean m_playing = true;
@Override
protected void onCreate(Bundle saved_state){
    super.onCreate(saved_state);
    setContentView(R.layout.main_demo);
    m_video_url = getIntent().getStringExtra("video_url");
    m_poster_url= getIntent().getStringExtra("poster_url");
    m_spark_player = findViewById(R.id.float_player);
    m_listener = new SampleListener();
    m_spark_player.addListener(m_listener);
    Log.d(MainActivity.TAG, "Spark Player main demo");
    init();
}
@Override
protected void onStart(){
    super.onStart();
    if (Util.SDK_INT > 23)
        start();
}
@Override
protected void onResume(){
    super.onResume();
    if (Util.SDK_INT <= 23)
        start();
}
@Override
protected void onPause(){
    super.onPause();
    if (Util.SDK_INT <= 23)
        stop();
}
@Override
protected void onStop(){
    super.onStop();
    if (Util.SDK_INT > 23)
        stop();
}
private void stop(){
    m_playing = m_spark_player.getPlayWhenReady();
    m_spark_player.setPlayWhenReady(false);
    m_spark_player.onPause();
}
private void start(){
    m_spark_player.onResume();
    m_spark_player.setPlayWhenReady(m_playing);
}
@Override
protected void onDestroy(){
    m_spark_player.removeListener(m_listener);
    if (m_spark_player!=null)
        m_spark_player.uninit();
    super.onDestroy();
}
public void init(){
    final boolean vr = m_video_url.equals(getString(R.string.video_url_hls_vr));
    m_spark_player.vr_mode(vr);
    m_spark_player.queue(new PlayItem(vr ? null : getString(R.string.ad_tag),
        m_video_url, m_poster_url));
}
class SampleListener extends Player.DefaultEventListener {
    @Override
    public void onPlayerStateChanged(boolean play, int playback_state){
        Log.d(MainActivity.TAG, play ? "play" : "pause");
        String state = playback_state==Player.STATE_IDLE ? "IDLE" :
            playback_state==Player.STATE_BUFFERING ? "BUFFERING" :
            playback_state==Player.STATE_READY ? "READY" :
            playback_state==Player.STATE_ENDED ? "ENDED" : "UNKNOWN";
        Log.d(MainActivity.TAG, "state: "+state);
    }
    @Override
    public void onSeekProcessed(){
        Log.d(MainActivity.TAG, "seeked");
    }
    @Override
    public void onPlayerError(ExoPlaybackException error){
        Log.e(MainActivity.TAG, "error: ", error);
    }
    @Override
    public void onLoadingChanged(boolean is_loading){
        Log.d(MainActivity.TAG, "loading: "+is_loading);
    }
}
}
