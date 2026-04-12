import React from "react";
import { StyleSheet, View } from "react-native";
import { GlassCard } from "../components/GlassCard";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState } from "../state/AppState";

const statusTone: Record<string, "success" | "warning" | "danger" | "neutral"> = {
  Interview: "warning",
  Applied: "neutral",
  Saved: "success",
  Rejected: "danger",
};

export function TrackerScreen() {
  const { theme } = useAppTheme();
  const { tracker } = useAppState();

  const interviews = tracker.filter((item) => item.status === "Interview").length;
  const applied = tracker.filter((item) => item.status === "Applied").length;

  return (
    <ScreenContainer
      title="Application Tracker"
      subtitle="High signal pipeline, clearly visible"
    >
      <GlassCard>
        <ThemedText variant="title" strong>
          Pipeline Health
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 6 }}>
          {tracker.length} total roles, {interviews} interview-stage, {applied} active applications.
        </ThemedText>
      </GlassCard>

      <View style={styles.listWrap}>
        {tracker.map((item) => {
          const tone = statusTone[item.status] ?? "neutral";
          const badgeColor =
            tone === "success"
              ? theme.colors.success
              : tone === "warning"
              ? theme.colors.warning
              : tone === "danger"
              ? theme.colors.danger
              : theme.colors.textMuted;

          return (
            <GlassCard key={`${item.company}-${item.role}`}>
              <View style={styles.rowTop}>
                <View style={{ flex: 1, paddingRight: 10 }}>
                  <ThemedText variant="title" strong>
                    {item.company}
                  </ThemedText>
                  <ThemedText variant="body" muted style={{ marginTop: 2 }}>
                    {item.role}
                  </ThemedText>
                </View>
                <View
                  style={[
                    styles.badge,
                    {
                      borderColor: badgeColor,
                      backgroundColor: `${badgeColor}22`,
                    },
                  ]}
                >
                  <ThemedText variant="label" style={{ color: badgeColor }}>
                    {item.status}
                  </ThemedText>
                </View>
              </View>
            </GlassCard>
          );
        })}
      </View>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  listWrap: {
    gap: 12,
  },
  rowTop: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  badge: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
});
