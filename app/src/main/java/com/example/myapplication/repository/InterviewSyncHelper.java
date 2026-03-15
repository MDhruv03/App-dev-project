package com.example.myapplication.repository;

import com.example.myapplication.model.InterviewQuestion;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InterviewSyncHelper {

    private InterviewSyncHelper() {
    }

    public static void mergeLocalQuestionStateIntoRemote(List<InterviewQuestion> remoteQuestions, List<InterviewQuestion> localQuestions) {
        if (remoteQuestions == null || remoteQuestions.isEmpty()) {
            return;
        }

        Map<String, InterviewQuestion> localByKey = new HashMap<>();
        if (localQuestions != null) {
            for (InterviewQuestion local : localQuestions) {
                localByKey.put(buildStableKey(local), local);
            }
        }

        for (InterviewQuestion remote : remoteQuestions) {
            InterviewQuestion local = localByKey.get(buildStableKey(remote));
            if (local == null) {
                continue;
            }

            remote.setId(local.getId());
            remote.setTimesAsked(local.getTimesAsked());
            remote.setAverageScore(local.getAverageScore());
            remote.setIsAnswered(local.getIsAnswered());
        }
    }

    public static String buildStableKey(InterviewQuestion question) {
        String domain = question.getDomain() == null ? "" : question.getDomain().trim().toLowerCase(Locale.ROOT);
        String topic = question.getTopic() == null ? "" : question.getTopic().trim().toLowerCase(Locale.ROOT);
        String text = question.getQuestion() == null ? "" : question.getQuestion().trim().toLowerCase(Locale.ROOT);
        return domain + "|" + topic + "|" + text;
    }
}
