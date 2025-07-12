package com.example.user.unclosgo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FlashcardDetailActivity extends BaseActivity {

    private TextView titleTextView, contentTextView;
    private Button showButton, againButton, hardButton, easyButton, addToFlashcardsButton;
    private String articleTitle, articleContent;
    private Flashcard flashcard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard_detail);

        titleTextView = findViewById(R.id.text_view_title);
        contentTextView = findViewById(R.id.text_view_content);

        // Check if we got title and content directly
        if (getIntent().hasExtra("title") && getIntent().hasExtra("content")) {
            // Simple display mode from AllFlashcardsActivity
            String title = getIntent().getStringExtra("title");
            String content = getIntent().getStringExtra("content");

            titleTextView.setText(title);

            contentTextView.setText(content);
        }
        else if (getIntent().hasExtra("flashcardId")) {
            // Original mode with database access (kept for compatibility)
            int flashcardId = getIntent().getIntExtra("flashcardId", -1);
            if (flashcardId == -1) {
                Toast.makeText(this, "No flashcard ID passed!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                Flashcard flashcard = FlashcardDatabase.getDatabase(this)
                        .flashcardDao()
                        .getFlashcardById(flashcardId);

                runOnUiThread(() -> {
                    if (flashcard == null) {
                        Toast.makeText(this, "Flashcard not found!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        titleTextView.setText(flashcard.getTitle());
                        contentTextView.setText(flashcard.getContent());
                    }
                });
            });
        }
        else {
            Toast.makeText(this, "No flashcard data provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupUI() {
        titleTextView = findViewById(R.id.text_view_title);
        contentTextView = findViewById(R.id.text_view_content);

        titleTextView.setText(articleTitle);
        contentTextView.setText(articleContent);
        contentTextView.setVisibility(View.GONE);

        showButton.setOnClickListener(v -> contentTextView.setVisibility(View.VISIBLE));

        againButton.setOnClickListener(v -> {Log.d("ButtonTest", "Again button clicked");
            saveReview("again");
        });
        hardButton.setOnClickListener(v -> saveReview("hard"));
        easyButton.setOnClickListener(v -> saveReview("easy"));

        addToFlashcardsButton.setOnClickListener(v -> {
            Toast.makeText(this, "Already in flashcards!", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveReview(String rating) {
        Log.d("ReviewDebug", "saveReview called with rating: " + rating);

        if (flashcard == null) return;

        long now = System.currentTimeMillis();
        String previousStatus = flashcard.getStatus();

        // Use ReviewTimeManager to calculate the next review time
        long nextReviewTime = ReviewTimeManager.calculateNextReviewTime(rating, now, flashcard.getGoodCountDuringLearning(), previousStatus);

        flashcard.setReviewChoice(rating);
        flashcard.setReviewTime(now);
        flashcard.setNextReview(nextReviewTime);

        switch (rating) {
            case "again":
                flashcard.setStatus("learning");
                flashcard.setGoodCountDuringLearning(0);
                break;
            case "hard":
                flashcard.setStatus("learning");
                break;
            case "easy":
                if ("learning".equals(previousStatus)) {
                    int goodCount = flashcard.getGoodCountDuringLearning() + 1;
                    flashcard.setGoodCountDuringLearning(goodCount);

                    if (goodCount >= 2) {
                        flashcard.setStatus("review");
                    }
                } else {
                    flashcard.setStatus("learning");
                    flashcard.setGoodCountDuringLearning(1);
                }
                break;
        }

        // Calculate time until next review in hours
        long timeDiffMillis = nextReviewTime - now;
        long timeDiffHours = TimeUnit.MILLISECONDS.toHours(timeDiffMillis);
        String timeMessage = "You'll review this flashcard again in about " + timeDiffHours + " hour" + (timeDiffHours != 1 ? "s" : "") + ".";

        Executors.newSingleThreadExecutor().execute(() -> {
            FlashcardDatabase db = FlashcardDatabase.getDatabase(getApplicationContext());
            db.flashcardDao().update(flashcard);

            // Send broadcast to update other activities
            Intent updateIntent = new Intent(AppConstants.FLASHCARD_UPDATE_ACTION);
            updateIntent.setPackage(getPackageName());
            sendBroadcast(updateIntent);

            runOnUiThread(() -> {
                Log.d("ReviewDebug", "Reached runOnUiThread: " + timeMessage);

                String combinedMessage = "Review saved.\n" + timeMessage;
                Toast.makeText(this, combinedMessage, Toast.LENGTH_LONG).show();

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updated", true);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }, 2500); // enough time for LONG toast
            });
        });
    }

}
