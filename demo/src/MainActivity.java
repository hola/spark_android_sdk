package com.spark.demo;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.spark.library.FloatingPlayerBehavior;
import com.spark.player.internal.Utils;
import com.spark.player.SparkPlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends ListActivity {
static final String TAG = "SparkPlayerDemo";
private String m_customer_id;
private View m_progress;
private ArrayList<Article> m_articles;
private ImageLoader m_image_loader;
private RequestQueue m_request_queue;
private VideoFragment m_video_fragment;
private boolean m_pinned_mode;

@Override
public void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    m_customer_id = getIntent().getStringExtra("customer_id");
    SparkPlayer.set_customer(this, m_customer_id);
    super.onCreate(saved_state);
    setContentView(R.layout.main_activity);
    m_progress = findViewById(R.id.progress_bar);
    m_request_queue = Volley.newRequestQueue(this);
    m_image_loader = new ImageLoader(m_request_queue, new ImageLoader.ImageCache() {
        private final LruCache<String, Bitmap> m_cache = new LruCache<>(10);
        public void putBitmap(String url, Bitmap bitmap) { m_cache.put(url, bitmap); }
        public Bitmap getBitmap(String url) { return m_cache.get(url); }
    });
    m_articles = new ArrayList<>();
    findViewById(R.id.spark_header).setOnLongClickListener(
        new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v){
            PopupMenu popup = new PopupMenu(MainActivity.this, v);
            final Menu menu = popup.getMenu();
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.settings, menu);
            popup.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener(){
                @Override
                public boolean onMenuItemClick(MenuItem item){
                    int id = item.getItemId();
                    m_pinned_mode = id==R.id.pinned_mode;
                    return true;
                }
            });
            popup.show();
            return true;
        }
    });
    load_playlist();
}
public void load_playlist(){
    // XXX andrey: use player API
    final String mode = "new";
    String url = new Uri.Builder().scheme("http")
        .authority("player.h-cdn.com").path("api/get_playlists")
        .appendQueryParameter("customer", m_customer_id)
        .appendQueryParameter("last", mode)
        .appendQueryParameter("vinfo", "1")
        .appendQueryParameter("hits", "6")
        .appendQueryParameter("ext", "1").build().toString();
    JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
        new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response){
                Iterator<String> keys = response.keys();
                while (keys.hasNext())
                {
                    String key = keys.next();
                    if (key.contains("_popular_") &&
                        response.optJSONArray(key).length()>0)
                    {
                        create_articles(response.optJSONArray(key));
                        m_progress.setVisibility(View.GONE);
                        return;
                    }
                }
                m_progress.setVisibility(View.GONE);
                setResult(RESULT_FIRST_USER);
                finish();
            }
        }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error){
            VolleyLog.e(TAG+" Error: "+error.getMessage());
            m_progress.setVisibility(View.GONE);
        }
    }){
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> params = new HashMap<>();
            params.put("Referer", "http://player.h-cdn.com/webview?customer="+
                m_customer_id);
            return params;
        }
    };
    m_request_queue.add(req);
    m_progress.setVisibility(View.VISIBLE);
}
public void create_articles(JSONArray json){
    String lorem = getString(R.string.lorem_ipsum);
    try
    {
        for (int i = 0; i<json.length() ; i++)
        {
            Log.d(TAG, json.getJSONObject(i).toString());
            JSONObject row = json.getJSONObject(i).getJSONObject("video_info");
            String url = row.optString("url", null);
            String poster = row.optString("poster",
                row.optString("video_poster", null));
            String desc = row.optString("description",
                row.optString("title", null));
            if (url==null || poster==null || desc==null)
                continue;
            try
            {
                long expires = Long.parseLong(Uri.parse(url)
                    .getQueryParameter("expires"));
                if (System.currentTimeMillis() > expires*1000)
                    continue;

            } catch (Exception ignored){}
            m_articles.add(new Article(poster, desc, lorem, url));
        }
        m_articles.add(new Article("","360 VR player", lorem,
            getString(R.string.video_url_hls_vr)));
        m_articles.add(new Article("","Live video", lorem,
            getString(R.string.video_url_live)));
        ArticleAdapter adapter = new ArticleAdapter(this, R.layout.listview_item,
            m_articles.toArray(new Article[0]));
        setListAdapter(adapter);
    } catch(JSONException e){ Log.d(TAG, "JSON exception "+e); }
}
private void create_video_fragment(){
    m_video_fragment = new VideoFragment();
    FragmentTransaction transaction = getFragmentManager().beginTransaction();
    transaction.add(R.id.coordinator_view, m_video_fragment);
    transaction.addToBackStack(null);
    transaction.commit();
    getFragmentManager().executePendingTransactions();
}
@Override
protected void onListItemClick(ListView listview, View view, int pos, long id){
    Article article = m_articles.get(pos);
    if (m_pinned_mode)
    {
        if (m_video_fragment==null)
            create_video_fragment();
        m_video_fragment.play_video(article.m_video_url, article.m_image_url);
        return;
    }
    Intent intent = new Intent(this, MainDemo.class);
    intent.putExtra("customer_id", m_customer_id);
    intent.putExtra("video_url", article.m_video_url);
    intent.putExtra("poster_url", article.m_image_url);
    startActivity(intent);
}
@Override
public void onBackPressed(){
    if (m_pinned_mode && m_video_fragment!=null &&
        m_video_fragment.get_state()!= FloatingPlayerBehavior.STATE_COLLAPSED)
    {
        m_video_fragment.set_state(FloatingPlayerBehavior.STATE_COLLAPSED);
    }
    else
        super.onBackPressed();
}

class Article {
    String m_image_url;
    String m_title;
    String m_text;
    String m_video_url;
    Article(String image_url, String title, String text, String video_url){
        m_image_url = Utils.fix_url(image_url);
        m_title = title;
        m_text = text;
        m_video_url = Utils.fix_url(video_url);
    }
}

public class ArticleAdapter extends ArrayAdapter<Article> {
    Context m_context;
    int m_layout_resource_id;
    Article m_data[];
    ArticleAdapter(Context context, int layout_resource_id, Article[] data){
        super(context, layout_resource_id, data);
        m_layout_resource_id = layout_resource_id;
        m_context = context;
        m_data = data;
    }
    @Override
    public @NonNull View getView(int position, View row, @NonNull ViewGroup parent){
        if(row == null)
        {
            LayoutInflater inflater = ((Activity) m_context).getLayoutInflater();
            row = inflater.inflate(m_layout_resource_id, parent, false);
        }
        NetworkImageView image = row.findViewById(R.id.article_image);
        TextView title = row.findViewById(R.id.article_title);
        TextView text = row.findViewById(R.id.article_text);
        Article article = m_data[position];
        image.setDefaultImageResId(R.color.black);
        if (article.m_image_url!=null)
            image.setImageUrl(article.m_image_url, m_image_loader);
        title.setText(article.m_title);
        text.setText(article.m_text);
        return row;
    }
}
}
