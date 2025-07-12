package com.example.user.unclosgo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Flashcard.class}, version = 4, exportSchema = false)
public abstract class FlashcardDatabase extends RoomDatabase {
    public abstract FlashcardDao flashcardDao();

    private static FlashcardDatabase INSTANCE;

    public static synchronized FlashcardDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (FlashcardDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    FlashcardDatabase.class, "flashcard.db")
                            .addMigrations(new Migration(3, 4) {
                                @Override
                                public void migrate(@NonNull SupportSQLiteDatabase database) {
                                    // Update all "learn" statuses to "learning"
                                    database.execSQL("UPDATE flashcards SET status = 'learning' WHERE status = 'learn'");
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
