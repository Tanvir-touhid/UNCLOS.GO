package com.example.user.unclosgo;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.google.android.material.button.MaterialButton;

public class FlashcardsActivity extends BaseActivity {
    private FlashcardDatabase db;
    private FlashcardDao flashcardDao;

    // LiveData declarations
    private LiveData<Integer> newCountLiveData;
    private LiveData<Integer> dueLearningCountLiveData;
    private LiveData<Integer> totalLearningCountLiveData;
    private LiveData<Integer> dueReviewCountLiveData;
    private MediatorLiveData<CombinedStats> combinedStats;

    // Current counts
    private int currentNewCount = 0;
    private int currentDueLearningCount = 0;
    private int currentTotalLearningCount = 0;
    private int currentDueReviewCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        // Initialize database
        db = FlashcardDatabase.getDatabase(getApplicationContext());
        flashcardDao = db.flashcardDao();

        // Initialize all the TextViews
        TextView newCountView = findViewById(R.id.text_new_count);
        TextView learningCountView = findViewById(R.id.text_learning_count);
        TextView reviewCountView = findViewById(R.id.text_review_count);

        // Set loading state
        newCountView.setText("New: Loading...");
        learningCountView.setText("Learning: Loading...");
        reviewCountView.setText("Review: Loading...");

        // Initialize LiveData sources
        long currentTime = System.currentTimeMillis();
        newCountLiveData = flashcardDao.countNewCardsLive(currentTime);
        dueLearningCountLiveData = flashcardDao.countDueLearningCards(currentTime);
        totalLearningCountLiveData = flashcardDao.countTotalLearningCards();
        dueReviewCountLiveData = flashcardDao.countDueReviewCards(currentTime);

        // Setup combined stats observer
        combinedStats = new MediatorLiveData<>();
        setupCountObservers();

        // Observe the combined stats
        combinedStats.observe(this, stats -> {
            if (stats != null) {
                updateStatsDisplay();
            }
        });

        Button allFlashcardsButton = findViewById(R.id.button_all_flashcards);
        allFlashcardsButton.setOnClickListener(v -> {
            Intent intent = new Intent(FlashcardsActivity.this, AllFlashcardsActivity.class);
            startActivity(intent);
        });

        // In your onCreate method
        MaterialButton studyNowButton = findViewById(R.id.button_study_now);
        studyNowButton.setOnClickListener(v -> {
            Intent intent = new Intent(FlashcardsActivity.this, StudyActivity.class);

            // Configure the transition options for API 21+
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                    FlashcardsActivity.this,
                    studyNowButton,  // The view that starts the transition
                    "shared_button"   // The transitionName defined in XML
            );

            // Start the activity with transition
            startActivity(intent, options.toBundle());

            // Optional: Add a short delay to ensure smooth animation
            new Handler().postDelayed(() -> {
                // Any post-transition operations
            }, 350); // Matches the transition duration
        });

    }

    private void setupCountObservers() {
        // New cards observer
        newCountLiveData.observe(this, count -> {
            if (count != null) {
                currentNewCount = count;
                combinedStats.postValue(new CombinedStats());
            }
        });

        // Learning cards observers
        dueLearningCountLiveData.observe(this, count -> {
            if (count != null) {
                currentDueLearningCount = count;
                combinedStats.postValue(new CombinedStats());
            }
        });

        totalLearningCountLiveData.observe(this, count -> {
            if (count != null) {
                currentTotalLearningCount = count;
                combinedStats.postValue(new CombinedStats());
            }
        });

        // Due review cards observer
        dueReviewCountLiveData.observe(this, count -> {
            if (count != null) {
                currentDueReviewCount = count;
                combinedStats.postValue(new CombinedStats());
            }
        });
    }

    private void updateStatsDisplay() {
        TextView newCountView = findViewById(R.id.text_new_count);
        TextView learningCountView = findViewById(R.id.text_learning_count);
        TextView reviewCountView = findViewById(R.id.text_review_count);

        newCountView.setText(String.format("New: %d", currentNewCount));
        learningCountView.setText(String.format("Learning: %d/%d", currentDueLearningCount, currentTotalLearningCount));
        reviewCountView.setText(String.format("Due: %d", currentDueReviewCount));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, "Flashcard updated", Toast.LENGTH_SHORT).show();
        }
    }

    // Simple marker class for triggering updates
    private static class CombinedStats {
        // No data needed, just triggers the observer
    }

    // Data class for combined stats (can be removed if not used elsewhere)
    private static class FlashcardStats {
        final int newCount;
        final int learningCount;
        final int reviewCount;

        FlashcardStats(int newCount, int learningCount, int reviewCount) {
            this.newCount = newCount;
            this.learningCount = learningCount;
            this.reviewCount = reviewCount;
        }
    }
}
