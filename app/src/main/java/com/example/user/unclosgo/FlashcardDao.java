package com.example.user.unclosgo;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FlashcardDao {

    // ========== Basic CRUD Operations ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Flashcard card);

    @Update
    void update(Flashcard flashcard);

    @Delete
    void delete(Flashcard flashcard);

    // ========== Single Flashcard Queries ==========

    // int versions (for StudyActivity - non-reactive)

    @Query("SELECT * FROM flashcards")
    LiveData<List<Flashcard>> getAllFlashcards();

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'new' AND reviewTime <= :now")
    int countNewCardsReady(long now);

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'learning' AND reviewTime <= :now")
    int countLearningCards(long now);

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'review' AND reviewTime <= :now")
    int countDueCards(long now);

    // ========== LiveData Counting Methods ==========

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'review'")
    LiveData<Integer> countTotalReviewCards();

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'new' AND nextReview <= :currentTime")
    LiveData<Integer> countNewCardsReadyLive(long currentTime);

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'learning' AND nextReview <= :currentTime")
    LiveData<Integer> countLearningCardsLive(long currentTime);

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'review' AND nextReview <= :currentTime")
    LiveData<Integer> countDueCardsLive(long currentTime);

    // ========== Flashcard Retrieval Queries ==========

    @Query("SELECT * FROM flashcards WHERE id = :flashcardId LIMIT 1")
    Flashcard getFlashcardById(long flashcardId);

    @Query("SELECT * FROM flashcards WHERE title = :title COLLATE NOCASE LIMIT 1")
    Flashcard getFlashcardByTitle(String title);

    // ========== Bulk Flashcard Queries ==========

    @Query("SELECT * FROM flashcards")
    LiveData<List<Flashcard>> getAllFlashcardsLive();

    @Query("SELECT * FROM flashcards LIMIT :limit OFFSET :offset")
    List<Flashcard> getFlashcardsWithPagination(int limit, int offset);

    // ========== Status-Based Queries ==========

    @Query("SELECT * FROM flashcards WHERE status = :status")
    List<Flashcard> getFlashcardsByStatus(String status);

    @Query("SELECT * FROM flashcards WHERE status = :status ORDER BY nextReview ASC LIMIT :limit OFFSET :offset")
    List<Flashcard> getFlashcardsByStatusWithPagination(String status, int limit, int offset);

    // ========== Time-Based Queries ==========

    @Query("SELECT * FROM flashcards WHERE nextReview <= :currentTime ORDER BY nextReview ASC")
    List<Flashcard> getDueFlashcards(long currentTime);

    @Query("SELECT * FROM flashcards WHERE status = 'new' AND nextReview <= :currentTime")
    List<Flashcard> getNewFlashcardsReadyForReview(long currentTime);

    @Query("SELECT * FROM flashcards WHERE status = 'learning' AND nextReview <= :currentTime")
    List<Flashcard> getDueLearningFlashcards(long currentTime);

    @Query("SELECT * FROM flashcards WHERE status = 'review' AND nextReview <= :currentTime")
    List<Flashcard> getDueReviewFlashcards(long currentTime);

    // ========== Counting Methods ==========

    // LiveData versions for real-time updates
    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'new' AND nextReview <= :currentTime")
    LiveData<Integer> countNewCardsLive(long currentTime);

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'learning'")
    LiveData<Integer> countTotalLearningCards();

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'learning' AND nextReview <= :currentTime")
    LiveData<Integer> countDueLearningCards(long currentTime);

    @Query("SELECT COUNT(*) FROM flashcards WHERE status = 'review' AND nextReview <= :currentTime")
    LiveData<Integer> countDueReviewCards(long currentTime);

    // Regular versions for transactions
    @Transaction
    @Query("SELECT " +
            "(SELECT COUNT(*) FROM flashcards WHERE status = 'new') as newCount, " +
            "(SELECT COUNT(*) FROM flashcards WHERE status = 'learning') as learningCount, " +
            "(SELECT COUNT(*) FROM flashcards WHERE status = 'review') as reviewCount")
    FlashcardCounts getTotalFlashcardCounts();

    @Transaction
    @Query("SELECT " +
            "(SELECT COUNT(*) FROM flashcards WHERE status = 'new' AND nextReview <= :currentTime) as newCount, " +
            "(SELECT COUNT(*) FROM flashcards WHERE status = 'learning' AND nextReview <= :currentTime) as learningCount, " +
            "(SELECT COUNT(*) FROM flashcards WHERE status = 'review' AND nextReview <= :currentTime) as reviewCount")
    FlashcardCounts getDueFlashcardCounts(long currentTime);

    // ========== Status Updates ==========

    @Transaction
    @Query("UPDATE flashcards SET status = :newStatus WHERE id = :id")
    void updateFlashcardStatus(long id, String newStatus);
}




/**
 * Helper class for combined statistics results
 */
class FlashcardCounts {
    public int newCount;
    public int learningCount;
    public int reviewCount;

    public FlashcardCounts() {
        // Default constructor for Room
    }

    public FlashcardCounts(int newCount, int learningCount, int reviewCount) {
        this.newCount = newCount;
        this.learningCount = learningCount;
        this.reviewCount = reviewCount;
    }

    public int getTotal() {
        return newCount + learningCount + reviewCount;
    }
}