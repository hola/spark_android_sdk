package com.holaspark.holaplayerdemo;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Player;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.PlayItem;
import com.holaspark.holaplayer.PlayListItem;
import com.holaspark.holaplayer.Const;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedList;
import java.util.List;
public class MainDemo extends Activity {
private HolaPlayer m_hola_player = null;
private SampleListener m_listener;
private String m_video_url;
private String m_poster_url;
@Override
protected void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    super.onCreate(saved_state);
    setContentView(R.layout.main_demo);
    m_video_url = getIntent().getStringExtra("video_url");
    m_poster_url= getIntent().getStringExtra("poster_url");
    m_hola_player = findViewById(R.id.float_player);
    m_hola_player.set_customer(getIntent().getStringExtra("customer_id"));
    m_listener = new SampleListener();
    m_hola_player.addListener(m_listener);
    Log.d(MainActivity.TAG, "Spark Player main demo");
    init();
}
@Override
protected void onResume(){
    super.onResume();
    m_hola_player.setVisibility(View.VISIBLE);
    m_hola_player.play();
}
@Override
protected void onPause(){
    super.onPause();
    m_hola_player.pause();
    m_hola_player.setVisibility(View.GONE);
}
@Override
protected void onDestroy(){
    m_hola_player.removeListener(m_listener);
    if (m_hola_player!=null)
        m_hola_player.uninit();
    super.onDestroy();
}
public void init(){
    final boolean vr = m_video_url.equals(getString(R.string.video_url_hls_vr));
    m_hola_player.vr_mode(vr);
    m_hola_player.queue(new PlayItem(vr ? null : getString(R.string.ad_tag),
        m_video_url, m_poster_url));
    // XXX andrey: properly request watch_next items
    String playlist_json = getIntent().getStringExtra("playlist_json");
    try
    {
        JSONArray json = new JSONArray(playlist_json);
        List<PlayListItem> playlist = new LinkedList<>();
        for (int i = 0; i<json.length() && playlist.size()<4; i++)
        {
            Log.d(Const.TAG, json.getJSONObject(i).toString());
            JSONObject row = json.getJSONObject(i)
                .getJSONObject("video_info");
            Log.d(MainActivity.TAG, "row "+row);
            String desc = "N/A", poster = "";
            if (row.has("description"))
                desc = row.getString("description");
            else if (row.has("title"))
                desc = row.getString("title");
            if (row.has("poster"))
                poster = row.getString("poster");
            else if (row.has("video_poster"))
                poster = row.getString("video_poster");
            if (!row.has("url"))
                continue;
            String url = row.getString("url");
            if (url.equals(m_video_url))
                continue;
            playlist.add(new PlayListItem(url, poster, desc));
        }
        if (playlist.size()<1)
            return;
        PlayListItem[] items = playlist.toArray(new PlayListItem[playlist.size()]);
        m_hola_player.set_watch_next_items(items);
    } catch(JSONException e){
        Log.d(MainActivity.TAG, "JSON exception "+e); }
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
