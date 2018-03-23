package com.spark.demo;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.exoplayer2.util.Util;
import com.spark.library.FloatingPlayerBehavior;
import com.spark.player.PlayItem;
import com.spark.player.SparkPlayer;

public class VideoFragment extends Fragment {
private SparkPlayer m_spark_player;
private FloatingPlayerBehavior m_behavior;
private boolean m_playing = true;
@Override
public View onCreateView(LayoutInflater inflater, ViewGroup container,
    Bundle saved_state) {
    View view = inflater.inflate(R.layout.video_fragment, container, false);
    CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    m_behavior = new FloatingPlayerBehavior(getContext(), null);
    lp.setBehavior(m_behavior);
    view.setLayoutParams(lp);
    m_spark_player = view.findViewById(R.id.float_player);
    return view;
}

public void play_video(String video_url, String poster_url){
    final boolean vr = video_url.equals(getString(R.string.video_url_hls_vr));
    m_spark_player.vr_mode(vr);
    m_spark_player.queue(new PlayItem(vr ? null : getString(R.string.ad_tag),
        video_url, poster_url));
    m_spark_player.setPlayWhenReady(true);
    getView().setVisibility(View.VISIBLE);
    m_behavior.setState(FloatingPlayerBehavior.STATE_EXPANDED);
}

public void set_state(int state){ m_behavior.setState(state); }

public int get_state(){ return m_behavior.getState(); }

@Override
public void onStart(){
    super.onStart();
    if (Util.SDK_INT > 23)
        start();
}

@Override
public void onResume(){
    super.onResume();
    if (Util.SDK_INT <= 23)
        start();
}

@Override
public void onPause(){
    super.onPause();
    if (Util.SDK_INT <= 23)
        stop();
}

@Override
public void onStop(){
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
public void onDestroy(){
    if (m_spark_player!=null)
        m_spark_player.uninit();
    super.onDestroy();
}
}
