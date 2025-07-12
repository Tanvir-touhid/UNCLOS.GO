package com.example.user.unclosgo;

public class ProgressFeedback {
    public static String getUnifiedFeedback(int articlesRead, int totalArticles,
                                            int cardsMastered, int totalCards) {

        float articleRatio = (float) articlesRead / totalArticles;
        float cardRatio = (float) cardsMastered / totalCards;
        float overallProgress = (articleRatio + cardRatio) / 2;

        // Base progress analysis
        if (overallProgress == 0) {
            return "🚀 \nLaunching your learning journey! Start with\n an article or flashcards!";
        } else if (overallProgress < 0.3) {
            return "🌱 \nYou're planting knowledge seeds - they'll\n grow with consistency!";
        } else if (overallProgress < 0.6) {
            return "📈 \nMaking solid progress! The middle\n is where magic happens!";
        } else if (overallProgress < 0.9) {
            return "✨ You're in the mastery zone - keep\n this momentum going!";
        }

        // Special cases
        if (articleRatio > 0.9 && cardRatio > 0.9) {
            return "🏆 \nLanguage champion! You're mastering\n both reading and vocabulary!";
        }
        if (articleRatio > cardRatio + 0.3) {
            return "📚 \nReading powerhouse! Try applying\n this to flashcards now!";
        }
        if (cardRatio > articleRatio + 0.3) {
            return "🧠 \nFlashcard expert! How about practicing\n with an article?";
        }
        if (articlesRead == 0 && cardsMastered > 0) {
            return "🔍 \nGreat flashcards progress! Articles will\n complete the picture!";
        }
        if (cardsMastered == 0 && articlesRead > 0) {
            return "👂 \nYou're training your comprehension - now train\n recall with flashcards!";
        }

        // Default encouragement
        return "🌟 " + getRandomEncouragement();
    }

    private static String getRandomEncouragement() {
        String[] encouragements = {
                "Every study session moves you forward!",
                "Consistency is your superpower!",
                "Your future self will thank you!",
                "Progress compounds - keep going!",
                "This is how fluency is built!"
        };
        return encouragements[(int) (Math.random() * encouragements.length)];
    }
}