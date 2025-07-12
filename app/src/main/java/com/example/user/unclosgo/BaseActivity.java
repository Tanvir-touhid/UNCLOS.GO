package com.example.user.unclosgo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BaseActivity extends AppCompatActivity {
    protected float currentFontSize = 16f; // Default medium
    private BroadcastReceiver fontChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // New additions for navigation and screen adaptation
        enableModernEdgeToEdge();
        loadFontSizePref();
        setupFontChangeReceiver();
    }
    private void enableModernEdgeToEdge() {
        // 1. Request edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 2. Get the WindowInsetsControllerCompat (works on all API levels)
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(
                getWindow(),
                getWindow().getDecorView()
        );

        if (insetsController != null) {
            // 3. Configure system bars behavior
            insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );

            // 4. Make navigation bar and status bar fully transparent
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);

            // 5. Set light/dark appearance based on theme
            boolean isDarkMode = isDarkMode();
            insetsController.setAppearanceLightNavigationBars(!isDarkMode);
            insetsController.setAppearanceLightStatusBars(!isDarkMode);
        }

        // 6. For API 30+, ensure legacy flags are set for full compatibility
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
    }

    private boolean isDarkMode() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        return prefs.getBoolean("dark_mode", false);
    }

    private void applyMarginToAllCards(ViewGroup root, int marginPx) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);

            // Recursively go through nested layouts
            if (child instanceof ViewGroup) {
                applyMarginToAllCards((ViewGroup) child, marginPx);
            }

            // Apply margins if it's a CardView or a similar custom card class
            if (child.getClass().getSimpleName().contains("Card")) {
                ViewGroup.MarginLayoutParams params =
                        (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                params.setMargins(marginPx, marginPx, marginPx, marginPx);
                child.setLayoutParams(params);
            }
        }
    }


    private void adjustForSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            // Apply padding to avoid overlap with system bars
            int bottomInset = insets.getSystemWindowInsetBottom();
            int rightInset = insets.getSystemWindowInsetRight();
            int leftInset = insets.getSystemWindowInsetLeft();

            // Apply to main container
            View mainContainer = findViewById(R.id.main_container);
            if (mainContainer != null) {
                mainContainer.setPadding(leftInset, 0, rightInset, bottomInset);
            }

            return insets;
        });
    }


    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyFontSizes(); // Apply fonts after layout is set
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyFontSizes(); // Apply fonts after layout is set
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Handle font scale changes
        if (newConfig.fontScale != getResources().getConfiguration().fontScale) {
            recreate();
        }

        // Handle screen layout changes
        if (newConfig.screenLayout != getResources().getConfiguration().screenLayout) {
            adjustLayoutForScreenSize();
            adjustScrollViewContent();
        }
    }

    private void adjustScrollViewContent() {
        NestedScrollView scrollView = findViewById(R.id.nestedScrollView);
        if (scrollView != null) {
            // Ensure content isn't clipped
            scrollView.setClipToPadding(false);
            scrollView.setPadding(
                    scrollView.getPaddingLeft(),
                    scrollView.getPaddingTop(),
                    scrollView.getPaddingRight(),
                    (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            16,
                            getResources().getDisplayMetrics()
                    )
            );
        }
    }

    private void adjustLayoutForScreenSize() {
        Configuration config = getResources().getConfiguration();

        // Get screen size category
        int screenLayout = config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        switch (screenLayout) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                // Adjust for small screens
                adjustMargins(4); // dp
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                // Normal phone size
                adjustMargins(8); // dp
                break;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                // Tablets
                adjustMargins(16); // dp
                break;
            case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                // Large tablets
                adjustMargins(24); // dp
                break;
            default:
                adjustMargins(8); // dp
        }
    }

    private void adjustMargins(int marginDp) {
        int marginPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                marginDp,
                getResources().getDisplayMetrics()
        );

        // Existing manual example (still needed for specific cases)
        View exitButton = findViewById(R.id.nav_exit);
        if (exitButton != null) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) exitButton.getLayoutParams();
            params.setMargins(marginPx, marginPx, marginPx, marginPx);
            exitButton.setLayoutParams(params);
        }

        // ✅ Automatically apply margin to all CardViews in the layout
        View rootView = findViewById(android.R.id.content);
        if (rootView instanceof ViewGroup) {
            applyMarginToAllCards((ViewGroup) rootView, marginPx);
        }
    }


    private void setupFontChangeReceiver() {
        fontChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadFontSizePref();
                applyFontSizes();
            }
        };

        IntentFilter filter = new IntentFilter("FONT_SIZE_CHANGED");
        registerReceiver(fontChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        // <-- system-wide broadcast
    }


    protected void loadFontSizePref() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        // Use the slider value directly, not the index-based system
        currentFontSize = prefs.getFloat("font_size_value", 16f); // Default 16sp
    }

    protected void applyFontSizes() {
        // This will work with any activity layout
        View rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView instanceof ViewGroup) {
            setFontSize((ViewGroup) rootView, currentFontSize);
        }
    }

    private void setFontSize(ViewGroup root, float size) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child instanceof TextView && !(child instanceof Button)) {
                int id = child.getId();
                TextView textView = (TextView) child;

                if (id == R.id.articleProgressText ||
                        id == R.id.masteredProgressText ||
                        id == R.id.pendingProgressText ||
                        id == R.id.percentageArticles ||
                        id == R.id.percentageMastered ||
                        id == R.id.percentagePending) {
                    // Apply reduced size (e.g., 80% of main font size)
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size * 0.8f);
                } else {
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
                }
            } else if (child instanceof ViewGroup) {
                setFontSize((ViewGroup) child, size);
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fontChangeReceiver != null) {
            unregisterReceiver(fontChangeReceiver);
        }
    }
}