package com.example.myapplication.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.myapplication.ai.RecommendationEngine;
import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.model.UserProfile;
import com.example.myapplication.repository.UserProfileRepository;
import com.example.myapplication.util.NotificationHelper;

import java.util.ArrayList;
import java.util.List;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);

        UserProfileRepository userProfileRepository = new UserProfileRepository(context);
        UserProfile profile = userProfileRepository.getProfileSync();

        List<Opportunity> all = db.opportunityDao().getAllOpportunitiesSync();
        if (all == null) {
            all = new ArrayList<>();
        }

        List<Opportunity> recommended = RecommendationEngine.getRecommended(all, profile);
        int topCount = Math.min(3, recommended.size());

        if (topCount <= 0) {
            return Result.success();
        }

        StringBuilder titles = new StringBuilder();
        for (int i = 0; i < topCount; i++) {
            if (i > 0) {
                titles.append(", ");
            }
            String title = recommended.get(i).getTitle();
            titles.append(title == null || title.trim().isEmpty() ? "Opportunity" : title);
        }

        String message = "Top opportunities for you today: " + titles;
        NotificationHelper.showChannelNotification(
            context,
            "opportunities",
            "Daily Opportunities",
            message
        );

        return Result.success();
    }
}
