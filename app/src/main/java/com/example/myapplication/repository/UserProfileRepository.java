package com.example.myapplication.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.example.myapplication.database.AppDatabase;
import com.example.myapplication.database.UserProfileDao;
import com.example.myapplication.model.UserProfile;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileRepository {

    private final UserProfileDao userProfileDao;
    private final ExecutorService executorService;

    public UserProfileRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context.getApplicationContext());
        userProfileDao = database.getUserProfileDao();
        executorService = Executors.newSingleThreadExecutor();
    }

    public void saveProfile(UserProfile profile) {
        executorService.execute(() -> userProfileDao.insertOrReplace(profile));
    }

    public LiveData<UserProfile> getProfile() {
        return userProfileDao.getProfile();
    }

    public UserProfile getProfileSync() {
        return userProfileDao.getProfileSync();
    }
}
