package com.spark.player.internal;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.spark.player.BuildConfig;
import com.spark.player.R;
import com.spark.player.SparkModule;
import com.spark.player.SparkPlayer;
import com.spark.player.SparkPlayerAPI;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class PlayerControlView extends PlaybackControlView {
private ExoPlayerController m_controller;
private SparkPlayer m_spark_player;
private Map<MenuItem, QualityItem> m_quality_map;
private Player.DefaultEventListener m_player_listener;
private View m_live_control;
private View m_position;
private View m_duration;
private DefaultTimeBar m_progress;
private ImageButton m_top_menu;
private ImageButton m_bottom_menu;
private ImageButton m_fullscreen_btn;
private ImageButton m_play_btn;
private ImageButton m_pause_btn;
private ImageButton m_replay_btn;
private boolean m_popup_visible;
public PlayerControlView(Context context){
    this(context, null);
}

public PlayerControlView(Context context, AttributeSet attrs){
    this(context, attrs, 0);
}

public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr){
    this(context, attrs, defStyleAttr, attrs);
}

public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr,
    AttributeSet playbackAttrs){
    super(context, attrs, defStyleAttr, playbackAttrs);
    m_live_control = findViewById(R.id.live_control);
    m_position = findViewById(R.id.exo_position);
    m_duration = findViewById(R.id.exo_duration);
    m_progress = findViewById(R.id.exo_progress);
    m_top_menu = findViewById(R.id.spark_player_menu_button);
    m_bottom_menu = findViewById(R.id.spark_player_gear_button);
    m_play_btn = findViewById(R.id.spark_play_button);
    m_pause_btn = findViewById(R.id.spark_pause_button);
    m_replay_btn = findViewById(R.id.spark_replay_button);
    m_fullscreen_btn = findViewById(R.id.spark_fullscreen_button);
    m_quality_map = new HashMap<>();
    OnClickListener on_click = new OnClickListener() {
        @Override
        public void onClick(View v){ show_settings_menu(v); }
    };
    m_top_menu.setOnClickListener(on_click);
    m_bottom_menu.setOnClickListener(on_click);
    m_fullscreen_btn.setOnClickListener(new OnClickListener(){
        @Override
        public void onClick(View v){
            if (m_spark_player!=null)
                m_spark_player.fullscreen(null);
        }
    });
    m_player_listener = new Player.DefaultEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playing, int state){
            update_auto_hide();
            update_playback_buttons();
        }
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest){
            boolean live = getPlayer().isCurrentWindowDynamic();
            m_live_control.setVisibility(live ? VISIBLE : GONE);
            m_position.setVisibility(live ? GONE : VISIBLE);
            m_duration.setVisibility(live ? GONE : VISIBLE);
            m_progress.setVisibility(live ? GONE : VISIBLE);
        }
    };
    OnClickListener button_click_listener = new OnClickListener() {
        @Override
        public void onClick(View v){
            Player p = getPlayer();
            if (v == m_play_btn)
                p.setPlayWhenReady(true);
            else if (v == m_pause_btn)
                p.setPlayWhenReady(false);
            else if (v == m_replay_btn)
            {
                p.seekTo(0);
                p.setPlayWhenReady(true);
            }
        }
    };
    m_play_btn.setOnClickListener(button_click_listener);
    m_pause_btn.setOnClickListener(button_click_listener);
    m_replay_btn.setOnClickListener(button_click_listener);
    update_playback_buttons();
}

@Override
public void setPlayer(Player player) {
    Player old = getPlayer();
    super.setPlayer(player);
    if (old==player)
        return;
    if (old!=null)
        old.removeListener(m_player_listener);
    if (player!=null)
        player.addListener(m_player_listener);
    update_playback_buttons();
}

private void show_settings_menu(View v){
    PopupMenu popup = new PopupMenu(getContext(), v);
    Menu menu = popup.getMenu();
    MenuInflater inflater = popup.getMenuInflater();
    inflater.inflate(R.menu.spark_settings, menu);
    populate_speed_menu(menu.findItem(R.id.speed_menu_item));
    populate_quality_menu(menu.findItem(R.id.quality_menu_item));
    menu.findItem(R.id.powered_by).setTitle(getResources().getString(R.string.powered_by)+
        " "+BuildConfig.VERSION_NAME);
    force_popup_menu_icons(popup);
    popup.setOnDismissListener(new PopupMenu.OnDismissListener(){
        @Override
        public void onDismiss(PopupMenu menu){
            m_popup_visible = false;
            update_auto_hide();
        }
    });
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
        @Override
        public boolean onMenuItemClick(MenuItem item){
            if (item.getItemId()==R.id.powered_by)
                return false;
            if (item.getGroupId()==R.id.spark_speed_menu)
                set_speed(item);
            else if (item.getGroupId()==R.id.spark_quality_menu)
                set_quality(item);
            return true;
        }
    });
    popup.show();
    m_popup_visible = true;
    update_auto_hide();
}

private void set_speed(MenuItem item){
    String title = item.getTitle().toString();
    float rate = title.equals(getResources().getString(R.string.normal)) ? 1 :
        Float.parseFloat(title.replace("x", ""));
    getPlayer().setPlaybackParameters(new PlaybackParameters(rate, rate));
}

private void set_quality(MenuItem item){
    m_controller.set_quality(m_quality_map.get(item));
}

private void populate_quality_menu(MenuItem menu_item){
    Menu menu = menu_item.getSubMenu();
    m_quality_map.clear();
    menu.clear();
    List<QualityItem> items = new ArrayList<>(m_controller.get_quality_items());
    Collections.sort(items, new QualityItem.BitrateComparator());
    if (items.size()<2)
    {
        menu_item.setVisible(false);
        return;
    }
    menu_item.setVisible(true);
    int group = R.id.spark_quality_menu;
    menu.add(group, Menu.NONE, Menu.NONE, getResources().getString(R.string.auto));
    for (QualityItem item : items)
        m_quality_map.put(menu.add(group, Menu.NONE, Menu.NONE, item.toString()), item);
}

private void populate_speed_menu(MenuItem menu_item){
    Menu menu = menu_item.getSubMenu();
    menu.clear();
    String[] rates = {"0.25", "0.5", "0.75", "1", "1.25", "1.5", "2"};
    String normal = getResources().getString(R.string.normal);
    int group = R.id.spark_speed_menu;
    for (String rate : rates)
        menu.add(group, Menu.NONE, Menu.NONE, rate.equals("1") ? normal: rate+"x");
}

// XXX andrey: find a way to show icons without reflection
public static void force_popup_menu_icons(PopupMenu popupMenu){
    try {
        Field[] fields = popupMenu.getClass().getDeclaredFields();
        for (Field field : fields)
        {
            if ("mPopup".equals(field.getName()))
            {
                field.setAccessible(true);
                Object menuPopupHelper = field.get(popupMenu);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon",
                    boolean.class);
                setForceIcons.invoke(menuPopupHelper, true);
                break;
            }
        }
    } catch (Throwable e){ e.printStackTrace(); }
}

private void update_playback_buttons(){
    Player p = getPlayer();
    boolean playing = p!=null && p.getPlayWhenReady();
    boolean ended = p!=null && p.getPlaybackState()==Player.STATE_ENDED;
    if (ended && !m_spark_player.is_floating())
        show();
    m_play_btn.setVisibility(!playing && !ended ? VISIBLE : GONE);
    m_pause_btn.setVisibility(playing && !ended ? VISIBLE : GONE);
    m_replay_btn.setVisibility(ended ? VISIBLE : GONE);
}

private void update_fullscreen_button(){
    m_fullscreen_btn.setImageResource(m_spark_player.is_fullscreen() ?
        R.drawable.ic_fullscreen_exit : R.drawable.ic_fullscreen);
}

public void update_auto_hide(){
    Player p = getPlayer();
    SparkModule wn = m_spark_player.get_watch_next_ctrl();
    set_auto_hide(p!=null && p.getPlayWhenReady() && !m_popup_visible &&
        p.getPlaybackState()!=Player.STATE_ENDED &&
        (wn==null || (boolean)wn.get_state("auto_hide")));
}

public void set_auto_hide(boolean val){
    setShowTimeoutMs(val ? DEFAULT_SHOW_TIMEOUT_MS : -1);
    if (isVisible())
        show();
}

public void set_player_controller(ExoPlayerController controller){ m_controller = controller; }
public void set_spark_player(SparkPlayer player){
    m_spark_player = player;
    boolean in_bottom = m_spark_player.get_config().m_bottom_settings_menu;
    m_top_menu.setVisibility(in_bottom ? GONE : VISIBLE);
    m_bottom_menu.setVisibility(in_bottom ? VISIBLE : GONE);
    m_spark_player.add_listener(new SparkPlayerAPI.DefaultEventListener() {
        @Override
        public void on_fullscreen_changed(boolean is_fullscreen){
            update_fullscreen_button(); }
    });
    update_fullscreen_button();
}
}
