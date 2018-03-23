package com.spark.player.internal;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class PlayerViewManager {
private ViewGroup m_parent;
private int m_parent_index;
private ViewGroup.LayoutParams m_layout_params;
private FrameLayout m_holder;
public PlayerViewManager(Context context){
    m_holder = new FrameLayout(context);
}
public FrameLayout get_holder(){ return m_holder; }
public void detach(View player){
    m_parent = (ViewGroup)player.getParent();
    m_parent_index = m_parent.indexOfChild(player);
    m_layout_params = player.getLayoutParams();
    ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(m_layout_params);
    p.width = player.getWidth();
    p.height = player.getHeight();
    m_parent.removeViewAt(m_parent_index);
    m_parent.addView(m_holder, m_parent_index, p);
}
public void restore(View player){
    m_parent.removeView(m_holder);
    ((ViewGroup) player.getParent()).removeView(player);
    m_parent.addView(player, m_parent_index, m_layout_params);
}
}
