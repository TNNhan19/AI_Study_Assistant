package com.example.aistudyassistant.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NoteSearchUtils {

    private NoteSearchUtils() {
    }

    public static boolean matches(String query, String... searchableValues) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) return true;

        StringBuilder searchableText = new StringBuilder();
        for (String value : searchableValues) {
            String normalizedValue = normalize(value);
            if (!normalizedValue.isEmpty()) {
                if (searchableText.length() > 0) searchableText.append(' ');
                searchableText.append(normalizedValue);
            }
        }

        String combinedText = searchableText.toString();
        if (combinedText.contains(normalizedQuery)) return true;

        String[] candidateTokens = combinedText.split(" ");
        for (String queryToken : normalizedQuery.split(" ")) {
            if (queryToken.isEmpty()) continue;

            boolean tokenMatched = false;
            for (String candidateToken : candidateTokens) {
                if (isCloseMatch(queryToken, candidateToken)) {
                    tokenMatched = true;
                    break;
                }
            }
            if (!tokenMatched) return false;
        }
        return true;
    }

    static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(
                value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replace("đ", "d")
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    private static boolean isCloseMatch(String queryToken, String candidateToken) {
        if (candidateToken.isEmpty()) return false;
        if (candidateToken.contains(queryToken)) return true;
        if (queryToken.length() < 3) return false;

        int allowedDistance;
        if (queryToken.length() <= 4) {
            allowedDistance = 1;
        } else if (queryToken.length() <= 8) {
            allowedDistance = 2;
        } else {
            allowedDistance = 3;
        }

        if (Math.abs(queryToken.length() - candidateToken.length())
                > allowedDistance) {
            return false;
        }
        return levenshteinDistance(queryToken, candidateToken)
                <= allowedDistance;
    }

    private static int levenshteinDistance(String first, String second) {
        List<Integer> previousRow = new ArrayList<>();
        for (int i = 0; i <= second.length(); i++) {
            previousRow.add(i);
        }

        for (int i = 1; i <= first.length(); i++) {
            List<Integer> currentRow = new ArrayList<>();
            currentRow.add(i);
            for (int j = 1; j <= second.length(); j++) {
                int insert = currentRow.get(j - 1) + 1;
                int delete = previousRow.get(j) + 1;
                int replace = previousRow.get(j - 1)
                        + (first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1);
                currentRow.add(Math.min(Math.min(insert, delete), replace));
            }
            previousRow = currentRow;
        }
        return previousRow.get(second.length());
    }
}
