package com.example.myapplication.repository;

import com.example.myapplication.model.Opportunity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpportunitySyncHelperTest {

    @Test
    public void buildStableKey_isCaseAndWhitespaceInsensitive() {
        Opportunity opportunity = new Opportunity();
        opportunity.setCompany("  OpenAI  ");
        opportunity.setTitle(" Android Engineer ");
        opportunity.setType(" Job ");

        String key = OpportunitySyncHelper.buildStableKey(opportunity);

        assertEquals("openai|android engineer|job", key);
    }

    @Test
    public void mergeLocalStateIntoRemote_copiesSavedAndAppliedFlags() {
        Opportunity local = new Opportunity();
        local.setCompany("OpenAI");
        local.setTitle("Android Engineer");
        local.setType("job");
        local.setSaved(true);
        local.setApplied(true);

        Opportunity remote = new Opportunity();
        remote.setCompany("openai");
        remote.setTitle("android engineer");
        remote.setType("JOB");
        remote.setSaved(false);
        remote.setApplied(false);

        List<Opportunity> localItems = new ArrayList<>();
        localItems.add(local);

        List<Opportunity> remoteItems = new ArrayList<>();
        remoteItems.add(remote);

        OpportunitySyncHelper.mergeLocalStateIntoRemote(remoteItems, localItems);

        assertTrue(remote.isSaved());
        assertTrue(remote.isApplied());
    }

    @Test
    public void mergeLocalStateIntoRemote_doesNotModifyUnmatchedItems() {
        Opportunity local = new Opportunity();
        local.setCompany("OpenAI");
        local.setTitle("Android Engineer");
        local.setType("job");
        local.setSaved(true);
        local.setApplied(true);

        Opportunity remote = new Opportunity();
        remote.setCompany("Another Co");
        remote.setTitle("Backend Engineer");
        remote.setType("job");
        remote.setSaved(false);
        remote.setApplied(false);

        List<Opportunity> localItems = new ArrayList<>();
        localItems.add(local);

        List<Opportunity> remoteItems = new ArrayList<>();
        remoteItems.add(remote);

        OpportunitySyncHelper.mergeLocalStateIntoRemote(remoteItems, localItems);

        assertFalse(remote.isSaved());
        assertFalse(remote.isApplied());
    }
}
