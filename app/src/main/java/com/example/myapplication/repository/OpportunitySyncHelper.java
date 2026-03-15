package com.example.myapplication.repository;

import com.example.myapplication.model.Opportunity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OpportunitySyncHelper {

    private OpportunitySyncHelper() {
    }

    public static void mergeLocalStateIntoRemote(List<Opportunity> remoteItems, List<Opportunity> localItems) {
        if (remoteItems == null || remoteItems.isEmpty()) {
            return;
        }

        Map<String, Opportunity> localByKey = new HashMap<>();
        if (localItems != null) {
            for (Opportunity local : localItems) {
                localByKey.put(buildStableKey(local), local);
            }
        }

        for (Opportunity remote : remoteItems) {
            Opportunity local = localByKey.get(buildStableKey(remote));
            if (local != null) {
                remote.setSaved(local.isSaved());
                remote.setApplied(local.isApplied());
            }
        }
    }

    public static String buildStableKey(Opportunity opportunity) {
        String company = opportunity.getCompany() == null ? "" : opportunity.getCompany().trim().toLowerCase(Locale.ROOT);
        String title = opportunity.getTitle() == null ? "" : opportunity.getTitle().trim().toLowerCase(Locale.ROOT);
        String type = opportunity.getType() == null ? "" : opportunity.getType().trim().toLowerCase(Locale.ROOT);
        return company + "|" + title + "|" + type;
    }
}
