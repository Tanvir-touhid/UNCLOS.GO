package com.example.user.unclosgo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import com.example.user.unclosgo.databinding.ActivityStudyBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StudyActivity extends BaseActivity {
    private List<Flashcard> studyQueue = new ArrayList<>();
    private int currentIndex = 0;
    private Flashcard currentCard;
    private FlashcardUpdateReceiver updateReceiver;
    private boolean isReceiverRegistered = false;

    private FlashcardDao flashcardDao;
    private ActivityStudyBinding binding;
    private int newCount = 0;
    private int learningCount = 0;
    private int dueCount = 0;
    private ReviewTimeManager reviewTimeManager;

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setEnterTransition(
                TransitionInflater.from(this).inflateTransition(R.transition.fade)
        );
        setContentView(R.layout.activity_study);
        applyFontSizes();

        binding = ActivityStudyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        reviewTimeManager = new ReviewTimeManager(this);

        initializeButtons();

        FlashcardDatabase db = FlashcardDatabase.getDatabase(getApplicationContext());
        flashcardDao = db.flashcardDao();
        db.getOpenHelper().getWritableDatabase(); // Force fresh database connection

        loadFlashcardsAndCounts();
    }

    private void updateReviewTimes() {
        if (currentCard == null) return;

        long now = System.currentTimeMillis();
        String currentStatus = currentCard.getStatus();
        int goodCount = currentCard.getGoodCountDuringLearning();

        // Calculate times for each button
        long againTime = reviewTimeManager.calculateNextReviewTime("again", now, goodCount, currentStatus);
        long hardTime = reviewTimeManager.calculateNextReviewTime("hard", now, goodCount, currentStatus);
        long easyTime = reviewTimeManager.calculateNextReviewTime("easy", now, goodCount, currentStatus);

        binding.buttonAgain.setText("Again (" + formatReviewTime(againTime - now) + ")");
        binding.buttonHard.setText("Hard (" + formatReviewTime(hardTime - now) + ")");
        binding.buttonEasy.setText("Easy (" + formatReviewTime(easyTime - now) + ")");
    }

    private String formatReviewTime(long millis) {
        if (millis < 60 * 60 * 1000) {
            // Less than 1 hour - show minutes
            return (millis / (60 * 1000)) + "m";
        } else if (millis < 24 * 60 * 60 * 1000) {
            // Less than 1 day - show hours
            return (millis / (60 * 60 * 1000)) + "h";
        } else {
            // Show days
            return (millis / (24 * 60 * 60 * 1000)) + "d";
        }
    }

    private void initializeButtons() {
        binding.textViewAnswer.setVisibility(View.GONE);

        // Hide the layout that contains the answer choices
        binding.choiceButtonsLayout.setVisibility(View.GONE);

        // Disable buttons (optional safety)
        binding.buttonAgain.setEnabled(false);
        binding.buttonHard.setEnabled(false);
        binding.buttonEasy.setEnabled(false);

        binding.buttonShowAnswer.setVisibility(View.VISIBLE);
        binding.buttonNext.setVisibility(View.GONE);
        binding.buttonOkay.setVisibility(View.GONE);

        binding.buttonOkay.setOnClickListener(v -> finish());
        binding.buttonShowAnswer.setOnClickListener(v -> showAnswer());
        binding.buttonNext.setOnClickListener(v -> {
            currentIndex++;
            loadNextCard();
        });
        binding.buttonAgain.setOnClickListener(v -> handleReviewChoice("again"));
        binding.buttonHard.setOnClickListener(v -> handleReviewChoice("hard"));
        binding.buttonEasy.setOnClickListener(v -> handleReviewChoice("easy"));
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerUpdateReceiver();
        loadCounts();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterUpdateReceiver();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerUpdateReceiver() {
        if (!isReceiverRegistered) {
            updateReceiver = new FlashcardUpdateReceiver(this::loadCounts);
            IntentFilter filter = new IntentFilter(AppConstants.FLASHCARD_UPDATE_ACTION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(updateReceiver, filter);
            }

            isReceiverRegistered = true;
        }
    }

    private void unregisterUpdateReceiver() {
        if (isReceiverRegistered) {
            unregisterReceiver(updateReceiver);
            isReceiverRegistered = false;
        }
    }

    private void loadFlashcardsAndCounts() {
        executor.execute(() -> {


            long now = System.currentTimeMillis();

            // Get fresh counts using the correct DAO methods
            newCount = flashcardDao.countNewCardsReady(now);
            learningCount = flashcardDao.countLearningCards(now);
            dueCount = flashcardDao.countDueCards(now);

            // Build study queue
            Set<Integer> seenIds = new HashSet<>();
            List<Flashcard> allToStudy = new ArrayList<>();

            // Use the correct query methods
            addCardsToQueue(flashcardDao.getNewFlashcardsReadyForReview(now), seenIds, allToStudy);
            addCardsToQueue(flashcardDao.getDueLearningFlashcards(now), seenIds, allToStudy);
            addCardsToQueue(flashcardDao.getDueReviewFlashcards(now), seenIds, allToStudy);

            studyQueue = allToStudy;
            currentIndex = 0;

            runOnUiThread(() -> {
                updateCountDisplay();
                if (studyQueue.isEmpty()) {
                    showNoCardsUI();
                } else {
                    loadNextCard();
                }
            });
        });
    }

    private void loadCounts() {
        executor.execute(() -> {
            long now = System.currentTimeMillis();

            // Use counting methods instead of getting lists
            int freshNewCount = flashcardDao.countNewCardsReady(now);
            int freshLearningCount = flashcardDao.countLearningCards(now);
            int freshDueCount = flashcardDao.countDueCards(now);

            runOnUiThread(() -> {
                newCount = freshNewCount;
                learningCount = freshLearningCount;
                dueCount = freshDueCount;

                updateCountDisplay();

                if (studyQueue.isEmpty()) {
                    loadFlashcardsAndCounts();
                }
            });
        });
    }

    private void updateCountDisplay() {
        binding.textViewNewCount.setText("N: " + newCount);
        binding.textViewLearningCount.setText("L: " + learningCount);
        binding.textViewDueCount.setText("D: " + dueCount);
    }

    private void addCardsToQueue(List<Flashcard> cards, Set<Integer> seenIds, List<Flashcard> queue) {
        for (Flashcard card : cards) {
            if (seenIds.add(card.getId())) {
                queue.add(card);
            }
        }
    }

    private void showNoCardsUI() {
        // Hide all existing views
        binding.textViewQuestion.setVisibility(View.GONE);
        binding.textViewAnswer.setVisibility(View.GONE);
        binding.buttonShowAnswer.setVisibility(View.GONE);
        binding.buttonNext.setVisibility(View.GONE);
        binding.buttonAgain.setVisibility(View.GONE);
        binding.buttonHard.setVisibility(View.GONE);
        binding.buttonEasy.setVisibility(View.GONE);
        binding.buttonOkay.setVisibility(View.GONE);
        binding.choiceButtonsLayout.setVisibility(View.GONE);
        binding.textViewNewCount.setVisibility(View.GONE);
        binding.textViewLearningCount.setVisibility(View.GONE);
        binding.textViewDueCount.setVisibility(View.GONE);

        // Inflate and show the no cards layout
        View noCardsView = getLayoutInflater().inflate(R.layout.no_cards_layout, binding.getRoot(), false);
        binding.getRoot().addView(noCardsView);

        // Set up the Done button
        Button doneButton = noCardsView.findViewById(R.id.button_done);
        doneButton.setOnClickListener(v -> {
            Intent intent = new Intent(StudyActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Finish the current activity to prevent going back to it
        });
    }

    private void showAnswer() {
        if (currentCard != null) {
            // Mark card as seen
            ReviewTimeManager.markCardAsSeen(currentCard);

            binding.textViewRecallHint.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> binding.textViewRecallHint.setVisibility(View.GONE))
                    .start();

            // Set the answer text but keep it invisible initially
            binding.textViewAnswer.setText(currentCard.getContent());
            binding.textViewAnswer.setAlpha(0f);
            binding.textViewAnswer.setVisibility(View.VISIBLE);

            // Prepare the choice buttons layout
            binding.choiceButtonsLayout.setAlpha(0f);
            binding.choiceButtonsLayout.setVisibility(View.VISIBLE);
            binding.choiceButtonsLayout.setTranslationY(50f);

            // Create a reveal animation sequence
            binding.textViewAnswer.animate()
                    .alpha(1f)
                    .setDuration(2000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .withStartAction(() -> {
                        // Optional: Add a subtle scale animation to the question
                        binding.textViewQuestion.animate()
                                .scaleX(0.98f)
                                .scaleY(0.98f)
                                .setDuration(200)
                                .start();
                    })
                    .withEndAction(() -> {
                        // Animate in the choice buttons
                        binding.choiceButtonsLayout.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(250)
                                .setInterpolator(new OvershootInterpolator(0.5f))
                                .start();

                        binding.buttonAgain.setVisibility(View.VISIBLE);
                        binding.buttonHard.setVisibility(View.VISIBLE);
                        binding.buttonEasy.setVisibility(View.VISIBLE);

                        binding.buttonAgain.setEnabled(true);
                        binding.buttonHard.setEnabled(true);
                        binding.buttonEasy.setEnabled(true);


                        // Enable buttons
                        binding.buttonAgain.setEnabled(true);
                        binding.buttonHard.setEnabled(true);
                        binding.buttonEasy.setEnabled(true);
                    })
                    .start();

            // Hide the show answer button with a fade out
            binding.buttonShowAnswer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction(() -> binding.buttonShowAnswer.setVisibility(View.GONE))
                    .start();

            updateReviewTimes();
        }
    }


    private void loadNextCard() {
        if (currentIndex >= studyQueue.size()) {
            showSessionCompleted();
            return;
        }

        currentCard = studyQueue.get(currentIndex);

        binding.textViewRecallHint.setVisibility(View.VISIBLE);
        binding.textViewRecallHint.setAlpha(1f);
        Log.d("FlashcardState", "Loaded Flashcard: " + currentCard.toString());

        binding.textViewQuestion.setText(currentCard.getTitle());
        binding.textViewAnswer.setVisibility(View.GONE);
        binding.textViewAnswer.setText("");
        updateReviewTimes();

        // Add this to show the current status
        updateStatusDisplay(currentCard.getStatus());

        binding.buttonShowAnswer.setVisibility(View.VISIBLE);
        binding.buttonShowAnswer.setEnabled(true);

        // 🔧 Hide these until Show Answer is clicked
        binding.buttonAgain.setVisibility(View.GONE);
        binding.buttonHard.setVisibility(View.GONE);
        binding.buttonEasy.setVisibility(View.GONE);

        binding.buttonNext.setVisibility(View.GONE);
        binding.buttonNext.setEnabled(false);

        binding.textViewAnswer.setAlpha(1f);
        binding.choiceButtonsLayout.setAlpha(1f);
        binding.choiceButtonsLayout.setTranslationY(0f);
        binding.textViewQuestion.setScaleX(1f);
        binding.textViewQuestion.setScaleY(1f);
        binding.buttonShowAnswer.setAlpha(1f);
    }

    private void updateStatusDisplay(String status) {
        // Reset all status text views first
        binding.textViewNewCount.setPaintFlags(binding.textViewNewCount.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        binding.textViewLearningCount.setPaintFlags(binding.textViewLearningCount.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
        binding.textViewDueCount.setPaintFlags(binding.textViewDueCount.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));

        // Underline the current status
        switch (status.toLowerCase()) {
            case "new":
                binding.textViewNewCount.setPaintFlags(binding.textViewNewCount.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                break;
            case "learning":
                binding.textViewLearningCount.setPaintFlags(binding.textViewLearningCount.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                break;
            case "review":
                binding.textViewDueCount.setPaintFlags(binding.textViewDueCount.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
                break;
        }
    }


    private void showSessionCompleted() {
        // Hide all existing views
        //binding.textViewQuestion.setVisibility(View.GONE);
        //binding.textViewAnswer.setVisibility(View.GONE);
        //binding.buttonShowAnswer.setVisibility(View.GONE);
        //binding.buttonNext.setVisibility(View.GONE);
        //binding.buttonAgain.setVisibility(View.GONE);
        //binding.buttonHard.setVisibility(View.GONE);
        //binding.buttonEasy.setVisibility(View.GONE);
        //binding.buttonOkay.setVisibility(View.GONE);
        //binding.choiceButtonsLayout.setVisibility(View.GONE);

        // Inflate and show the congratulatory layout
        //binding.textViewNewCount.setVisibility(View.GONE);
        //binding.textViewLearningCount.setVisibility(View.GONE);
        //binding.textViewDueCount.setVisibility(View.GONE);
        binding.getRoot().removeAllViews();
        View congratsView = getLayoutInflater().inflate(R.layout.congrats_layout, binding.getRoot(), false);
        binding.getRoot().addView(congratsView);

        // Update counts display (optional)
        updateCountDisplay();

        // Set up the Done button
        Button doneButton = congratsView.findViewById(R.id.button_done);
        doneButton.setOnClickListener(v -> {
            // Clear the back stack and return to MainActivity
            Intent intent = new Intent(StudyActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });


    }

    private void handleReviewChoice(String reviewChoice) {
        if (currentIndex >= studyQueue.size()) return;

        Flashcard current = studyQueue.get(currentIndex);

        // Let ReviewTimeManager handle review timing and status
        ReviewTimeManager reviewManager = new ReviewTimeManager(this); // context = your activity/application context
        reviewManager.handleReview(current, reviewChoice);

        // Update the status display
        updateStatusDisplay(current.getStatus());

        executor.execute(() -> {
            try {
                flashcardDao.update(current);


                long now = System.currentTimeMillis();
                // Use counting methods instead of getting lists
                newCount = flashcardDao.countNewCardsReady(now);
                learningCount = flashcardDao.countLearningCards(now);
                dueCount = flashcardDao.countDueCards(now);

                runOnUiThread(() -> {
                    updateCountDisplay();
                    sendUpdateBroadcast();

                    currentIndex++;
                    if (currentIndex >= studyQueue.size()) {
                        showSessionCompleted();
                    } else {
                        loadNextCard();
                    }
                });
            } catch (Exception e) {
                Log.e("FlashcardUpdate", "Error updating flashcard", e);
            }
        });
    }

    private void sendUpdateBroadcast() {
        Intent updateIntent = new Intent(AppConstants.FLASHCARD_UPDATE_ACTION);
        updateIntent.setPackage(getPackageName());

        sendBroadcast(updateIntent);

        Log.d("Study", "Sent flashcard update broadcast");
    }
}
