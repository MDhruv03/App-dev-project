package com.example.myapplication.repository;

import com.example.myapplication.model.Application;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApplicationSyncHelperTest {

    @Test
    public void buildStableKey_isCaseAndWhitespaceInsensitive() {
        Application application = new Application();
        application.setOpportunityId(12);
        application.setCompany("  OpenAI  ");
        application.setPosition(" Android Engineer ");

        String key = ApplicationSyncHelper.buildStableKey(application);

        assertEquals("12|openai|android engineer", key);
    }

    @Test
    public void mergeLocalStateIntoRemote_preservesLocalStatusAndNotes() {
        Application local = new Application();
        local.setId(33);
        local.setUserId(1);
        local.setOpportunityId(12);
        local.setCompany("OpenAI");
        local.setPosition("Android Engineer");
        local.setStatus("interview");
        local.setNotes("Strong referral");
        local.setInterviewNotes("Round 2 pending");
        local.setSavedAt(new Date(1000));

        Application remote = new Application();
        remote.setId(77);
        remote.setUserId(99);
        remote.setOpportunityId(12);
        remote.setCompany(" openai ");
        remote.setPosition("android engineer");
        remote.setStatus("saved");

        List<Application> localItems = new ArrayList<>();
        localItems.add(local);

        List<Application> remoteItems = new ArrayList<>();
        remoteItems.add(remote);

        ApplicationSyncHelper.mergeLocalStateIntoRemote(1, remoteItems, localItems);

        assertEquals(1, remote.getUserId());
        assertEquals(33, remote.getId());
        assertEquals("interview", remote.getStatus());
        assertEquals("Strong referral", remote.getNotes());
        assertEquals("Round 2 pending", remote.getInterviewNotes());
        assertTrue(remote.getSavedAt() != null);
    }
}
