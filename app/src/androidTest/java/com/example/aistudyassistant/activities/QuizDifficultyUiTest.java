package com.example.aistudyassistant.activities;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.aistudyassistant.utils.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class QuizDifficultyUiTest {

    private ActivityScenario<QuizActivity> scenario;

    @Before
    public void launchQuizWithoutExistingSet() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, QuizActivity.class);
        intent.putExtra(Constants.EXTRA_DOCUMENT_ID, "ui-test-document");
        intent.putExtra(Constants.EXTRA_DOCUMENT_NAME, "Tài liệu kiểm thử");
        intent.putExtra(Constants.EXTRA_DOCUMENT_URL, "ui-test/document.txt");
        intent.putExtra(Constants.EXTRA_DOCUMENT_TYPE, "txt");
        scenario = ActivityScenario.launch(intent);
    }

    @After
    public void closeActivity() {
        if (scenario != null) scenario.close();
    }

    @Test
    public void difficultyDialog_displaysAllLevelsAndDefaultsToMedium() {
        onView(withText("Select difficulty")).check(matches(isDisplayed()));
        onView(withText("Easy")).check(matches(isDisplayed()));
        onView(withText("Medium"))
                .check(matches(isDisplayed()))
                .check(matches(isChecked()));
        onView(withText("Hard")).check(matches(isDisplayed()));
        onView(withText("Generate quiz")).check(matches(isDisplayed()));
    }

    @Test
    public void difficultyDialog_allowsChoosingEasyAndHard() {
        onView(withText("Easy")).perform(click()).check(matches(isChecked()));
        onView(withText("Hard")).perform(click()).check(matches(isChecked()));
    }
}
