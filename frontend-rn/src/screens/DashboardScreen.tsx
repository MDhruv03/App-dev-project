import React, { useMemo, useState } from "react";
import { Pressable, StyleSheet, TextInput, View } from "react-native";
import { useNavigation } from "@react-navigation/native";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
import { ScreenContainer } from "../components/ScreenContainer";
import { StatCard } from "../components/StatCard";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState, type OpportunitySortBy } from "../state/AppState";
import type { OpportunityType } from "../services/opportunitiesService";

const opportunityTypes: OpportunityType[] = ["internship", "job", "hackathon"];
const sortOptions: OpportunitySortBy[] = ["recommended", "deadline", "match", "company"];

function daysUntil(deadlineEpoch: number): number {
  return Math.max(0, Math.ceil((deadlineEpoch - Date.now()) / (24 * 60 * 60 * 1000)));
}

export function DashboardScreen() {
  const { theme } = useAppTheme();
  const navigation = useNavigation();
  const [focusView, setFocusView] = useState(true);
  const {
    profile,
    readiness,
    coding,
    analytics,
    dashboardHeadline,
    isOpportunityFeedUnlocked,
    filteredOpportunities,
    savedOpportunities,
    opportunityFilter,
    setOpportunityQuery,
    toggleOpportunityTypeFilter,
    toggleRemoteOnlyFilter,
    togglePaidOnlyFilter,
    toggleSavedOnlyFilter,
    setOpportunitySortBy,
    clearOpportunityFilters,
    toggleSaveOpportunity,
    applyToOpportunity,
    refreshOpportunities,
    isSyncingOpportunities,
    tracker,
    activityLog,
  } = useAppState();

  const isFirstSession =
    profile.skills.trim().length === 0 &&
    coding.solved === 0 &&
    coding.rating === 0 &&
    analytics.savedCount === 0 &&
    analytics.totalApplications === 0 &&
    analytics.interviewAttempts === 0;

  const statusByOpportunity = useMemo(() => {
    const map = new Map<string, string>();
    for (const item of tracker) {
      map.set(item.opportunityId, item.status);
    }
    return map;
  }, [tracker]);

  return (
    <ScreenContainer
      title="Discover"
      subtitle="Opportunity search, save/apply lifecycle, and live activity"
    >
      <GlassCard>
        <ThemedText variant="label" muted>
          {isFirstSession ? "Welcome" : "Live Signal"}
        </ThemedText>
        <ThemedText variant="title" strong style={{ marginTop: 6 }}>
          {isFirstSession ? "Your workspace is intentionally blank." : dashboardHeadline}
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 8 }}>
          {isFirstSession
            ? "No seeded profile, coding, or tracker data is injected. Start by adding your own signals."
            : "Discover supports focus mode so you can reveal details only when you are ready."}
        </ThemedText>

        <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
          <PrimaryButton
            label="Open Tracker"
            style={{ flex: 1 }}
            onPress={() => {
              navigation.navigate("Tracker" as never);
            }}
          />
          <PrimaryButton
            label="Mock Interview"
            secondary
            style={{ flex: 1 }}
            onPress={() => {
              navigation.navigate("Interview" as never);
            }}
          />
        </View>

        <PrimaryButton
          label={focusView ? "Open Full Discover Workspace" : "Switch To Focus View"}
          secondary
          style={{ marginTop: 10 }}
          onPress={() => {
            setFocusView((prev) => !prev);
          }}
        />
      </GlassCard>

      {isFirstSession && (
        <GlassCard>
          <ThemedText variant="title" strong>
            First Session Checklist
          </ThemedText>
          <ThemedText variant="body" muted style={{ marginTop: 8 }}>
            1. Add profile skills and target roles.
          </ThemedText>
          <ThemedText variant="body" muted style={{ marginTop: 4 }}>
            2. Sync coding handles for real contest/problem signal.
          </ThemedText>
          <ThemedText variant="body" muted style={{ marginTop: 4 }}>
            3. Run one mock interview to generate coaching history.
          </ThemedText>
        </GlassCard>
      )}

      {focusView ? (
        <>
          <View style={styles.metricRow}>
            <StatCard label="Readiness" value={`${readiness}%`} />
            <StatCard label="Active" value={`${analytics.totalApplications}`} tone="success" />
          </View>

          <GlassCard>
            <ThemedText variant="title" strong>
              Focus Mode Is On
            </ThemedText>
            <ThemedText variant="body" muted style={{ marginTop: 8 }}>
              You are seeing a calm view by default. Open the full workspace when you want filters, feed cards, and activity logs.
            </ThemedText>
            <PrimaryButton
              label={
                isSyncingOpportunities
                  ? "Refreshing..."
                  : isOpportunityFeedUnlocked
                  ? "Refresh Curated Feed"
                  : "Load Curated Feed"
              }
              style={{ marginTop: 12 }}
              disabled={isSyncingOpportunities}
              onPress={refreshOpportunities}
            />
          </GlassCard>
        </>
      ) : (
        <>
          <View style={styles.metricRow}>
            <StatCard label="Readiness" value={`${readiness}%`} />
            <StatCard label="Active" value={`${analytics.totalApplications}`} tone="success" />
          </View>

          <View style={styles.metricRow}>
            <StatCard label="Saved" value={`${savedOpportunities.length}`} tone="warning" />
            <StatCard label="CF Rating" value={`${coding.rating}`} />
          </View>

          <GlassCard>
            <ThemedText variant="title" strong>
              Opportunity Radar
            </ThemedText>

            <TextInput
              value={opportunityFilter.query}
              onChangeText={setOpportunityQuery}
              placeholder="Search company, role, skill, location"
              placeholderTextColor={theme.colors.textMuted}
              style={[
                styles.searchInput,
                {
                  borderColor: theme.colors.border,
                  color: theme.colors.text,
                  backgroundColor: theme.colors.cardAlt,
                },
              ]}
            />

            <View style={styles.chipWrap}>
              {opportunityTypes.map((type) => (
                <FilterChip
                  key={type}
                  label={type}
                  selected={opportunityFilter.types.includes(type)}
                  onPress={() => toggleOpportunityTypeFilter(type)}
                />
              ))}
              <FilterChip label="Remote" selected={opportunityFilter.remoteOnly} onPress={toggleRemoteOnlyFilter} />
              <FilterChip label="Paid" selected={opportunityFilter.paidOnly} onPress={togglePaidOnlyFilter} />
              <FilterChip label="Saved" selected={opportunityFilter.savedOnly} onPress={toggleSavedOnlyFilter} />
            </View>

            <View style={styles.sortRow}>
              {sortOptions.map((sortBy) => (
                <FilterChip
                  key={sortBy}
                  label={sortBy}
                  selected={opportunityFilter.sortBy === sortBy}
                  onPress={() => setOpportunitySortBy(sortBy)}
                />
              ))}
            </View>

            <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
              <PrimaryButton
                label={
                  isSyncingOpportunities
                    ? "Refreshing..."
                    : isOpportunityFeedUnlocked
                    ? "Refresh Feed"
                    : "Load Curated Feed"
                }
                style={{ flex: 1 }}
                disabled={isSyncingOpportunities}
                onPress={refreshOpportunities}
              />
              <PrimaryButton
                label="Clear Filters"
                secondary
                style={{ flex: 1 }}
                onPress={clearOpportunityFilters}
              />
            </View>
          </GlassCard>

          <View style={styles.listWrap}>
            {!isOpportunityFeedUnlocked && (
              <GlassCard>
                <ThemedText variant="title" strong>
                  Feed is Curated by You
                </ThemedText>
                <ThemedText variant="body" muted style={{ marginTop: 8 }}>
                  Roles are hidden on startup so you are not overwhelmed. Use "Load Curated Feed" when you are ready.
                </ThemedText>
              </GlassCard>
            )}

            {isOpportunityFeedUnlocked && filteredOpportunities.length === 0 && (
              <GlassCard>
                <ThemedText variant="title" strong>
                  No Matches Yet
                </ThemedText>
                <ThemedText variant="body" muted style={{ marginTop: 8 }}>
                  Try a broader search or clear filters to widen the feed.
                </ThemedText>
              </GlassCard>
            )}

            {filteredOpportunities.slice(0, 8).map((opportunity) => {
              const status = statusByOpportunity.get(opportunity.id);
              const saved = status === "Saved";
              const locked = status === "Applied" || status === "Interview" || status === "Accepted";

              return (
                <GlassCard key={opportunity.id}>
                  <View style={styles.rowTop}>
                    <View style={{ flex: 1, paddingRight: 8 }}>
                      <ThemedText variant="title" strong>
                        {opportunity.title}
                      </ThemedText>
                      <ThemedText variant="body" muted style={{ marginTop: 2 }}>
                        {opportunity.company} - {opportunity.location}
                      </ThemedText>
                    </View>
                    <View style={[styles.badge, { borderColor: theme.colors.border, backgroundColor: theme.colors.cardAlt }]}>
                      <ThemedText variant="label">{opportunity.type}</ThemedText>
                    </View>
                  </View>

                  <ThemedText variant="body" muted style={{ marginTop: 8 }}>
                    Match {opportunity.matchScore}% - Deadline in {daysUntil(opportunity.deadlineEpoch)} day(s) - {opportunity.salaryRange}
                  </ThemedText>

                  <View style={{ marginTop: 8, flexDirection: "row", flexWrap: "wrap", gap: 6 }}>
                    {opportunity.skills.slice(0, 4).map((skill) => (
                      <View
                        key={`${opportunity.id}-${skill}`}
                        style={[styles.skillPill, { borderColor: theme.colors.border, backgroundColor: theme.colors.cardAlt }]}
                      >
                        <ThemedText variant="label" muted>
                          {skill}
                        </ThemedText>
                      </View>
                    ))}
                  </View>

                  <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
                    <PrimaryButton
                      label={saved ? "Unsave" : "Save"}
                      secondary
                      style={{ flex: 1 }}
                      onPress={() => toggleSaveOpportunity(opportunity.id)}
                    />
                    <PrimaryButton
                      label={locked ? status ?? "Applied" : "Apply"}
                      style={{ flex: 1 }}
                      disabled={locked}
                      onPress={() => applyToOpportunity(opportunity.id)}
                    />
                  </View>
                </GlassCard>
              );
            })}
          </View>

          <GlassCard>
            <ThemedText variant="title" strong>
              Activity Stream
            </ThemedText>
            <View style={{ marginTop: 10, gap: 8 }}>
              {activityLog.length === 0 ? (
                <ThemedText variant="body" muted>
                  No events yet. Save or apply to start building your activity trail.
                </ThemedText>
              ) : (
                activityLog.slice(0, 6).map((entry) => (
                  <View
                    key={entry.id}
                    style={[
                      styles.activityItem,
                      {
                        borderColor: theme.colors.border,
                        backgroundColor: theme.colors.cardAlt,
                      },
                    ]}
                  >
                    <ThemedText variant="body" style={{ flex: 1 }}>
                      {entry.message}
                    </ThemedText>
                    <ThemedText variant="label" muted>
                      {new Date(entry.timestamp).toLocaleDateString()}
                    </ThemedText>
                  </View>
                ))
              )}
            </View>
          </GlassCard>
        </>
      )}
    </ScreenContainer>
  );
}

type FilterChipProps = {
  label: string;
  selected: boolean;
  onPress: () => void;
};

function FilterChip({ label, selected, onPress }: FilterChipProps) {
  const { theme } = useAppTheme();

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.filterChip,
        {
          borderColor: selected ? theme.colors.accent : theme.colors.border,
          backgroundColor: selected ? theme.colors.accentSoft : theme.colors.cardAlt,
        },
      ]}
    >
      <ThemedText variant="body" strong={selected} style={{ color: selected ? theme.colors.accent : theme.colors.text }}>
        {label}
      </ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  metricRow: {
    flexDirection: "row",
    gap: 12,
  },
  searchInput: {
    marginTop: 12,
    minHeight: 46,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    fontSize: 15,
  },
  chipWrap: {
    marginTop: 10,
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  sortRow: {
    marginTop: 8,
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  filterChip: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  listWrap: {
    gap: 12,
  },
  rowTop: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
  },
  badge: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  skillPill: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  activityItem: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 10,
    flexDirection: "row",
    gap: 10,
    alignItems: "center",
  },
});
