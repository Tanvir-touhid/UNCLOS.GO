package com.example.user.unclosgo;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ArticleDetailActivity extends BaseActivity {

    private TextView titleTextView, contentTextView;

    private LinearLayout actionBar;
    private ScrollView contentScrollView;
    private boolean isActionBarVisible = false;
    private Handler autoHideHandler = new Handler();
    private static final long AUTO_HIDE_DELAY = 5000; // 3 seconds
    private Runnable autoHideRunnable = this::hideActionBar;
    private GestureDetector gestureDetector;

    private ImageView markReadIcon;
    private TextView markReadText;
    private ImageView addFlashcardIcon;

    private TextView addFlashcardText;
    private final Flashcard[] currentFlashcard = {null}; // Added declaration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        // Initialize views
        // Inside onCreate(), after initializing actionBar:
        markReadIcon = findViewById(R.id.markAsReadContainer).findViewById(R.id.markReadIcon);
        markReadText = findViewById(R.id.markAsReadContainer).findViewById(R.id.markReadText);
        addFlashcardIcon = findViewById(R.id.addToFlashcardsContainer).findViewById(R.id.addFlashcardIcon);
        addFlashcardText = findViewById(R.id.addToFlashcardsContainer).findViewById(R.id.addFlashcardText);
        titleTextView = findViewById(R.id.detailTitle);
        contentTextView = findViewById(R.id.detailContent);
        actionBar = findViewById(R.id.actionBar);
        contentScrollView = findViewById(R.id.contentScrollView);
        markReadText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10); // 14sp
        addFlashcardText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10); // 14sp

        TextView viewExplanationText = findViewById(R.id.viewExplanationContainer).findViewById(R.id.explanationTextView);
        viewExplanationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10); // 14sp


        setupGestureDetector();
        setupTouchHandling();

        String title = getIntent().getStringExtra("title") == null ? "No Title" : getIntent().getStringExtra("title");
        String content = getIntent().getStringExtra("content") == null ? "No Content" : getIntent().getStringExtra("content");
        new Thread(() -> {
            FlashcardDao dao = FlashcardDatabase.getDatabase(this).flashcardDao();
            Flashcard existingCard = dao.getFlashcardByTitle(title);
            runOnUiThread(() -> {
                if (existingCard != null) {
                    currentFlashcard[0] = existingCard;
                    updateAddFlashcardUI(true);
                } else {
                    updateAddFlashcardUI(false);
                }
            });
        }).start();

        titleTextView.setText(title);
        contentTextView.setText(content);
        contentTextView.setTypeface(Typeface.MONOSPACE);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean[] isRead = {prefs.getBoolean("read_" + title, false)};
        updateMarkReadUI(isRead[0]);



        findViewById(R.id.markAsReadContainer).setOnClickListener(v -> {
            isRead[0] = !isRead[0];
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("read_" + title, isRead[0]);
            editor.apply();

            Intent updateIntent = new Intent("ARTICLE_PROGRESS_UPDATED");
            sendBroadcast(updateIntent);

            updateMarkReadUI(isRead[0]); // Update the action bar UI
            Toast.makeText(this, isRead[0] ? "Marked as read" : "Marked as unread", Toast.LENGTH_SHORT).show();
            // Send broadcast to update the list
            hideActionBar(); // Optional: Hide the bar after click
        });
        final boolean[] isAdded = {false};

        findViewById(R.id.addToFlashcardsContainer).setOnClickListener(v -> {
            new Thread(() -> {
                FlashcardDao dao = FlashcardDatabase.getDatabase(this).flashcardDao();
                String currentTitle = titleTextView.getText().toString();
                Flashcard existingCard = dao.getFlashcardByTitle(currentTitle);

                runOnUiThread(() -> {
                    if (existingCard != null) {
                        // Show confirmation dialog before deletion
                        new AlertDialog.Builder(ArticleDetailActivity.this)
                                .setTitle("Remove Flashcard")
                                .setMessage("Are you sure you want to remove this flashcard?")
                                .setPositiveButton("Remove", (dialog, which) -> {
                                    // User confirmed - proceed with deletion
                                    new Thread(() -> {
                                        dao.delete(existingCard);
                                        runOnUiThread(() -> {
                                            currentFlashcard[0] = null;
                                            updateAddFlashcardUI(false);
                                            Toast.makeText(this, "Removed from flashcards", Toast.LENGTH_SHORT).show();
                                            hideActionBar();

                                            // ADD THIS: Send broadcast after deletion
                                            sendFlashcardUpdateBroadcast();
                                        });
                                    }).start();
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .setCancelable(true)
                                .show();
                    } else {
                        // If card doesn't exist, add it
                        new Thread(() -> {
                            Flashcard newCard = new Flashcard(currentTitle, contentTextView.getText().toString());
                            dao.insert(newCard);
                            runOnUiThread(() -> {
                                currentFlashcard[0] = newCard;
                                updateAddFlashcardUI(true);
                                Toast.makeText(this, "Added to flashcards", Toast.LENGTH_SHORT).show();
                                hideActionBar();

                                // ADD THIS: Send broadcast after addition
                                sendFlashcardUpdateBroadcast();
                            });
                        }).start();
                    }
                });
            }).start();
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleActionBar();
                return true;
            }
        });

        // NEW: Action bar touch/scroll logic
        FrameLayout contentFrame = findViewById(R.id.contentFrame);
        contentFrame.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false; // Let the event propagate to children
            }
        });


        contentScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (isActionBarVisible) {
                hideActionBar();
            }
        });

        findViewById(R.id.viewExplanationContainer).setOnClickListener(v -> {
            showExplanation();
            hideActionBar();
        });

        showActionBar();
    }

    // ADD THIS HELPER METHOD:
    private void sendFlashcardUpdateBroadcast() {
        Intent updateIntent = new Intent(AppConstants.FLASHCARD_UPDATE_ACTION);
        updateIntent.setPackage(getPackageName());
        sendBroadcast(updateIntent);
    }


    private String loadExplanationForTitle(String title) {
        try {
            InputStream is = getAssets().open("explanation.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject root = new JSONObject(jsonString);
            JSONArray articles = root.getJSONArray("articles");

            for (int i = 0; i < articles.length(); i++) {
                JSONObject article = articles.getJSONObject(i);
                if (article.getString("title").equalsIgnoreCase(title.trim())) {
                    return article.getString("explanation");
                }
            }
        } catch (IOException | JSONException e) {
            System.out.println("Failed to load");
        }
        return null;
    }

    private void updateMarkReadUI(boolean isRead) {
        runOnUiThread(() -> {
            if (isRead) {
                String a = "Marked Read";
                markReadIcon.setImageResource(R.drawable.ic_marked_as_read);
                markReadText.setText(a);
            } else {
                String a = "Mark Read";
                markReadIcon.setImageResource(R.drawable.ic_marks_as_read);
                markReadText.setText(a);
            }
        });
    }

    private void updateAddFlashcardUI(boolean isAdded) {
        runOnUiThread(() -> {
            if (isAdded) {
                String b = "Added";
                addFlashcardIcon.setImageResource(R.drawable.ic_flashcard_added);
                addFlashcardText.setText(b);
            } else {
                addFlashcardIcon.setImageResource(R.drawable.ic_addtoflashcard);
                addFlashcardText.setText("Add Flashcard");
            }
        });
    }


    private void setupGestureDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleActionBar();
                return true;
            }
        });
    }

    private void setupTouchHandling() {
        FrameLayout contentFrame = findViewById(R.id.contentFrame);
        contentFrame.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // Let children handle the event if needed
        });

        // Ensure ScrollView doesn't interfere with the action bar
        contentScrollView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }


    private void toggleActionBar() {
        runOnUiThread(() -> {
            if (isActionBarVisible) {
                hideActionBar();
            } else {
                showActionBar();
            }
        });
    }


    private void showActionBar() {
        if (isActionBarVisible || actionBar == null) return;

        isActionBarVisible = true;
        actionBar.setVisibility(View.VISIBLE);
        actionBar.setAlpha(0f);
        actionBar.setTranslationY(actionBar.getHeight());

        actionBar.animate()
                .translationY(0)
                .alpha(1f)
                .setDuration(250)
                .start();

        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.postDelayed(autoHideRunnable, AUTO_HIDE_DELAY);
    }

    private void hideActionBar() {
        if (!isActionBarVisible || actionBar == null) return;

        isActionBarVisible = false;
        autoHideHandler.removeCallbacks(autoHideRunnable);

        actionBar.animate()
                .translationY(actionBar.getHeight())
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> actionBar.setVisibility(View.GONE))
                .start();
    }


    private void showExplanation() {
        String title = titleTextView.getText().toString();
        String explanation = loadExplanationForTitle(title);

        if (explanation == null || explanation.isEmpty()) {
            Toast.makeText(this, "Explanation not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_explanation);

        // Get current font size from preferences
        Context context = this.createDeviceProtectedStorageContext(); // optional based on your need
        SharedPreferences prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE);

        float fontSize = prefs.getFloat("font_size_value", 20f);

        // Set explanation text with proper font size
        TextView explanationText = dialog.findViewById(R.id.dialogExplanationText);
        explanationText.setText(explanation);
        explanationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize); // Apply font size

        // Set close button action
        Button closeButton = dialog.findViewById(R.id.dialogCloseButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize); // Apply to button too

        // Configure dialog window
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.8f);
            window.getAttributes().windowAnimations = R.style.DialogAnimation;

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        dialog.show();
    }

}
