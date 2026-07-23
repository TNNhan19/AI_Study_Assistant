package com.example.aistudyassistant.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NoteSearchUtilsTest {

    @Test
    public void matchesTitleIgnoringVietnameseAccents() {
        assertTrue(NoteSearchUtils.matches(
                "xac suat", "Xác suất thống kê", "", ""));
    }

    @Test
    public void matchesContentWithSmallTypingMistake() {
        assertTrue(NoteSearchUtils.matches(
                "thuta toan", "", "Các thuật toán tìm kiếm", ""));
    }

    @Test
    public void matchesLinkedDocumentName() {
        assertTrue(NoteSearchUtils.matches(
                "mobile programming", "", "", "Mobile Programming.pdf"));
    }

    @Test
    public void requiresEverySearchTermToMatch() {
        assertFalse(NoteSearchUtils.matches(
                "android database", "Android lifecycle", "Activity states", ""));
    }
}
