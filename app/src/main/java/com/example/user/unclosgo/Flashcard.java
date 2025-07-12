package com.example.user.unclosgo;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "flashcards",
        indices = {@Index(value = {"title"}, unique = true)}
)
public class Flashcard implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "status") // e.g., "new", "learning", "review"
    private String status;

    @ColumnInfo(name = "dueAt") // Optional, can be removed if using nextReview only
    private long dueAt;

    @ColumnInfo(name = "repetitions")
    private int repetitions = 0;

    @ColumnInfo(name = "interval")
    private int interval = 1;

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    private long creationTime; // NEW FIELD
    private int goodCountDuringLearning;



    public int getGoodCountDuringLearning() {
        return goodCountDuringLearning;
    }

    public void setGoodCountDuringLearning(int count) {
        this.goodCountDuringLearning = count;
    }

    @ColumnInfo(name = "easeFactor")
    private double easeFactor = 2.5f;

    @ColumnInfo(name = "nextReview")
    private long nextReview;

    @ColumnInfo(name = "isRead")
    private boolean isRead;

    @ColumnInfo(name = "reviewChoice")
    private String reviewChoice; // Add this field to store the review choice

    @ColumnInfo(name = "reviewTime")
    private long reviewTime; // This field stores the time when the flashcard was reviewed

    // Getter and Setter for reviewTime
    public long getReviewTime() {
        return reviewTime;
    }

    public void setReviewTime(long reviewTime) {
        this.reviewTime = reviewTime;
    }

    // Getter and Setter for reviewChoice
    public String getReviewChoice() {
        return reviewChoice;
    }

    public void setReviewChoice(String reviewChoice) {
        this.reviewChoice = reviewChoice;
    }


    // 🚀 Default constructor (needed for Room)
    public Flashcard() {
        this.repetitions = 0;
        this.interval = 1;
        this.easeFactor = 2.5;
        this.nextReview = System.currentTimeMillis();
        this.status = "new";
        this.dueAt = this.nextReview;
        this.isRead = false;
    }

    // 🚀 Constructor for creating a new card manually
    public Flashcard(String title, String content) {
        this();
        this.title = title;
        this.content = content;
        this.creationTime = System.currentTimeMillis();
    }

    // 🛠 Getters and setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getDueAt() { return dueAt; }
    public void setDueAt(long dueAt) { this.dueAt = dueAt; }

    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public double getEaseFactor() { return easeFactor; }
    public void setEaseFactor(double easeFactor) { this.easeFactor = easeFactor; }

    public long getNextReview() { return nextReview; }
    public void setNextReview(long nextReview) { this.nextReview = nextReview; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    // 📦 Parcelable support

    protected Flashcard(Parcel in) {
        id = in.readInt();
        title = in.readString();
        content = in.readString();
        status = in.readString();
        dueAt = in.readLong();
        repetitions = in.readInt();
        interval = in.readInt();
        easeFactor = in.readDouble();
        nextReview = in.readLong();
        isRead = in.readByte() != 0;
    }

    public static final Creator<Flashcard> CREATOR = new Creator<Flashcard>() {
        @Override
        public Flashcard createFromParcel(Parcel in) {
            return new Flashcard(in);
        }

        @Override
        public Flashcard[] newArray(int size) {
            return new Flashcard[size];
        }
    };

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeString(title);
        parcel.writeString(content);
        parcel.writeString(status);
        parcel.writeLong(dueAt);
        parcel.writeInt(repetitions);
        parcel.writeInt(interval);
        parcel.writeDouble(easeFactor);
        parcel.writeLong(nextReview);
        parcel.writeByte((byte) (isRead ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
