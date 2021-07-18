package org.floens.chan.core.site.sites.chan4;

import android.util.Pair;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonArrayRequest;

import org.floens.chan.Chan;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Hashtable;

public class Chan4PagePositionFooter {

    private static final int timeout_connect = 1000;
    private static final int timeout_read = 5000;
    private static Hashtable<String, Pair<Hashtable<Integer, String>, Long>> cache = null;

    public static String getPage(String board, int thread) {
        String result = null;
        try {
            if (cache == null) {
                cache = new Hashtable<String, Pair<Hashtable<Integer, String>, Long>>();
            }
            Pair<Hashtable<Integer, String>, Long> stored = cache.get(board);
            if (stored != null && stored.second + 30000 > System.currentTimeMillis()) {
                if (stored.first.containsKey(thread)) {
                    return "\n[page " + String.valueOf(stored.first.get(thread)) + "]";
                }
            }

            // if we couldn't return a page position, reload the cache
            RequestQueue requestQueue = Chan.getInstance().injector().instance(RequestQueue.class);
            requestQueue.add(new JsonArrayRequest("https://a.4cdn.org/" + board + "/threads.json",
                    new Response.Listener<JSONArray>() {

                        @Override
                        public void onResponse(JSONArray jsonArray) {
                            try {
                                long now = System.currentTimeMillis();
                                if (cache.get(board) != null && cache.get(board).second + 30000 > now) {
                                    return;
                                }
                                cache.put(board, new Pair<Hashtable<Integer, String>, Long>(null, now));
                                Hashtable<Integer, String> pages = new Hashtable<Integer, String>();
                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    int page = jsonObject.getInt("page");
                                    JSONArray threads = jsonObject.getJSONArray("threads");
                                    for (int j = 0; j < threads.length(); j++) {
                                        pages.put(threads.getJSONObject(j).getInt("no"), page + " / post " + (j + 1) + "/" + threads.length());
                                    }
                                }
                                cache.put(board, new Pair<Hashtable<Integer, String>, Long>(pages, now));
                            } catch (Exception ignored) {  }
                        }

                    }, null));
        } catch (Exception ignored) {  }
        return "\n[page ? / post ?]";
    }

}
