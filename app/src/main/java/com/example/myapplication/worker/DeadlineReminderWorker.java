package com.example.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.util.NotificationHelper;

import java.util.List;

public class DeadlineReminderWorker extends Worker {

    public DeadlineReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);

        List<Opportunity> saved = db.opportunityDao().getSavedOpportunities();
        int count = saved == null ? 0 : saved.size();

        String message = "You have " + count + " saved opportunities — don't miss your deadlines!";
        NotificationHelper.showChannelNotification(
            context,
            "reminders",
            "Deadline Reminder",
            message
        );

        return Result.success();
    }
}
