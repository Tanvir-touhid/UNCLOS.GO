package com.example.user.unclosgo;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

/**
 * ReviewTimeManager is responsible for managing the review schedule
 * of flashcards based on user interaction (again, hard, easy).
 *
 * Rules:
 * - A flashcard remains "new" until the user reviews it at least once.
 * - After the first review, timing and status updates are applied.
 */
public class ReviewTimeManager {
    private SharedPreferences sharedPreferences;
    private float initialEaseFactor;
    private int graduationEasies;
    private int fuzzPercent;

    // Constructor to initialize the context and load settings
    public ReviewTimeManager(Context context) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Load user preferences
        loadSettings();
    }

    // Load user preferences from SharedPreferences
    private void loadSettings() {
        // Retrieve settings from SharedPreferences
        initialEaseFactor = sharedPreferences.getFloat("initial_ease_factor", 2.5f);  // Default to 2.5 if not set
        graduationEasies = sharedPreferences.getInt("graduation_easies", 2);          // Default to 2 if not set
        fuzzPercent = sharedPreferences.getInt("fuzz_percent", 10);                   // Default to 10% if not set
    }
    //ignore next 6 lines
    /**
     * Updates the flashcard's status and next review time based on user's review choice.
     *
     * @param card          the flashcard to update
     * @param reviewChoice  user's review input ("again", "hard", or "easy")
     */
    public void handleReview(Flashcard card, String reviewChoice) {
        long currentTime = System.currentTimeMillis();
        String currentStatus = card.getStatus();
        int goodCount = card.getGoodCountDuringLearning();

        switch (currentStatus) {
            case "new":
                handleNewCard(card, reviewChoice, currentTime);
                break;

            case "learning":
                handleLearningCard(card, reviewChoice, currentTime, goodCount);
                break;

            case "review":
                handleReviewCard(card, reviewChoice, currentTime);
                break;
        }

        // Update other SRS parameters
        card.setReviewTime(currentTime);
        card.setReviewChoice(reviewChoice);
    }

    private void handleNewCard(Flashcard card, String reviewChoice, long currentTime) {
        card.setStatus("learning");
        switch (reviewChoice) {
            case "again":
                card.setNextReview(currentTime + 10 * 60 * 1000); // 10 min
                card.setGoodCountDuringLearning(0);
                break;
            case "hard":
                card.setNextReview(currentTime + 20 * 60 * 1000); // 20 min
                break;
            case "easy":
                card.setNextReview(currentTime + 2 * 60 * 60 * 1000); // 2 hours
                card.setGoodCountDuringLearning(1);
                break;
        }
    }

    private void handleLearningCard(Flashcard card, String reviewChoice, long currentTime, int goodCount) {
        switch (reviewChoice) {
            case "again":
                card.setNextReview(currentTime + 10 * 60 * 1000); // 10 min
                card.setGoodCountDuringLearning(0); // full reset
                break;

            case "hard":
                card.setNextReview(currentTime + 30 * 60 * 1000); // 30 min
                break;

            case "easy":
                goodCount++;
                if (goodCount >= graduationEasies) { // Use user-defined graduation threshold
                    // ✅ Graduate to review
                    card.setStatus("review");
                    card.setInterval(1); // start review with 1-day interval
                    card.setRepetitions(1);
                    card.setNextReview(currentTime + 24 * 60 * 60 * 1000); // 1 day
                    card.setGoodCountDuringLearning(0); // reset learning count
                } else {
                    card.setNextReview(currentTime + 2 * 60 * 60 * 1000); // 2 hours
                    card.setGoodCountDuringLearning(goodCount);
                }
                break;
        }
    }


    private void handleReviewCard(Flashcard card, String reviewChoice, long currentTime) {
        int interval = card.getInterval(); // in days
        double easeFactor = card.getEaseFactor();
        int repetitions = card.getRepetitions();

        switch (reviewChoice) {
            case "again":
                easeFactor = updateEaseFactor(easeFactor, reviewChoice);
                interval = 1;
                card.setStatus("learning"); // Demote card to learning
                card.setGoodCountDuringLearning(0); // Reset learning steps
                break;
            case "hard":
                easeFactor = updateEaseFactor(easeFactor, reviewChoice);
                interval = Math.max(1, (int) (interval * 1.2));
                repetitions++;
                break;
            case "easy":
                easeFactor = updateEaseFactor(easeFactor, reviewChoice);
                interval = (int) Math.max(1, interval * easeFactor);
                repetitions++;
                break;
        }

        card.setEaseFactor(easeFactor);
        card.setInterval(interval);
        card.setRepetitions(repetitions);
        long baseMillis = interval * 24 * 60 * 60 * 1000L;
        long fuzz = (long) (baseMillis * (fuzzPercent / 100f)); //  Apply fuzz based on user setting

        // Random number between -fuzz and +fuzz
        long offset = (long) (Math.random() * (2 * fuzz)) - fuzz;

        // Apply fuzz to nextReview time
        card.setNextReview(currentTime + baseMillis + offset);

    }


    protected static long calculateNextReviewTime(String rating, long currentTime, int goodCountDuringLearning, String previousStatus) {
        switch (previousStatus) {
            case "new":
                return currentTime + 10 * 60 * 1000; // Default 10 min for new cards

            case "learning":
                if ("again".equals(rating)) {
                    return currentTime + 10 * 60 * 1000; // 10 min
                } else if ("hard".equals(rating)) {
                    return currentTime + 30 * 60 * 1000; // 30 min
                } else {
                    goodCountDuringLearning++;
                    if (goodCountDuringLearning >= 2) {
                        return currentTime + 24 * 60 * 60 * 1000; // 1 day
                    }
                    return currentTime + 2 * 60 * 60 * 1000; // 2 hours
                }

            case "review":
                if ("again".equals(rating)) {
                    return currentTime + 10 * 60 * 1000; // 10 min
                } else if ("hard".equals(rating)) {
                    return currentTime + 3 * 24 * 60 * 60 * 1000; // 3 days
                } else {
                    return currentTime + 5 * 24 * 60 * 60 * 1000; // 5 days
                }

            default:
                return currentTime + 24 * 60 * 60 * 1000; // Default 1 day
        }
    }

    /**
     * Marks a card as "seen" when the user first interacts with it.
     */
    public static void markCardAsSeen(Flashcard card) {
        if ("new".equals(card.getStatus())) {
            card.setStatus("learning");
            card.setNextReview(System.currentTimeMillis() + 10 * 60 * 1000); // 10 minutes
            card.setGoodCountDuringLearning(0);
        }
    }

    private static double updateEaseFactor(double currentEase, String rating) {
        switch (rating) {
            case "again":
                return Math.max(1.3, currentEase - 0.2);
            case "hard":
                return Math.max(1.3, currentEase - 0.15);
            case "easy":
                return currentEase + 0.1;
            default:
                return currentEase;
        }
    }

}