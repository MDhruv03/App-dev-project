import React from "react";
import { StyleSheet, View } from "react-native";
import { GlassCard } from "../components/GlassCard";
import { ScreenContainer } from "../components/ScreenContainer";
import { StatCard } from "../components/StatCard";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState } from "../state/AppState";

function ProgressBar({ value }: { value: number }) {
  const { theme } = useAppTheme();

  return (
    <View
      style={{
        marginTop: 8,
        height: 9,
        borderRadius: 999,
        backgroundColor: theme.colors.cardAlt,
        overflow: "hidden",
      }}
    >
      <View
        style={{
          width: `${Math.max(0, Math.min(value, 100))}%`,
          height: "100%",
          backgroundColor: theme.colors.accent,
        }}
      />
    </View>
  );
}

export function AnalyticsScreen() {
  const { theme } = useAppTheme();
  const { analytics, readiness, coding, interview, activityLog } = useAppState();

  return (
    <ScreenContainer
      title="Analytics"
      subtitle="Application outcomes, interview signal, and growth velocity"
    >
      <View style={styles.metricRow}>
        <StatCard label="Readiness" value={`${readiness}%`} />
        <StatCard label="Success" value={`${analytics.successRate}%`} tone="success" />
      </View>

      <View style={styles.metricRow}>
        <StatCard label="Interview Avg" value={`${analytics.avgInterviewScore || 0}`} tone="warning" />
        <StatCard label="Attempts" value={`${analytics.interviewAttempts}`} />
      </View>

      <GlassCard>
        <ThemedText variant="title" strong>
          Pipeline Snapshot
        </ThemedText>
        <View style={{ marginTop: 10, gap: 10 }}>
          <KPI label="Total Applications" value={`${analytics.totalApplications}`} />
          <KPI label="Saved" value={`${analytics.savedCount}`} />
          <KPI label="Interview Stage" value={`${analytics.interviewCount}`} />
          <KPI label="Offers" value={`${analytics.offerCount}`} />
        </View>
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Signal Quality
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 8 }}>
          Coding depth
        </ThemedText>
        <ProgressBar value={coding.depth} />

        <ThemedText variant="body" muted style={{ marginTop: 12 }}>
          Interview confidence
        </ThemedText>
        <ProgressBar
          value={
            interview.answers.length > 0
              ? Math.round(
                  interview.answers.reduce((sum, answer) => sum + answer.rubric.confidence, 0) /
                    interview.answers.length
                )
              : 0
          }
        />

        <ThemedText variant="body" muted style={{ marginTop: 12 }}>
          Interview structure
        </ThemedText>
        <ProgressBar
          value={
            interview.answers.length > 0
              ? Math.round(
                  interview.answers.reduce((sum, answer) => sum + answer.rubric.structure, 0) /
                    interview.answers.length
                )
              : 0
          }
        />
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Recent Events
        </ThemedText>
        <View style={{ marginTop: 10, gap: 8 }}>
          {activityLog.length === 0 ? (
            <ThemedText variant="body" muted>
              Activity events will appear as you discover, apply, and practice.
            </ThemedText>
          ) : (
            activityLog.slice(0, 8).map((entry) => (
              <View
                key={entry.id}
                style={[
                  styles.eventItem,
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
    </ScreenContainer>
  );
}

function KPI({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
      <ThemedText variant="body" muted>
        {label}
      </ThemedText>
      <ThemedText variant="body" strong>
        {value}
      </ThemedText>
    </View>
  );
}

const styles = StyleSheet.create({
  metricRow: {
    flexDirection: "row",
    gap: 12,
  },
  eventItem: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 10,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
});
