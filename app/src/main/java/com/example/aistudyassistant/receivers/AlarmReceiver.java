package com.example.aistudyassistant.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.aistudyassistant.utils.Constants;
import com.example.aistudyassistant.utils.NotificationHelper;

/**
 * AlarmReceiver is triggered by AlarmManager when a study session time arrives.
 * It displays a notification to the user.
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int scheduleId = intent.getIntExtra(Constants.ALARM_SCHEDULE_ID, 0);
        String title = intent.getStringExtra(Constants.ALARM_SCHEDULE_TITLE);
        String description = intent.getStringExtra("schedule_description");

        if (title == null) title = "Study Session";

        NotificationHelper.showStudyReminder(context, scheduleId, title, description);
    }
}
