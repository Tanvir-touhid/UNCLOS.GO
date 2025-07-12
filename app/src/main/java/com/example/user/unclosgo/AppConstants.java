package com.example.user.unclosgo;

public class AppConstants {
    // Broadcast Actions
    public static final String FLASHCARD_UPDATE_ACTION = "com.example.user.unclosgo.FLASHCARD_UPDATED";
    public static final String ARTICLE_UPDATE_ACTION = "com.example.user.unclosgo.ARTICLE_UPDATED";
    public static final String ARTICLE_PROGRESS_UPDATED = "com.example.user.unclosgo.ARTICLE_PROGRESS_UPDATED";
    public static final String THEME_CHANGED = "THEME_CHANGED";

    // SharedPreferences Keys
    public static final String PREF_DARK_MODE = "dark_mode";
    public static final String PREF_READ_PREFIX = "read_";
    public static final String PREF_REVIEW_TIME_PREFIX = "reviewTime_";
    public static final String PREF_REVIEW_CHOICE_PREFIX = "reviewChoice_";

    // Flashcard Status Values
    public static final String FLASHCARD_STATUS_NEW = "new";
    public static final String FLASHCARD_STATUS_LEARNING = "learning";
    public static final String FLASHCARD_STATUS_REVIEW = "review";

    // Review Choices
    public static final String REVIEW_AGAIN = "again";
    public static final String REVIEW_HARD = "hard";
    public static final String REVIEW_GOOD = "good";
    public static final String REVIEW_EASY = "easy";

    // Default Values
    public static final float DEFAULT_EASE_FACTOR = 2.5f;
    public static final int DEFAULT_INTERVAL = 1;
    public static final int DEFAULT_REPETITIONS = 0;
    public static final int TOTAL_ARTICLES = 320;

    // Intent Extras
    public static final String EXTRA_FLASHCARD_ID = "flashcardId";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_UPDATED = "updated";

    // Request Codes
    public static final int REQUEST_FLASHCARD_DETAIL = 1;

    // Time Constants (in milliseconds)
    public static final long TEN_MINUTES = 10 * 60 * 1000;
    public static final long TWENTY_MINUTES = 20 * 60 * 1000;
    public static final long TWO_HOURS = 2 * 60 * 60 * 1000;
    public static final long ONE_DAY = 24 * 60 * 60 * 1000;
}