package com.example.user.unclosgo;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.preference.PreferenceManager;

import com.google.android.material.navigation.NavigationView;



public class MainActivity extends BaseActivity {

    // UI Components
    //https://play.google.com/store/apps/details?id=com.unclos.go&pcampaignid=web_share

    private ProgressBar articleProgress, masteredProgress, pendingProgress;
    private TextView articleProgressText, percentageArticles;
    private TextView masteredProgressText, percentageMastered;
    private TextView pendingProgressText, percentagePending;
    private ImageView infoArticles;
    private ImageView infoMastered;
    private ImageView infoPending;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private CardView flashcardsButton;
    private ActionBarDrawerToggle drawerToggle;
    private View pendingIndicatorDot;



    // Data
    // In MainActivity.java
    private final BroadcastReceiver progressUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            switch (intent.getAction()) {
                case "ARTICLE_PROGRESS_UPDATED":
                    updateArticlesProgress();
                    break;

                case AppConstants.FLASHCARD_UPDATE_ACTION:
                    updateFlashcardsProgress();
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        updateArticlesProgress();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Theme setup
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? MODE_NIGHT_YES : MODE_NIGHT_NO);

        getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);

        super.onCreate(savedInstanceState);

        PrefManager prefManager = new PrefManager(this);
        if (prefManager.isFirstTimeLaunch()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        // 🔧 Register broadcast receiver for updates
        IntentFilter filter = new IntentFilter();
        filter.addAction("ARTICLE_PROGRESS_UPDATED");          // <- plain string
        filter.addAction(AppConstants.FLASHCARD_UPDATE_ACTION);
        registerReceiver(progressUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);// ✅ <-- THIS IS THE FIX

        applyFontSizes();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    showExitDialog();
                }
            }
        });

        // Flashcards button click
        findViewById(R.id.button_flashcards).setOnClickListener(v -> {
            pendingIndicatorDot.setVisibility(View.VISIBLE);
            startActivity(new Intent(MainActivity.this, FlashcardsActivity.class));
            new Handler().postDelayed(() -> {
                Intent updateIntent = new Intent(AppConstants.FLASHCARD_UPDATE_ACTION);
                updateIntent.setPackage(getPackageName());
                sendBroadcast(updateIntent);
            }, 2000);
        });

        // Force theme redraw
        getWindow().getDecorView().post(() -> getWindow().getDecorView().invalidate());

        // Initialize views
        setupViews();
        setupToolbar();
        setupDrawer();
        setupButtonListeners();

        // Initial data load
        updateAllProgressData();
    }


    private void setupViews() {
        // Articles Read
        infoArticles = findViewById(R.id.infoArticles);
        articleProgress = findViewById(R.id.articleProgress);
        articleProgressText = findViewById(R.id.articleProgressText);
        percentageArticles = findViewById(R.id.percentageArticles);
        LinearLayout AR = findViewById(R.id.a);
        LinearLayout CM = findViewById(R.id.b);
        LinearLayout RP = findViewById(R.id.c);
        RelativeLayout PF = findViewById(R.id.d);

        // Cards Mastered
        infoMastered = findViewById(R.id.infoMastered);
        masteredProgress = findViewById(R.id.masteredProgress);
        masteredProgressText = findViewById(R.id.masteredProgressText);
        percentageMastered = findViewById(R.id.percentageMastered);


        // Pending Review
        infoPending = findViewById(R.id.infoPending);
        pendingProgress = findViewById(R.id.pendingProgress);
        pendingProgressText = findViewById(R.id.pendingProgressText);
        percentagePending = findViewById(R.id.percentagePending);
        flashcardsButton = findViewById(R.id.button_flashcards);


        // Set click listeners
        infoArticles.setOnClickListener(v -> showArticlesInfoDialog());
        infoMastered.setOnClickListener(v -> showMasteredInfoDialog());
        infoPending.setOnClickListener(v -> showPendingInfoDialog());
        AR.setOnClickListener(v -> showArticlesInfoDialog());
        CM.setOnClickListener(v -> showMasteredInfoDialog());
        RP.setOnClickListener(v -> showPendingInfoDialog());

        // Set click listener for feedback info button
        ImageView infoFeedback = findViewById(R.id.infoFeedback);
        infoFeedback.setOnClickListener(v -> showFeedbackInfoDialog());
        PF.setOnClickListener(v -> showFeedbackInfoDialog());
        pendingIndicatorDot = findViewById(R.id.ic_flashcards);
    }

    private void showFeedbackInfoDialog() {
        SpannableString styledTitle = new SpannableString("Progress Feedback");
        styledTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, styledTitle.length(), 0);
        styledTitle.setSpan(new AbsoluteSizeSpan(20, true), 0, styledTitle.length(), 0); // 20sp

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(styledTitle)
                .setMessage("This section provides personalized recommendations based on your reading and flashcard progress.")
                .setPositiveButton("OK", null)
                .create();

        dialog.show();

        // Set rounded background programmatically
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(this, R.color.article_reading_background)); // You can also use theme color
            background.setCornerRadius(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics())
            );
            background.setStroke(1, Color.LTGRAY); // Optional border

            window.setBackgroundDrawable(background);

            // Optional: Resize dialog if desired
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85); // 85% width
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Now safely customize the positive button
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            int color = ContextCompat.getColor(this, R.color.textSecondary);
            positiveButton.setTextColor(color);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(progressUpdateReceiver);
        } catch (IllegalArgumentException e) {
            // Receiver was not registered, ignore
        }
    }

    // In your MainActivity.java
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        CardView readingCard = findViewById(R.id.startReadingButton);
        CardView flashcardCard = findViewById(R.id.button_flashcards);

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape adjustments
            ViewGroup.LayoutParams params = readingCard.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 58, getResources().getDisplayMetrics());
            readingCard.setLayoutParams(params);
            flashcardCard.setLayoutParams(params);

            // You can also adjust other properties here
            readingCard.setRadius(20);
            flashcardCard.setRadius(20);
        } else {
            // Portrait defaults
            ViewGroup.LayoutParams params = readingCard.getLayoutParams();
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, getResources().getDisplayMetrics());
            readingCard.setLayoutParams(params);
            flashcardCard.setLayoutParams(params);

            readingCard.setRadius(20);
            flashcardCard.setRadius(20);
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // Enable the home button (for hamburger icon)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void setupButtonListeners() {
        findViewById(R.id.startReadingButton).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ReadActivity.class));
        });

        // In the activity you're transitioning FROM (e.g., MainActivity)
        flashcardsButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, FlashcardsActivity.class));
        });
    }


    private void updateAllProgressData() {
        updateArticlesProgress();
        updateFlashcardsProgress();
        updateProgressFeedback();
    }

    private void updateProgressFeedback() {
        int articlesRead = calculateArticlesRead();
        int cardsMastered = masteredProgress.getProgress();

        runOnUiThread(() -> {
            TextView feedbackText = findViewById(R.id.progressFeedbackText);
            feedbackText.setText(ProgressFeedback.getUnifiedFeedback(
                    articlesRead,
                    320,
                    cardsMastered,
                    masteredProgress.getMax()
            ));
        });
    }


    private int calculateArticlesRead() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int count = 0;
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("read_") && prefs.getBoolean(key, false)) {
                count++;
            }
        }
        return count;
    }
    private void showExitDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialogInterface, which) -> {
                    finishAffinity();
                    System.exit(0);
                })
                .setNegativeButton("No", null)
                .setIcon(R.drawable.ida)
                .create();

        dialog.show();

        // Customize dialog background with rounded corners
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(this, R.color.article_reading_background));
            background.setCornerRadius(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics())
            );
            background.setStroke(1, Color.LTGRAY);

            window.setBackgroundDrawable(background);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set button colors after showing dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
    }



    private void updateArticlesProgress() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int totalArticles = 320; // Update this if dynamic

        int count = 0;
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("read_") && prefs.getBoolean(key, false)) {
                count++;
            }
        }
        int articlesRead = count;
        int percentage = (int) (((float) articlesRead / totalArticles) * 100);

        runOnUiThread(() -> {
            articleProgress.setMax(totalArticles);
            articleProgress.setProgress(articlesRead);
            articleProgressText.setText(articlesRead + " of " + totalArticles);
            percentageArticles.setText(percentage + "%");

            updateProgressFeedback();
        });
    }
    interface StatsUpdater {
        void update(CombinedFlashcardStats stats, int count);
    }

    private void updateFlashcardsProgress() {
        runOnUiThread(() -> {
            masteredProgress.setProgress(0);
            masteredProgressText.setText("0 of 0");
            percentageMastered.setText("0%");

            pendingProgress.setProgress(0);
            pendingProgressText.setText("0 of 0");
            percentagePending.setText("0%");
        });
        FlashcardDao dao = FlashcardDatabase.getDatabase(this).flashcardDao();
        long currentTime = System.currentTimeMillis();

        final MediatorLiveData<CombinedFlashcardStats> combinedStats = new MediatorLiveData<>();
        CombinedFlashcardStats initialStats = new CombinedFlashcardStats();
        initialStats.isInitialLoad = true;  // Add this flag to your CombinedFlashcardStats class
        combinedStats.setValue(initialStats);

        // Add all LiveData sources
        LiveData<Integer> newCardsLive = dao.countNewCardsReadyLive(currentTime);
        LiveData<Integer> dueLearningLive = dao.countDueLearningCards(currentTime);
        LiveData<Integer> totalReviewLive = dao.countTotalReviewCards();
        LiveData<Integer> dueReviewLive = dao.countDueReviewCards(currentTime);

        combinedStats.addSource(newCardsLive, count -> {
            CombinedFlashcardStats current = combinedStats.getValue();
            if (current != null) {
                current.readyNewCount = count != null ? count : 0;
                current.isInitialLoad = false;
                combinedStats.setValue(current);
            }
        });

        combinedStats.addSource(dueLearningLive, count -> {
            CombinedFlashcardStats current = combinedStats.getValue();
            if (current != null) {
                current.dueLearningCount = count != null ? count : 0;
                current.isInitialLoad = false;
                combinedStats.setValue(current);
            }
        });

        combinedStats.addSource(totalReviewLive, count -> {
            CombinedFlashcardStats current = combinedStats.getValue();
            if (current != null) {
                current.totalReviewCount = count != null ? count : 0;
                current.isInitialLoad = false;
                combinedStats.setValue(current);
            }
        });

        combinedStats.addSource(dueReviewLive, count -> {
            CombinedFlashcardStats current = combinedStats.getValue();
            if (current != null) {
                current.dueReviewCount = count != null ? count : 0;
                current.isInitialLoad = false;
                combinedStats.setValue(current);
            }
        });

        combinedStats.observe(this, stats -> {
            if (stats != null && !stats.isInitialLoad) {
                runOnUiThread(() -> {
                    // Calculate total cards (new + learning + review)
                    int totalCards = stats.readyNewCount + stats.dueLearningCount + stats.totalReviewCount;

                    // Calculate mastered cards (cards in review status that aren't due)
                    int masteredCards = stats.totalReviewCount - stats.dueReviewCount;

                    // Calculate pending cards (new + learning + due review)
                    int pendingCards = stats.readyNewCount + stats.dueLearningCount + stats.dueReviewCount;

                    // Update Mastered Cards UI
                    if (totalCards > 0) {
                        masteredProgress.setMax(totalCards);
                        masteredProgress.setProgress(masteredCards);
                        masteredProgressText.setText(masteredCards + " of " + totalCards);
                        percentageMastered.setText((int)(masteredCards / (float)totalCards * 100) + "%");
                    } else {
                        masteredProgress.setProgress(0);
                        masteredProgressText.setText("0 of 0");
                        percentageMastered.setText("0%");
                    }

                    // Update Pending Cards UI
                    if (totalCards > 0) {
                        pendingProgress.setMax(totalCards);
                        pendingProgress.setProgress(pendingCards);
                        pendingProgressText.setText(pendingCards + " of " + totalCards);
                        percentagePending.setText((int)(pendingCards / (float)totalCards * 100) + "%");
                    } else {
                        pendingProgress.setProgress(0);
                        pendingProgressText.setText("0 of 0");
                        percentagePending.setText("0%");
                    }

                    updateFlashcardIconState(stats);
                });
            }
        });
    }

    private void updateFlashcardIconState(CombinedFlashcardStats stats) {
        boolean hasPendingCards = (stats.dueReviewCount > 0 ||
                stats.dueLearningCount > 0 ||
                stats.readyNewCount > 0);

        runOnUiThread(() -> {
            if (pendingIndicatorDot != null) {
                if (hasPendingCards) {
                    if (!isDotAnimating()) {
                        startDotBlink();
                    }
                } else {
                    stopDotBlink();
                }
            }
        });
    }

    private boolean isDotAnimating() {
        return pendingIndicatorDot != null &&
                pendingIndicatorDot.getAnimation() != null;
    }

    // Helper class to combine all stats
    private static class CombinedFlashcardStats {
        int readyNewCount = 0;       // New cards ready for first review
        int dueLearningCount = 0;    // Learning cards due for review
        int dueReviewCount = 0;      // Review cards due
        int totalReviewCount = 0;    // All cards in review status

        boolean isInitialLoad = true;
    }

    private void showArticlesInfoDialog() {
        SpannableString styledTitle = new SpannableString("Articles Read");
        styledTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, styledTitle.length(), 0);
        styledTitle.setSpan(new AbsoluteSizeSpan(20, true), 0, styledTitle.length(), 0); // 20sp

        // Step 1: Create the dialog
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle(styledTitle)
                .setMessage("The number of articles you marked as read out of 320.")
                .setPositiveButton("OK", null)
                .create();
        // Step 2: Show the dialog first
        dialog.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            int color = ContextCompat.getColor(this, R.color.textSecondary); // resolve color
            positiveButton.setTextColor(color);
        }

        // Step 3: Resize the dialog using WindowManager
        Window window = dialog.getWindow();
        if (window != null) {
            // Create a GradientDrawable with rounded corners
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(this, R.color.article_reading_background)); // Or use a theme color
            background.setCornerRadius(32f); // Set radius in pixels (e.g., 32px = ~16dp on mdpi)
            background.setStroke(1, Color.LTGRAY); // Optional border

            window.setBackgroundDrawable(background);

            // Resize the dialog
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());

            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

            window.setAttributes(layoutParams);
        }
    }
    private void showMasteredInfoDialog() {
        try {
            String masteredText = masteredProgressText.getText().toString();
            String[] parts = masteredText.split(" of ");
            int masteredCount = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int totalCount = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int dueCount = totalCount - masteredCount;

            String message = String.format(
                    "You have mastered %d cards (%d remaining to review) out of %d total cards in your collection.",
                    masteredCount, dueCount, totalCount
            );

            SpannableString styledTitle = new SpannableString("Cards Mastered");
            styledTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, styledTitle.length(), 0);
            styledTitle.setSpan(new AbsoluteSizeSpan(20, true), 0, styledTitle.length(), 0); // 20sp

            // Create dialog
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle(styledTitle)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .create();

            // Show dialog
            dialog.show();
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                int color = ContextCompat.getColor(this, R.color.textSecondary); // resolve color
                positiveButton.setTextColor(color);
            }

            // Resize the dialog
            Window window = dialog.getWindow();
            if (window != null) {
                // Create a GradientDrawable with rounded corners
                GradientDrawable background = new GradientDrawable();
                background.setColor(ContextCompat.getColor(this, R.color.article_reading_background)); // Or use a theme color
                background.setCornerRadius(32f); // Set radius in pixels (e.g., 32px = ~16dp on mdpi)
                background.setStroke(1, Color.LTGRAY); // Optional border

                window.setBackgroundDrawable(background);

                // Resize the dialog
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(window.getAttributes());

                layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

                window.setAttributes(layoutParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing mastered info", Toast.LENGTH_SHORT).show();
        }
    }



    private void showPendingInfoDialog() {
        try {
            String pendingText = pendingProgressText.getText().toString();
            String[] parts = pendingText.split(" of ");
            int pendingCount = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int totalCount = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

            String message;
            if (pendingCount == 0) {
                message = "You currently have no cards in learning. Great job!";
            } else {
                message = String.format(
                        "You are currently learning %d cards. Keep reviewing them to master the content!",
                        pendingCount
                );
            }

            SpannableString styledTitle = new SpannableString("Pending Review");
            styledTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, styledTitle.length(), 0);
            styledTitle.setSpan(new AbsoluteSizeSpan(20, true), 0, styledTitle.length(), 0); // 20sp

            // Create the dialog
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                    .setTitle(styledTitle)
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .create();

            // Show the dialog
            dialog.show();
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                int color = ContextCompat.getColor(this, R.color.textSecondary); // resolve color
                positiveButton.setTextColor(color);
            }

            // Resize it
            Window window = dialog.getWindow();
            if (window != null) {
                // Create a GradientDrawable with rounded corners
                GradientDrawable background = new GradientDrawable();
                background.setColor(ContextCompat.getColor(this, R.color.article_reading_background)); // Or use a theme color
                background.setCornerRadius(32f); // Set radius in pixels (e.g., 32px = ~16dp on mdpi)
                background.setStroke(1, Color.LTGRAY); // Optional border

                window.setBackgroundDrawable(background);

                // Resize the dialog
                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                layoutParams.copyFrom(window.getAttributes());

                layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

                window.setAttributes(layoutParams);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing pending info", Toast.LENGTH_SHORT).show();
        }
    }
    // Broadcast receiver handling
    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.user.unclosgo.ARTICLE_PROGRESS_UPDATED");
        filter.addAction(AppConstants.FLASHCARD_UPDATE_ACTION);

        // ✅ Lint-safe receiver registration for all API levels
        ContextCompat.registerReceiver(
                this,                        // context
                progressUpdateReceiver,     // receiver
                filter,                     // intent filter
                ContextCompat.RECEIVER_NOT_EXPORTED  // flag for Android 13+, ignored on older
        );
    }



    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(progressUpdateReceiver);
    }

    private View customScrim;

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        customScrim = findViewById(R.id.custom_scrim);

        // Disable default scrim
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        ) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                if (customScrim != null) {
                    customScrim.setVisibility(View.VISIBLE);
                    customScrim.setAlpha(slideOffset);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (customScrim != null) {
                    customScrim.setVisibility(View.GONE);
                }
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.nav_credits) {
                startActivity(new Intent(MainActivity.this, CreditsActivity.class));
            } else if (id == R.id.nav_exit) {
                showExitConfirmationDialog();
            } else if (id == R.id.history_of_unclos) {
                startActivity(new Intent(MainActivity.this, HistoryOfUnclosActivity.class));
            } else if (id == R.id.table_of_content) {
                startActivity(new Intent(MainActivity.this, TableofContents.class));
            } else if (id == R.id.faq) {
                startActivity(new Intent(MainActivity.this, faq.class));
            } else if (id == R.id.rateUs){
                new RateUsHelper(MainActivity.this).handleRateAction();
            } else if (id == R.id.share) {
                new ShareHelper(MainActivity.this).showShareDialog();
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    private void showExitConfirmationDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialogInterface, which) -> {
                    finishAffinity();
                    System.exit(0);
                })
                .setNegativeButton("No", null)
                .setIcon(R.drawable.ida)
                .create();

        dialog.show();

        // Apply rounded corners programmatically
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(this, R.color.article_reading_background));
            // Or use a themed color
            background.setCornerRadius(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics())
            );
            background.setStroke(1, Color.LTGRAY); // Optional border
            window.setBackgroundDrawable(background);

            // Optional: Resize the dialog
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.85);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set button text colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.textSecondary));
    }
    private void startDotBlink() {
        if (pendingIndicatorDot == null) return;

        pendingIndicatorDot.setVisibility(View.VISIBLE);
        pendingIndicatorDot.clearAnimation();
        Animation blinkAnimation = new AlphaAnimation(1, 0.3f);
        blinkAnimation.setDuration(800);
        blinkAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        blinkAnimation.setRepeatCount(Animation.INFINITE);
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        pendingIndicatorDot.startAnimation(blinkAnimation);
    }

    private void stopDotBlink() {
        if (pendingIndicatorDot == null) return;

        Animation currentAnim = pendingIndicatorDot.getAnimation();
        if (currentAnim != null) {
            currentAnim.cancel();
            currentAnim.reset();
        }
        pendingIndicatorDot.clearAnimation();
        pendingIndicatorDot.setAlpha(1f); // Reset to full opacity
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Let the drawer toggle handle the home button click
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        // Handle other menu items if needed
        return super.onOptionsItemSelected(item);
    }
}