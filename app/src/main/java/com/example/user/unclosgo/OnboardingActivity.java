package com.example.user.unclosgo;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class OnboardingActivity extends AppCompatActivity {
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        prefManager = new PrefManager(this);

        // If not first time launch AND user has agreed to disclaimer, go to MainActivity
        if (!prefManager.isFirstTimeLaunch() && prefManager.hasAgreedToDisclaimer()) {
            launchMainActivity();
            finish();
            return;
        }

        // If user hasn't agreed to disclaimer (even if not first time), reset onboarding
        if (!prefManager.hasAgreedToDisclaimer()) {
            prefManager.setFirstTimeLaunch(true); // Force onboarding to show again
        }

        // Load first fragment
        loadFragment(new OnboardingFragment1());
    }

    public void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    public void goToNextFragment(int currentFragment) {
        switch (currentFragment) {
            case 1:
                loadFragment(new OnboardingFragment2());
                break;
            case 2:
                loadFragment(new OnboardingFragment3());
                break;
            case 3:
                completeOnboarding();
                break;
        }
    }

    private void completeOnboarding() {
        prefManager.setFirstTimeLaunch(false);
        startActivity(new Intent(this, DisclaimerActivity.class));
        finish();
    }

    private void launchMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}