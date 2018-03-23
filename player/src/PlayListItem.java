package com.spark.player;
import com.spark.player.internal.Utils;

public class PlayListItem {
public String m_video_url;
public String m_poster_url;
public String m_description;

public PlayListItem(String video_url, String poster_url, String description){
    m_video_url = Utils.fix_url(video_url);
    m_poster_url = Utils.fix_url(poster_url);
    m_description = description;
}
}
