package com.example.user.unclosgo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class FlashcardUpdateReceiver extends BroadcastReceiver {
    private final Runnable onUpdate;

    public FlashcardUpdateReceiver(Runnable onUpdate) {
        this.onUpdate = onUpdate;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onUpdate.run();
    }
}
