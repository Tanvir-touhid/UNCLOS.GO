package com.example.user.unclosgo;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

// Add this inner class to your MainActivity (or as a separate file if preferred)
public class RateUsHelper {
    private static final String PREF_NEVER_SHOW_RATE = "pref_never_show_rate";
    private static final String PREF_LAUNCH_COUNT = "pref_launch_count";
    private static final String PREF_FIRST_LAUNCH = "pref_first_launch";
    private static final int LAUNCHES_UNTIL_PROMPT = 5; // Ask after 5 launches
    private static final long DAYS_UNTIL_PROMPT = 3; // Ask after 3 days

    private final Context context;
    private final SharedPreferences prefs;

    public RateUsHelper(Context context) {
        this.context = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void handleRateAction() {
        if (shouldShowRateDialog()) {
            showRateDialog();
        } else {
            launchPlayStore();
        }
    }

    private boolean shouldShowRateDialog() {
        // Check if user selected "Never"
        if (prefs.getBoolean(PREF_NEVER_SHOW_RATE, false)) {
            return false;
        }

        // Check launch count and date
        SharedPreferences.Editor editor = prefs.edit();

        // Get current launch count
        long launchCount = prefs.getLong(PREF_LAUNCH_COUNT, 0) + 1;
        editor.putLong(PREF_LAUNCH_COUNT, launchCount);

        // Get date of first launch
        Long firstLaunchDate = prefs.getLong(PREF_FIRST_LAUNCH, 0);
        if (firstLaunchDate == 0) {
            firstLaunchDate = System.currentTimeMillis();
            editor.putLong(PREF_FIRST_LAUNCH, firstLaunchDate);
        }

        editor.apply();

        // Wait at least n launches and n days
        return launchCount >= LAUNCHES_UNTIL_PROMPT &&
                System.currentTimeMillis() >= firstLaunchDate + (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000);
    }

    private void showRateDialog() {
        // Create styled title
        SpannableString styledTitle = new SpannableString(context.getString(R.string.rate_our_app));
        styledTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, styledTitle.length(), 0);
        styledTitle.setSpan(new AbsoluteSizeSpan(20, true), 0, styledTitle.length(), 0);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.AlertDialogTheme)
                .setTitle(styledTitle)
                .setMessage(R.string.rate_app_message)
                .setPositiveButton(R.string.rate_now, (d, which) -> {
                    launchPlayStore();
                    setNeverShowAgain();
                })
                .setNeutralButton(R.string.later, null)
                .setNegativeButton(R.string.never, (d, which) -> setNeverShowAgain())
                .create();

        dialog.show();

        // Customize dialog appearance to match your app's style
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(context, R.color.article_reading_background));
            background.setCornerRadius(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics())
            );
            background.setStroke(1, Color.LTGRAY);
            window.setBackgroundDrawable(background);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);

            // Style buttons
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));

            Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
        }
    }

    private void launchPlayStore() {
        String packageName = context.getPackageName();
        try {
            // Try Play Store app first
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException e) {
            // Fallback to browser
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    private void setNeverShowAgain() {
        prefs.edit().putBoolean(PREF_NEVER_SHOW_RATE, true).apply();
    }
}