package com.example.user.unclosgo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class OnboardingFragment1 extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_onboarding1, container, false);

        Button getStartedButton = view.findViewById(R.id.getStartedButton);
        getStartedButton.setOnClickListener(v -> {
            ((OnboardingActivity)requireActivity()).goToNextFragment(1);
        });

        return view;
    }
}