package com.example.myapplication.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.myapplication.model.UserProfile;
import com.example.myapplication.repository.UserProfileRepository;

public class UserProfileViewModel extends AndroidViewModel {

    private final UserProfileRepository repository;
    private final LiveData<UserProfile> profile;

    public UserProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new UserProfileRepository(application.getApplicationContext());
        profile = repository.getProfile();
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public void saveProfile(UserProfile userProfile) {
        repository.saveProfile(userProfile);
    }
}
