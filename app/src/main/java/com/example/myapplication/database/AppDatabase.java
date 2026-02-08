package com.example.myapplication.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.myapplication.model.Application;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.InterviewQuestion;
import com.example.myapplication.model.Opportunity;
import com.example.myapplication.model.User;
import com.example.myapplication.model.UserPreferences;

@Database(entities = {
    Opportunity.class,
    User.class,
    UserPreferences.class,
    Application.class,
    InterviewQuestion.class,
    InterviewProgress.class
}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    
    private static AppDatabase instance;
    
    public abstract OpportunityDao opportunityDao();
    public abstract ApplicationDao applicationDao();
    public abstract UserDao userDao();
    public abstract InterviewQuestionDao interviewQuestionDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "opportunity_hub_database"
            )
            .fallbackToDestructiveMigration()
            .build();
        }
        return instance;
    }
}
