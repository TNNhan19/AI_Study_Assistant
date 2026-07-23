package com.example.aistudyassistant.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.aistudyassistant.models.SearchResult;

import org.junit.Test;

import java.util.List;

public class SearchRepositoryTest {

    @Test
    public void parsesRankedRpcResults() {
        String response = "[{"
                + "\"result_id\":\"11111111-1111-1111-1111-111111111111\","
                + "\"result_title\":\"Android Notes\","
                + "\"result_subtitle\":\"Lifecycle\","
                + "\"result_type\":\"NOTE\","
                + "\"result_score\":92.5"
                + "}]";

        List<SearchResult> results =
                SearchRepository.parseRpcResults(response);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(SearchResult.Type.NOTE, results.get(0).getType());
        assertEquals("Android Notes", results.get(0).getTitle());
    }

    @Test
    public void acceptsEmptyRpcResultArray() {
        List<SearchResult> results =
                SearchRepository.parseRpcResults("[]");

        assertNotNull(results);
        assertEquals(0, results.size());
    }

    @Test
    public void rejectsPostgrestErrorObject() {
        assertNull(SearchRepository.parseRpcResults(
                "{\"code\":\"PGRST202\",\"message\":\"missing function\"}"));
    }
}
