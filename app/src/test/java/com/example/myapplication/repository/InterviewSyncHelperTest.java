package com.example.myapplication.repository;

import com.example.myapplication.model.InterviewQuestion;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InterviewSyncHelperTest {

    @Test
    public void buildStableKey_isCaseAndWhitespaceInsensitive() {
        InterviewQuestion question = new InterviewQuestion();
        question.setDomain(" SDE ");
        question.setTopic(" DSA ");
        question.setQuestion(" Explain time complexity? ");

        String key = InterviewSyncHelper.buildStableKey(question);

        assertEquals("sde|dsa|explain time complexity?", key);
    }

    @Test
    public void mergeLocalQuestionStateIntoRemote_preservesProgressFields() {
        InterviewQuestion local = new InterviewQuestion();
        local.setId(42);
        local.setDomain("SDE");
        local.setTopic("DSA");
        local.setQuestion("Explain time complexity?");
        local.setTimesAsked(7);
        local.setAverageScore(86.5);
        local.setIsAnswered(true);

        InterviewQuestion remote = new InterviewQuestion();
        remote.setId(101);
        remote.setDomain("sde");
        remote.setTopic("dsa");
        remote.setQuestion("explain time complexity?");
        remote.setTimesAsked(0);
        remote.setAverageScore(0.0);
        remote.setIsAnswered(false);

        List<InterviewQuestion> localItems = new ArrayList<>();
        localItems.add(local);

        List<InterviewQuestion> remoteItems = new ArrayList<>();
        remoteItems.add(remote);

        InterviewSyncHelper.mergeLocalQuestionStateIntoRemote(remoteItems, localItems);

        assertEquals(42, remote.getId());
        assertEquals(7, remote.getTimesAsked());
        assertEquals(86.5, remote.getAverageScore(), 0.01);
        assertTrue(remote.getIsAnswered());
    }
}
