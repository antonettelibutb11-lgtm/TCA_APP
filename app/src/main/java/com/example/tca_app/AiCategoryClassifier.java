package com.example.tca_app;

/**
 * Note: This component is a deterministic rule-based keyword matcher (Regex / String Tokenizer),
 * not a Machine Learning or Neural AI classifier. It categorizes posts based on curated keyword dictionary matching.
 */
public class AiCategoryClassifier {

    /**
     * Automatically classifies the category of a post based on its text content
     * if the user selects 'Smart AI Auto-Detect' or if automatic matching is requested.
     */
    public static String classifyCategory(String content, String selectedCategory) {
        if (selectedCategory != null && !selectedCategory.contains("Auto-Categorize") && !selectedCategory.equals("General")) {
            return selectedCategory;
        }

        if (content == null || content.trim().isEmpty()) {
            return "Broadcast";
        }

        String lower = content.toLowerCase();

        // 1. ACADEMICS KEYWORDS
        if (containsAny(lower, "academic", "academics", "homework", "lecture", "assignment",
                "thesis", "professor", "class", "subject", "study", "exam", "quiz", "gpa")) {
            return "Academics";
        }

        // 2. SPORTS KEYWORDS
        if (containsAny(lower, "sports", "basketball", "volleyball", "badminton", "football",
                "soccer", "athletics", "intramurals", "game", "match", "league", "champion")) {
            return "Sports";
        }

        // 3. HOROSCOPES KEYWORDS
        if (containsAny(lower, "horoscope", "zodiac", "astrology", "leo", "scorpio", "aries",
                "taurus", "gemini", "cancer", "virgo", "libra", "sagittarius", "capricorn",
                "aquarius", "pisces", "future prediction", "lucky number", "star sign")) {
            return "Horoscopes";
        }

        // 4. LITERATURE KEYWORDS
        if (containsAny(lower, "poem", "poetry", "tula", "balak", "short story", "essay",
                "novel", "excerpt", "creative writing", "stanza", "verse", "literature",
                "chapter", "prose", "writer")) {
            return "Literature";
        }

        // 5. EVENTS KEYWORDS
        if (containsAny(lower, "event", "ceremony", "expo", "cultural night",
                "party", "gathering", "concert", "festival", "tournament", "celebration", "program")) {
            return "Events";
        }

        // 6. UPDATES KEYWORDS
        if (containsAny(lower, "update", "schedule", "midterm", "registrar",
                "advisory", "deadline", "tuition", "notice", "clearance", "enrollment", "grading")) {
            return "Updates";
        }

        // 7. BROADCAST KEYWORDS
        if (containsAny(lower, "broadcast", "airwaves", "radio", "live", "breaking news",
                "official release", "podcast", "press release", "bulletin")) {
            return "Broadcast";
        }

        return "Broadcast";
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
