package com.example.user.unclosgo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DisclaimerActivity extends AppCompatActivity {
    private PrefManager prefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disclaimer);

        prefManager = new PrefManager(this);

        Button agreeButton = findViewById(R.id.agreeButton);
        Button exitButton = findViewById(R.id.exitButton);

        agreeButton.setOnClickListener(v -> {
            // User agrees, set flag and launch main app activity
            prefManager.setHasAgreedToDisclaimer(true);
            Intent intent = new Intent(DisclaimerActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        exitButton.setOnClickListener(v -> {
            // User does not agree, clear flags and exit
            prefManager.setHasAgreedToDisclaimer(false);
            prefManager.setFirstTimeLaunch(true); // Force onboarding to show next time
            finishAffinity(); // This closes the app and clears task stack
        });
    }
}