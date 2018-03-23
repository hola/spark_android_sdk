package com.spark.player.internal;
import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import com.spark.player.SparkPlayer;

public class FullScreenPlayer extends Dialog {
private boolean m_active;
private SparkPlayer m_player;
private PlayerViewManager m_viewmanager;
public FullScreenPlayer(@NonNull Context context){
    super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    m_active = false;
    m_viewmanager = new PlayerViewManager(context);
}
public void activate(SparkPlayer player){
    m_player = player;
    m_viewmanager.detach(player);
    addContentView(player, new ViewGroup.LayoutParams(ViewGroup.LayoutParams
        .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    m_active = true;
    show();
}
public void restore_player(){
    if (!m_active)
        return;
    m_viewmanager.restore(m_player);
    m_active = false;
    dismiss();
}
@Override
public void onBackPressed(){
    m_player.fullscreen(false);
    super.onBackPressed();
}
}
