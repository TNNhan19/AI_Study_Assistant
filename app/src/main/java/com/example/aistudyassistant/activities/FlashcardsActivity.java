package com.example.aistudyassistant.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.GeminiClient;
import com.example.aistudyassistant.models.Flashcard;
import com.example.aistudyassistant.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

public class FlashcardsActivity extends AppCompatActivity {

    private TextView tvCardCount, tvFrontText, tvBackText;
    private LinearLayout cardFront, cardBack, layoutLoading, layoutProgressDots;
    private LinearLayout flipCardContainer;
    private MaterialButton btnPrev, btnNext, btnKnown, btnUnknown, btnGenerate;
    private ImageButton btnBack;

    private List<Flashcard> flashcards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShowingFront = true;

    private String documentId;
    private String documentName;
    private String documentUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);

        initViews();
        setupClickListeners();
        loadFlashcards();
    }

    private void initViews() {
        tvCardCount = findViewById(R.id.tv_card_count);
        tvFrontText = findViewById(R.id.tv_front_text);
        tvBackText = findViewById(R.id.tv_back_text);
        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        flipCardContainer = findViewById(R.id.flip_card_container);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutProgressDots = findViewById(R.id.layout_progress_dots);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnKnown = findViewById(R.id.btn_known);
        btnUnknown = findViewById(R.id.btn_unknown);
        btnGenerate = findViewById(R.id.btn_generate);
        btnBack = findViewById(R.id.btn_back);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnGenerate.setOnClickListener(v -> generateFlashcards());

        // Flip card on tap
        flipCardContainer.setOnClickListener(v -> flipCard());

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                isShowingFront = true;
                displayCard(currentIndex);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < flashcards.size() - 1) {
                currentIndex++;
                isShowingFront = true;
                displayCard(currentIndex);
            } else {
                Toast.makeText(this, "You've reviewed all flashcards! 🎉", Toast.LENGTH_SHORT).show();
            }
        });

        btnKnown.setOnClickListener(v -> {
            if (!flashcards.isEmpty()) {
                flashcards.get(currentIndex).setKnown(true);
                updateProgressDots();
                // Move to next card
                if (currentIndex < flashcards.size() - 1) {
                    currentIndex++;
                    isShowingFront = true;
                    displayCard(currentIndex);
                }
            }
        });

        btnUnknown.setOnClickListener(v -> {
            if (!flashcards.isEmpty()) {
                flashcards.get(currentIndex).setKnown(false);
                updateProgressDots();
            }
        });
    }

    private void loadFlashcards() {
        // TODO: Load from Supabase first
        // If empty, show generate button
    }

    private void generateFlashcards() {
        setLoading(true);

        new Thread(() -> {
            String documentText = "Sample document content."; // TODO: Load actual document
            String responseJson = GeminiClient.getInstance().generateFlashcards(documentText, 15);

            runOnUiThread(() -> {
                setLoading(false);
                if (responseJson != null) {
                    parseFlashcards(responseJson);
                } else {
                    loadDemoFlashcards();
                }
            });
        }).start();
    }

    private void parseFlashcards(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```json")) cleaned = cleaned.substring(7);
            if (cleaned.startsWith("```")) cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3);

            JsonArray array = JsonParser.parseString(cleaned.trim()).getAsJsonArray();
            flashcards.clear();
            for (int i = 0; i < array.size(); i++) {
                JsonObject obj = array.get(i).getAsJsonObject();
                flashcards.add(new Flashcard(
                        obj.get("front").getAsString(),
                        obj.get("back").getAsString()
                ));
            }
            currentIndex = 0;
            displayCard(0);
            updateProgressDots();
        } catch (Exception e) {
            loadDemoFlashcards();
        }
    }

    private void loadDemoFlashcards() {
        flashcards.clear();
        flashcards.add(new Flashcard("What is TCP?",
                "TCP (Transmission Control Protocol) is a connection-oriented protocol that ensures reliable data delivery."));
        flashcards.add(new Flashcard("What is UDP?",
                "UDP (User Datagram Protocol) is a connectionless protocol that is faster but does not guarantee delivery."));
        flashcards.add(new Flashcard("What is the OSI model?",
                "The OSI model is a conceptual framework with 7 layers: Physical, Data Link, Network, Transport, Session, Presentation, Application."));
        displayCard(0);
        updateProgressDots();
    }

    private void displayCard(int index) {
        if (index >= flashcards.size()) return;

        Flashcard card = flashcards.get(index);
        tvFrontText.setText(card.getFront());
        tvBackText.setText(card.getBack());

        // Always show front when navigating
        cardFront.setVisibility(View.VISIBLE);
        cardBack.setVisibility(View.GONE);
        isShowingFront = true;

        tvCardCount.setText("Card " + (index + 1) + " of " + flashcards.size());
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < flashcards.size() - 1);

        updateProgressDots();
    }

    private void flipCard() {
        if (isShowingFront) {
            cardFront.setVisibility(View.GONE);
            cardBack.setVisibility(View.VISIBLE);
            isShowingFront = false;
        } else {
            cardFront.setVisibility(View.VISIBLE);
            cardBack.setVisibility(View.GONE);
            isShowingFront = true;
        }
    }

    private void updateProgressDots() {
        layoutProgressDots.removeAllViews();
        int size = Math.min(flashcards.size(), 10); // Show max 10 dots
        for (int i = 0; i < size; i++) {
            View dot = new View(this);
            int sizeDp = (int) (8 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeDp, sizeDp);
            params.setMargins(4, 0, 4, 0);
            dot.setLayoutParams(params);

            if (i < flashcards.size() && flashcards.get(i).isKnown()) {
                dot.setBackgroundResource(R.drawable.bg_dot_known);
            } else if (i == currentIndex) {
                dot.setBackgroundResource(R.drawable.bg_dot_active);
            } else {
                dot.setBackgroundResource(R.drawable.bg_dot_inactive);
            }
            layoutProgressDots.addView(dot);
        }
    }

    private void setLoading(boolean loading) {
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        flipCardContainer.setVisibility(loading ? View.GONE : View.VISIBLE);
    }
}
