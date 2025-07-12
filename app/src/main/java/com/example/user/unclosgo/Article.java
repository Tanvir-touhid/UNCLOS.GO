package com.example.user.unclosgo;

public class Article {
    private int id;
    private String title;
    private String content;

    private long reviewTime;       // Time for the next review (in milliseconds)
    private String reviewChoice;   // User's review choice (e.g., "again", "hard")
    private int repetitions;       // Number of repetitions
    private float easeFactor;      // Ease factor for SM2 algorithm
    private int interval;          // Interval for next review (in days)

    // Constructor
    public Article(int id, String title, String content) {
        this.id = id;
        this.title = title;
        this.content = content;

        this.repetitions = 0;
        this.easeFactor = 2.5f; // Default ease factor
        this.interval = 1; // Initial interval (1 day)
    }
    private int iconResId;

    public int getIconResId() { return iconResId; }
    public void setIconResId(int iconResId) { this.iconResId = iconResId; }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public long getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(long reviewTime) {
        this.reviewTime = reviewTime;
    }

    public String getReviewChoice() {
        return reviewChoice;
    }

    public void setReviewChoice(String reviewChoice) {
        this.reviewChoice = reviewChoice;
    }

    public int getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(int repetitions) {
        this.repetitions = repetitions;
    }

    public float getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(float easeFactor) {
        this.easeFactor = easeFactor;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}


