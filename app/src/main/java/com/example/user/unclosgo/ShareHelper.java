package com.example.user.unclosgo;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class ShareHelper {
    private final Activity activity;
    private final String packageName;
    private final String appName;

    public ShareHelper(Activity activity) {
        this.activity = activity;
        this.packageName = activity.getPackageName();
        this.appName = activity.getString(R.string.app_name);
    }

    public void showShareDialog() {
        // Create styled title
        SpannableString styledTitle = new SpannableString(activity.getString(R.string.share_app_title));
        styledTitle.setSpan(new StyleSpan(Typeface.BOLD), 0, styledTitle.length(), 0);
        styledTitle.setSpan(new AbsoluteSizeSpan(20, true), 0, styledTitle.length(), 0);

        // Tint the icon
        Drawable icon = ContextCompat.getDrawable(activity, R.drawable.share);
        if (icon != null) {
            icon.setTint(ContextCompat.getColor(activity, R.color.textSecondary));
        }

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AlertDialogTheme)
                .setTitle(styledTitle)
                .setMessage(R.string.share_app_prompt)
                .setPositiveButton(R.string.share_now, (d, which) -> launchShareIntent())
                .setNegativeButton(R.string.cancel, null)
                .setIcon(icon)
                .create();

        dialog.show();

        // Apply identical styling to exit dialog
        Window window = dialog.getWindow();
        if (window != null) {
            GradientDrawable background = new GradientDrawable();
            background.setColor(ContextCompat.getColor(activity, R.color.article_reading_background));
            background.setCornerRadius(
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                            activity.getResources().getDisplayMetrics())
            );
            background.setStroke(1, Color.LTGRAY);
            window.setBackgroundDrawable(background);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }

        // Set identical button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(activity, R.color.textSecondary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(activity, R.color.textSecondary));
    }


    private void launchShareIntent() {
        String shareMessage = activity.getString(R.string.share_app_message,
                appName,
                "https://play.google.com/store/apps/details?id=" + packageName);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, appName);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        try {
            activity.startActivity(Intent.createChooser(shareIntent,
                    activity.getString(R.string.share_via)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity,
                    R.string.no_sharing_app_found,
                    Toast.LENGTH_SHORT).show();
        }
    }
}