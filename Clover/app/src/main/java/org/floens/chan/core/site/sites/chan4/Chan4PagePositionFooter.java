package org.floens.chan.core.site.sites.chan4;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.Hashtable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

            new AsyncTask<String, Void, Void>() {

                @Override
                protected Void doInBackground(String... params) {
                    updateCache(params[0]);
                    return null;
                }

            }.execute(board);
        } catch (Exception e) {
        }
        return "\n[page ? / post ?]";
    }

    private static void updateCache(String board) {
        try {
            long now = System.currentTimeMillis();
            if (cache.get(board) != null && cache.get(board).second + 30000 > now) {
                return;
            }
            cache.put(board, new Pair<Hashtable<Integer, String>, Long>(null, now));
            if (cache == null) {
                cache = new Hashtable<String, Pair<Hashtable<Integer, String>, Long>>();
            }
            Hashtable<Integer, String> pages = new Hashtable<Integer, String>();
            Request request = new Request.Builder()
                    .url("https://a.4cdn.org/" + board + "/threads.json")
                    .build();
            String text = new OkHttpClient().newCall(request).execute().body().string();
            JSONArray jsonArray = new JSONArray(text);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int page = jsonObject.getInt("page");
                JSONArray threads = jsonObject.getJSONArray("threads");
                for (int j = 0; j < threads.length(); j++) {
                    pages.put(threads.getJSONObject(j).getInt("no"), page + " / post " + (j + 1) + "/" + threads.length());
                }
            }
            cache.put(board, new Pair<Hashtable<Integer, String>, Long>(pages, now));
        } catch (Exception e) {
        }
    }

}
