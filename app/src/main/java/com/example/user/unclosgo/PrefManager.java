package com.example.user.unclosgo;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {
    private static final String PREF_NAME = "myapp_pref";
    private static final String IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch";
    private static final String HAS_AGREED_TO_DISCLAIMER = "HasAgreedToDisclaimer";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;

    public PrefManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        editor.putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime);
        editor.commit();
    }

    public boolean isFirstTimeLaunch() {
        return pref.getBoolean(IS_FIRST_TIME_LAUNCH, true);
    }

    public void setHasAgreedToDisclaimer(boolean hasAgreed) {
        editor.putBoolean(HAS_AGREED_TO_DISCLAIMER, hasAgreed);
        editor.commit();
    }

    public boolean hasAgreedToDisclaimer() {
        return pref.getBoolean(HAS_AGREED_TO_DISCLAIMER, false);
    }
}