package com.example.user.unclosgo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class AllFlashcardsActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private FlashcardAdapter adapter;
    private FlashcardDao flashcardDao;
    private View emptyStateLayout;  // Add this

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // The LiveData observer will handle the refresh automatically
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_flashcards);

        recyclerView = findViewById(R.id.recycler_all_flashcards);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);  // Initialize empty state
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FlashcardDatabase db = FlashcardDatabase.getDatabase(getApplicationContext());
        flashcardDao = db.flashcardDao();

        flashcardDao.getAllFlashcards().observe(this, flashcards -> {
            if (flashcards == null || flashcards.isEmpty()) {
                // Show empty state
                recyclerView.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
                Button doneButton = emptyStateLayout.findViewById(R.id.button_done);
                doneButton.setOnClickListener(v -> finish());
            } else {
                // Show flashcards
                recyclerView.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);

                adapter = new FlashcardAdapter(flashcards,
                        flashcard -> {
                            Intent intent = new Intent(AllFlashcardsActivity.this, FlashcardDetailActivity.class);
                            intent.putExtra("title", flashcard.getTitle());
                            intent.putExtra("content", flashcard.getContent());
                            startActivity(intent);
                        },
                        flashcard -> {
                            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(AllFlashcardsActivity.this, R.style.AlertDialogTheme)
                                    .setTitle("Remove")
                                    .setMessage("Are you sure you want to remove this flashcard?")
                                    .setIcon(R.drawable.ida) // Optional: use your own icon here
                                    .setPositiveButton("Delete", (dialogInterface, which) -> {
                                        new Thread(() -> {
                                            flashcardDao.delete(flashcard);
                                            runOnUiThread(() -> {
                                                Toast.makeText(AllFlashcardsActivity.this,
                                                        "Flashcard deleted", Toast.LENGTH_SHORT).show();
                                            });
                                        }).start();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .create();

                            dialog.show();

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

                            // Optional: style the buttons
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(ContextCompat.getColor(AllFlashcardsActivity.this, R.color.textSecondary));
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                                    .setTextColor(ContextCompat.getColor(AllFlashcardsActivity.this, R.color.textSecondary));
                        }
                );


                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutAnimation(
                        AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
                );
                recyclerView.getAdapter().notifyDataSetChanged();
                recyclerView.scheduleLayoutAnimation();
            }
        });

        registerReceiver(updateReceiver, new IntentFilter(AppConstants.FLASHCARD_UPDATE_ACTION),
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(updateReceiver);
    }
}

