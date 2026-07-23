package com.example.aistudyassistant.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.aistudyassistant.R;
import com.example.aistudyassistant.api.ApiCallback;
import com.example.aistudyassistant.models.Document;
import com.example.aistudyassistant.models.Flashcard;
import com.example.aistudyassistant.repositories.AIContentRepository;
import com.example.aistudyassistant.services.AIProcessingService;
import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.SharedPrefManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class FlashcardsActivity extends AppCompatActivity {

    private TextView tvCardCount, tvFrontText, tvBackText;
    private LinearLayout cardFront, cardBack, layoutLoading, layoutProgressDots;
    private FrameLayout flipCardContainer;
    private MaterialButton btnPrev, btnNext, btnKnown, btnUnknown, btnGenerate;
    private ImageButton btnBack;

    private List<Flashcard> flashcards = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isShowingFront = true;

    private String documentId;
    private String documentName;
    private String documentUrl;
    private String documentType;
    private String topicId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcards);

        documentId = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_ID);
        documentName = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_NAME);
        documentUrl = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_URL);
        documentType = getIntent().getStringExtra(Constants.EXTRA_DOCUMENT_TYPE);
        topicId = getIntent().getStringExtra(Constants.EXTRA_TOPIC_ID);

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
        setLoading(true);
        String userId = SharedPrefManager.getInstance(this).getUserId();
        AIContentRepository.getInstance().getFlashcardsByDocument(
                userId, documentId,
                new ApiCallback<List<Flashcard>>() {
            @Override
            public void onSuccess(List<Flashcard> result) {
                runOnUiThread(() -> {
                    showFlashcards(result);
                    setLoading(false);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(FlashcardsActivity.this,
                            errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void generateFlashcards() {
        setLoading(true);
        AIProcessingService.getInstance(this).generateFlashcards(
                buildDocument(), 15,
                new ApiCallback<List<Flashcard>>() {
            @Override
            public void onSuccess(List<Flashcard> result) {
                saveFlashcards(result);
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(FlashcardsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onWaitingForNetwork() {
                Toast.makeText(FlashcardsActivity.this,
                        "Mất kết nối. Flashcard sẽ tự tạo lại khi có mạng.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveFlashcards(List<Flashcard> generatedFlashcards) {
        AIContentRepository.getInstance().saveFlashcards(
                buildDocument(), generatedFlashcards,
                new ApiCallback<List<Flashcard>>() {
            @Override
            public void onSuccess(List<Flashcard> savedFlashcards) {
                runOnUiThread(() -> {
                    showFlashcards(savedFlashcards);
                    setLoading(false);
                    Toast.makeText(FlashcardsActivity.this,
                            "Flashcards saved", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(FlashcardsActivity.this,
                            errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showFlashcards(List<Flashcard> loadedFlashcards) {
        flashcards.clear();
        flashcards.addAll(loadedFlashcards);
        currentIndex = 0;
        if (flashcards.isEmpty()) {
            tvCardCount.setText("No cards yet");
            layoutProgressDots.removeAllViews();
            return;
        }
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
        if (flashcards.isEmpty()) return;
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
        boolean hasCards = !flashcards.isEmpty();
        layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        flipCardContainer.setVisibility(!loading && hasCards ? View.VISIBLE : View.GONE);
        layoutProgressDots.setVisibility(!loading && hasCards ? View.VISIBLE : View.GONE);
        btnPrev.setEnabled(!loading && hasCards && currentIndex > 0);
        btnNext.setEnabled(!loading && hasCards && currentIndex < flashcards.size() - 1);
        btnKnown.setEnabled(!loading && hasCards);
        btnUnknown.setEnabled(!loading && hasCards);
        btnGenerate.setEnabled(!loading && !hasCards);
        btnGenerate.setText(loading ? "Loading..." : hasCards ? "Generated" : "Generate");
    }

    private Document buildDocument() {
        Document document = new Document();
        document.setId(documentId);
        document.setUserId(SharedPrefManager.getInstance(this).getUserId());
        document.setName(documentName);
        document.setFilePath(documentUrl);
        document.setFileType(documentType);
        document.setTopicId(topicId);
        return document;
    }
}
