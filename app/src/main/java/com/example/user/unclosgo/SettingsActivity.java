package com.example.user.unclosgo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseActivity {

    private SharedPreferences sharedPreferences;
    private BroadcastReceiver fontSizeReceiver;

    private EditText initialEaseFactorEditText;
    private EditText graduationEasiesEditText;
    private SeekBar fuzzPercentSeekBar;
    private TextView fuzzPercentTextView;
    private Slider fontSizeSlider;
    private TextView fontSizePreviewText;
    private SwitchMaterial darkModeSwitch;

    private boolean hasUnsavedChanges = false;
    private float initialFontSize;
    private float initialEaseFactor;
    private int initialGraduationEasies;
    private int initialFuzzPercent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Load initial values to compare later
        loadInitialValues();

        // Initialize the BroadcastReceiver
        fontSizeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("FONT_SIZE_CHANGED".equals(intent.getAction())) {
                    float newSize = intent.getFloatExtra("new_size", 16f);
                    fontSizePreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, newSize);
                }
            }
        };

        // Register the receiver
        registerReceiver(fontSizeReceiver, new IntentFilter("FONT_SIZE_CHANGED"), Context.RECEIVER_NOT_EXPORTED);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);

        // Initialize views
        initialEaseFactorEditText = findViewById(R.id.editTextInitialEaseFactor);
        graduationEasiesEditText = findViewById(R.id.editTextGraduationEasies);
        fuzzPercentSeekBar = findViewById(R.id.seekBarFuzzPercent);
        fuzzPercentTextView = findViewById(R.id.textViewFuzzPercentValue);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        fontSizeSlider = findViewById(R.id.fontSizeSlider);
        fontSizePreviewText = findViewById(R.id.fontSizePreviewText);

        // Setup font size slider
        float savedSize = sharedPreferences.getFloat("font_size_value", 16f);
        fontSizeSlider.setValue(savedSize);
        fontSizePreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, savedSize);

        // SRS Info button click listener
        findViewById(R.id.infoIconSrs).setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_srs_info, null);
            TextView infoText = dialogView.findViewById(R.id.infoText);
            infoText.setText("• Initial Ease Factor:\nThis tells the app how easy the article feels the first time you add it for review. For example, if an article feels very simple, it gets a higher score, and you won't see it too often right away.\n\n" +
                    "• Graduation Easies:\nThis means how many times you need to press 'Easy' on an article before it's considered fully learned and moves out of the learning phase. Example: If it's set to 2, you need to tap 'Easy' two times (on two different days).\n\n" +
                    "• Fuzz Percentage:\nThis mixes up your review timing just a little so you're not expecting the article at an exact time. For example, if something is due tomorrow, it might come today or the day after — this helps you actually learn it, not just remember when it's coming.");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                infoText.setJustificationMode(Layout.JUSTIFICATION_MODE_INTER_WORD);
            }
            new AlertDialog.Builder(this)
                    .setTitle("SRS Settings Explained")
                    .setView(dialogView)
                    .setPositiveButton("OK", null)
                    .show();
        });

        // Fuzz percentage seekbar listener
        fuzzPercentSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fuzzPercentTextView.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Font size slider listener
        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                fontSizePreviewText.setTextSize(TypedValue.COMPLEX_UNIT_SP, value);
                // Save temporarily (permanent save happens when Save button is clicked)
                sharedPreferences.edit().putFloat("font_size_value_temp", value).apply();

                // Broadcast for live preview
                Intent intent = new Intent("FONT_SIZE_CHANGED");
                intent.putExtra("new_size", value);
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
            }
        });

        // Dark mode switch listener
        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Post the theme change to ensure smooth animation
            darkModeSwitch.postDelayed(() -> {
                AppCompatDelegate.setDefaultNightMode(isChecked ?
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                sharedPreferences.edit().putBoolean("dark_mode", isChecked).apply();
            }, 100); // 100ms delay to let the switch animation complete
        });

        // Load saved settings
        loadSettings();

        // Save button click listener
        Button saveButton = findViewById(R.id.buttonSave);
        saveButton.setOnClickListener(v -> saveSettings());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the activity is destroyed
        if (fontSizeReceiver != null) {
            unregisterReceiver(fontSizeReceiver);
        }
    }

    private void loadInitialValues() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        initialFontSize = prefs.getFloat("font_size_value", 16f);
        initialEaseFactor = prefs.getFloat("initial_ease_factor", 2.5f);
        initialGraduationEasies = prefs.getInt("graduation_easies", 2);
        initialFuzzPercent = prefs.getInt("fuzz_percent", 10);
    }

    private boolean checkForUnsavedChanges() {
        // Get current UI values
        float currentFontSize = fontSizeSlider.getValue();
        float currentEaseFactor;
        int currentGraduationEasies;
        int currentFuzzPercent = fuzzPercentSeekBar.getProgress();

        try {
            currentEaseFactor = Float.parseFloat(initialEaseFactorEditText.getText().toString());
            currentGraduationEasies = Integer.parseInt(graduationEasiesEditText.getText().toString());
        } catch (NumberFormatException e) {
            // If parsing fails, consider no changes (or handle differently)
            return false;
        }

        // Compare with initial values
        return currentFontSize != initialFontSize ||
                currentEaseFactor != initialEaseFactor ||
                currentGraduationEasies != initialGraduationEasies ||
                currentFuzzPercent != initialFuzzPercent;
    }

    private void showUnsavedChangesDialog() {
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Do you want to save them before exiting?")
                .setPositiveButton("Save", (dialogInterface, which) -> {
                    saveSettings();
                    finish();
                })
                .setNegativeButton("Discard", (dialogInterface, which) -> {
                    finish();
                })
                .setNeutralButton("Cancel", (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                })
                .create();

        dialog.show();

        // Apply rounded corners
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

        // Apply consistent button text style
        int textAppearance = R.style.AlertDialogTheme;
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextAppearance(this, textAppearance);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextAppearance(this, textAppearance);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextAppearance(this, textAppearance);
    }

    @Override
    public void onBackPressed() {
        if (checkForUnsavedChanges()) {
            showUnsavedChangesDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void loadSettings() {
        // Load settings from SharedPreferences
        float initialEaseFactor = sharedPreferences.getFloat("initial_ease_factor", 2.5f);
        int graduationEasies = sharedPreferences.getInt("graduation_easies", 2);
        int fuzzPercent = sharedPreferences.getInt("fuzz_percent", 10);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);

        // Set values in UI components
        initialEaseFactorEditText.setText(String.valueOf(initialEaseFactor));
        graduationEasiesEditText.setText(String.valueOf(graduationEasies));
        fuzzPercentSeekBar.setProgress(fuzzPercent);
        fuzzPercentTextView.setText(fuzzPercent + "%");

        // Set dark mode switch state
        darkModeSwitch.setChecked(isDarkMode);
    }

    private void saveSettings() {
        // Get all current values
        float fontSize = fontSizeSlider.getValue();
        float initialEaseFactor = Float.parseFloat(initialEaseFactorEditText.getText().toString());
        int graduationEasies = Integer.parseInt(graduationEasiesEditText.getText().toString());
        int fuzzPercent = fuzzPercentSeekBar.getProgress();

        // Save all settings permanently
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putFloat("font_size_value", fontSize);
        editor.putFloat("initial_ease_factor", initialEaseFactor);
        editor.putInt("graduation_easies", graduationEasies);
        editor.putInt("fuzz_percent", fuzzPercent);
        editor.apply();

        // Broadcast final font size change
        Intent intent = new Intent("FONT_SIZE_CHANGED");
        intent.putExtra("new_size", fontSize);
        intent.setPackage(getPackageName());
        sendBroadcast(intent);

        loadInitialValues();
        hasUnsavedChanges = false;

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private void setupChangeListeners() {
        fontSizeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) hasUnsavedChanges = true;
        });

        initialEaseFactorEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                hasUnsavedChanges = true;
            }
        });

        graduationEasiesEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                hasUnsavedChanges = true;
            }
        });

        fuzzPercentSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) hasUnsavedChanges = true;
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}