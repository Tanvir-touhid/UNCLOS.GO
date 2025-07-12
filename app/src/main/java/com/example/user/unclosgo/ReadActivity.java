package com.example.user.unclosgo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.search.SearchBar;

import java.util.List;

public class ReadActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private ArticleAdapter adapter;
    private List<Article> articleList;
    private EditText searchEditText;

    private final BroadcastReceiver progressUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BroadcastDebug", "Broadcast received in " + getClass().getSimpleName());
            // Recalculate progress and refresh list
            calculateReadingProgress();
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set up the adapter
        articleList = DataLoader.loadArticlesFromJson(this);
        adapter = new ArticleAdapter(this, articleList);
        recyclerView.setAdapter(adapter);

        // Bind TextView for progress

        // Set up the SearchView
        searchEditText = findViewById(R.id.searchEditText);
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    adapter.filter(s.toString());
                }

                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Calculate initial reading progress
        calculateReadingProgress();
    }

    private void setupSearchView() {
        // Access the TextInputEditText inside SearchView
        searchEditText = findViewById(R.id.searchEditText);

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Filter the articles as the user types
                    adapter.filter(s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        calculateReadingProgress();
        adapter.notifyDataSetChanged();

        // Clear search when returning to the activity
        if (searchEditText != null) {
            searchEditText.setText("");
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter("com.example.user.unclosgo.ARTICLE_PROGRESS_UPDATED");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

        // Register receiver dynamically with NOT_EXPORTED flag to restrict to your app
        registerReceiver(progressUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }


    private void calculateReadingProgress() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        int readCount = 0;
        for (Article article : articleList) {
            if (prefs.getBoolean("read_" + article.getTitle(), false)) {
                readCount++;
            }
        }

        int totalArticles = articleList.size();
        float percentage = (readCount / (float) totalArticles) * 100;
        String formattedPercentage = String.format("Progress: %.2f%%", percentage);

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        recreate();
    }

    private void markArticleAsReviewed(Article article, String reviewChoice) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();

        article.setReviewChoice(reviewChoice);
        updateSpacedRepetition(article);

        editor.putBoolean("read_" + article.getTitle(), true);
        editor.putLong("reviewTime_" + article.getTitle(), article.getReviewTime());
        editor.putString("reviewChoice_" + article.getTitle(), article.getReviewChoice());
        editor.apply();

        sendProgressUpdateBroadcast();
    }

    private void sendProgressUpdateBroadcast() {
        Intent intent = new Intent("com.example.user.unclosgo.ARTICLE_PROGRESS_UPDATED");
        intent.setPackage(getPackageName());  // restrict to your app only
        sendBroadcast(intent);  // send a normal broadcast scoped to your app
    }



    private void updateSpacedRepetition(Article article) {
        long currentTime = System.currentTimeMillis();
        String reviewChoice = article.getReviewChoice();
        int repetitions = article.getRepetitions();
        float easeFactor = article.getEaseFactor();
        int interval = article.getInterval();

        if (reviewChoice.equals("again")) {
            repetitions = 0;
            interval = 1;
        } else if (reviewChoice.equals("hard")) {
            repetitions += 1;
            interval *= 2;
        } else {
            repetitions++;
            interval = (int) (interval * easeFactor);
        }

        article.setRepetitions(repetitions);
        article.setInterval(interval);
        article.setEaseFactor(easeFactor);
        article.setReviewTime(currentTime + (interval * 24 * 60 * 60 * 1000L));
    }


    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(progressUpdateReceiver);

    }

}