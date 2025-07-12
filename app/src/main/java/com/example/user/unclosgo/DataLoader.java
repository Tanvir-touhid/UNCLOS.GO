package com.example.user.unclosgo;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DataLoader {
    private static final String TAG = "DataLoader";

    public static List<Article> loadArticlesFromJson(Context context) {
        List<Article> articles = new ArrayList<>();

        // Predefined list of icon resource names
        String[] iconNames = {
                "ic_p1", "ic_p2s1", "ic_p2s2", "ic_p2s3", "ic_p2s4", "ic_p3s1", "ic_p3s2", "ic_p3s3",
                "ic_p4", "ic_p5", "ic_p6", "ic_p7s1", "ic_p7s2", "ic_p8", "ic_p9", "ic_p10",
                "ic_p11s1", "ic_p11s2", "ic_p11s3", "ic_p11s4", "ic_p11s5", "ic_p12s1", "ic_p12s2",
                "ic_p12s3", "ic_p12s4", "ic_p12s5", "ic_p12s6", "ic_p12s7", "ic_p12s8", "ic_p12s9",
                "ic_p12s10", "ic_p12s11", "ic_p13s1", "ic_p13s2", "ic_p13s3", "ic_p13s4", "ic_p13s5",
                "ic_p13s6", "ic_p14s1", "ic_p14s2", "ic_p14s3", "ic_p14s4", "ic_p15s1", "ic_p15s2",
                "ic_p15s3", "ic_p16", "ic_p17"
        };

        try {
            // Load JSON file
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("articles.json");

            // Compatible with API 21+
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            inputStream.close();

            String json = result.toString("UTF-8");
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                int id = jsonObject.getInt("id");
                String title = jsonObject.getString("title");
                String content = jsonObject.getString("content");
                String iconName = jsonObject.optString("icon", "");

                int iconResId = resolveIconResource(context, iconName, id, iconNames);

                Article article = new Article(id, title, content);
                article.setIconResId(iconResId);
                articles.add(article);

                Log.d(TAG, String.format(
                        "Loaded Article: ID=%d, Title=%s, Icon=%s (ResID=%d)",
                        id, title, iconName, iconResId
                ));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error loading articles", e);
        }

        return articles;
    }

    private static int resolveIconResource(Context context, String iconName, int articleId, String[] iconNames) {
        int iconResId = 0;

        // Method 1: Try to load specified icon from JSON
        if (!iconName.isEmpty()) {
            iconResId = context.getResources().getIdentifier(
                    iconName,
                    "drawable",
                    context.getPackageName()
            );

            if (iconResId != 0) {
                return iconResId;
            }
            Log.w(TAG, "Specified icon not found: article_icons/" + iconName);
        }

        // Method 2: Fallback to cycling icons
        int iconIndex = articleId % iconNames.length;
        String fallbackIconName = iconNames[iconIndex];
        iconResId = context.getResources().getIdentifier(
                "article_icons/" + fallbackIconName,
                "drawable",
                context.getPackageName()
        );

        if (iconResId != 0) {
            Log.d(TAG, "Used fallback icon: " + fallbackIconName);
            return iconResId;
        }

        // Method 3: Ultimate fallback
        Log.w(TAG, "No valid icon found, using default");
        return R.drawable.ic_article_default;
    }
}
