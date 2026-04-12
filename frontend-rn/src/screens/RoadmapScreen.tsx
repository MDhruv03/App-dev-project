import React from "react";
import { StyleSheet, View } from "react-native";
import { GlassCard } from "../components/GlassCard";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState } from "../state/AppState";

export function RoadmapScreen() {
  const { theme } = useAppTheme();
  const { roadmapTasks, coding, analytics, interview } = useAppState();

  const latestWeaknesses =
    interview.answers.length > 0
      ? interview.answers[interview.answers.length - 1].improvements.slice(0, 3)
      : ["Run one mock round first to unlock AI improvement hints."];

  return (
    <ScreenContainer
      title="Roadmap"
      subtitle="Dynamic progression plan generated from your live app state"
    >
      <GlassCard>
        <ThemedText variant="title" strong>
          This Week
        </ThemedText>
        <View style={{ marginTop: 12, gap: 10 }}>
          {roadmapTasks.map((task) => (
            <View
              key={task}
              style={[
                styles.taskItem,
                {
                  borderColor: theme.colors.border,
                  backgroundColor: theme.colors.cardAlt,
                },
              ]}
            >
              <View style={[styles.dot, { backgroundColor: theme.colors.accent }]} />
              <ThemedText variant="body" style={{ flex: 1 }}>
                {task}
              </ThemedText>
            </View>
          ))}
        </View>
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Focus Metrics
        </ThemedText>
        <View style={{ marginTop: 10, gap: 8 }}>
          <MetricLine label="Coding depth" value={`${coding.depth}%`} />
          <MetricLine label="Interview attempts" value={`${analytics.interviewAttempts}`} />
          <MetricLine label="Applications in motion" value={`${analytics.totalApplications}`} />
          <MetricLine label="Offer conversion" value={`${analytics.successRate}%`} />
        </View>
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Interview Weaknesses
        </ThemedText>
        <View style={{ marginTop: 10, gap: 8 }}>
          {latestWeaknesses.map((item) => (
            <View
              key={item}
              style={[
                styles.taskItem,
                {
                  borderColor: theme.colors.border,
                  backgroundColor: theme.colors.cardAlt,
                },
              ]}
            >
              <View style={[styles.dot, { backgroundColor: theme.colors.warning }]} />
              <ThemedText variant="body" style={{ flex: 1 }}>
                {item}
              </ThemedText>
            </View>
          ))}
        </View>
      </GlassCard>
    </ScreenContainer>
  );
}

function MetricLine({ label, value }: { label: string; value: string }) {
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
  taskItem: {
    flexDirection: "row",
    alignItems: "flex-start",
    gap: 10,
    borderWidth: 1,
    borderRadius: 12,
    padding: 10,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 999,
    marginTop: 7,
  },
});
