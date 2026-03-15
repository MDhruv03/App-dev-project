package com.example.myapplication.ui.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.RoadmapAdapter;
import com.example.myapplication.model.InterviewProgress;
import com.example.myapplication.model.RoadmapTopic;
import com.example.myapplication.model.UserProfile;
import com.example.myapplication.repository.InterviewRepository;
import com.example.myapplication.repository.UserProfileRepository;
import com.example.myapplication.util.RoadmapGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoadmapFragment extends Fragment {

    private static final int DEFAULT_USER_ID = 1;
    private static final String PREFS_NAME = "roadmap_preferences";

    private UserProfileRepository userProfileRepository;
    private InterviewRepository interviewRepository;

    private UserProfile currentProfile;
    private List<InterviewProgress> currentProgress = new ArrayList<>();

    private SharedPreferences prefs;

    private View bannerIncomplete;
    private TextView tvBannerMessage;
    private RecyclerView rvHighPriority;
    private RecyclerView rvMediumPriority;
    private RecyclerView rvGrowthGoals;

    private RoadmapAdapter highAdapter;
    private RoadmapAdapter mediumAdapter;
    private RoadmapAdapter growthAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_roadmap, container, false);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);
        userProfileRepository = new UserProfileRepository(requireContext());
        interviewRepository = new InterviewRepository(requireContext());

        initViews(view);
        setupRecyclerViews();
        loadData();

        return view;
    }

    private void initViews(View root) {
        bannerIncomplete = root.findViewById(R.id.bannerIncomplete);
        tvBannerMessage = root.findViewById(R.id.tvBannerMessage);
        rvHighPriority = root.findViewById(R.id.rvHighPriority);
        rvMediumPriority = root.findViewById(R.id.rvMediumPriority);
        rvGrowthGoals = root.findViewById(R.id.rvGrowthGoals);
    }

    private void setupRecyclerViews() {
        highAdapter = new RoadmapAdapter(createListener());
        mediumAdapter = new RoadmapAdapter(createListener());
        growthAdapter = new RoadmapAdapter(createListener());

        rvHighPriority.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHighPriority.setAdapter(highAdapter);
        rvHighPriority.setNestedScrollingEnabled(false);

        rvMediumPriority.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMediumPriority.setAdapter(mediumAdapter);
        rvMediumPriority.setNestedScrollingEnabled(false);

        rvGrowthGoals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGrowthGoals.setAdapter(growthAdapter);
        rvGrowthGoals.setNestedScrollingEnabled(false);
    }

    private RoadmapAdapter.OnTopicActionListener createListener() {
        return new RoadmapAdapter.OnTopicActionListener() {
            @Override
            public void onResourceClick(RoadmapTopic topic) {
                openResource(topic);
            }

            @Override
            public void onCompletionChanged(RoadmapTopic topic, boolean completed) {
                String key = "roadmap_" + topic.getId() + "_done";
                prefs.edit().putBoolean(key, completed).apply();
            }
        };
    }

    private void loadData() {
        userProfileRepository.getProfile().observe(getViewLifecycleOwner(), profile -> {
            currentProfile = profile;
            updateIncompleteBanner(profile);
            renderRoadmap();
        });

        interviewRepository.getUserProgress(DEFAULT_USER_ID, progress -> {
            currentProgress = progress == null ? Collections.emptyList() : progress;
            if (isAdded()) {
                requireActivity().runOnUiThread(this::renderRoadmap);
            }
        });
    }

    private void updateIncompleteBanner(UserProfile profile) {
        String skills = profile == null ? "" : safe(profile.getSkills());
        boolean incomplete = skills.isEmpty();
        bannerIncomplete.setVisibility(incomplete ? View.VISIBLE : View.GONE);
        if (incomplete) {
            tvBannerMessage.setText("Complete your profile to get personalized recommendations");
        }
    }

    private void renderRoadmap() {
        List<RoadmapTopic> roadmap = RoadmapGenerator.generateRoadmap(currentProfile, currentProgress);
        for (RoadmapTopic topic : roadmap) {
            String key = "roadmap_" + topic.getId() + "_done";
            topic.setCompleted(prefs.getBoolean(key, false));
        }

        List<RoadmapTopic> high = new ArrayList<>();
        List<RoadmapTopic> medium = new ArrayList<>();
        List<RoadmapTopic> low = new ArrayList<>();

        for (RoadmapTopic topic : roadmap) {
            if (topic.getPriority() == 1) {
                high.add(topic);
            } else if (topic.getPriority() == 2) {
                medium.add(topic);
            } else {
                low.add(topic);
            }
        }

        highAdapter.submitList(high);
        mediumAdapter.submitList(medium);
        growthAdapter.submitList(low);
    }

    private void openResource(RoadmapTopic topic) {
        String resources = safe(topic.getResources());
        if (resources.isEmpty()) {
            Toast.makeText(requireContext(), "No resource found", Toast.LENGTH_SHORT).show();
            return;
        }

        String firstUrl = resources.split(",")[0].trim();
        if (firstUrl.isEmpty()) {
            Toast.makeText(requireContext(), "No valid link found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(firstUrl));
            startActivity(browserIntent);
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
