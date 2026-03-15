package com.example.myapplication.repository;

import com.example.myapplication.model.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ApplicationSyncHelper {

    private ApplicationSyncHelper() {
    }

    public static void mergeLocalStateIntoRemote(int userId, List<Application> remoteApplications, List<Application> localApplications) {
        if (remoteApplications == null || remoteApplications.isEmpty()) {
            return;
        }

        Map<String, Application> localByKey = new HashMap<>();
        if (localApplications != null) {
            for (Application local : localApplications) {
                localByKey.put(buildStableKey(local), local);
            }
        }

        for (Application remote : remoteApplications) {
            remote.setUserId(userId);
            Application local = localByKey.get(buildStableKey(remote));
            if (local == null) {
                continue;
            }

            remote.setId(local.getId());
            remote.setStatus(local.getStatus());
            remote.setNotes(local.getNotes());
            remote.setInterviewNotes(local.getInterviewNotes());
            remote.setInterviewDate(local.getInterviewDate());
            remote.setInterviewLocation(local.getInterviewLocation());
            remote.setInterviewReminderSet(local.isInterviewReminderSet());
            remote.setSavedAt(local.getSavedAt());
            remote.setAppliedAt(local.getAppliedAt());
            remote.setAppliedDate(local.getAppliedDate());
            remote.setResponseDate(local.getResponseDate());
            remote.setInterviewScheduledAt(local.getInterviewScheduledAt());
            remote.setStatusUpdatedAt(local.getStatusUpdatedAt());
        }
    }

    public static String buildStableKey(Application application) {
        String company = application.getCompany() == null ? "" : application.getCompany().trim().toLowerCase(Locale.ROOT);
        String position = application.getPosition() == null ? "" : application.getPosition().trim().toLowerCase(Locale.ROOT);
        return application.getOpportunityId() + "|" + company + "|" + position;
    }
}
