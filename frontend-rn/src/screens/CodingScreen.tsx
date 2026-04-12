import React, { useEffect, useState } from "react";
import { StyleSheet, TextInput, View } from "react-native";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
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
        height: 8,
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

export function CodingScreen() {
  const { theme } = useAppTheme();
  const { coding, syncCoding, isSyncingCoding, lastError, clearLastError } = useAppState();
  const [leetCode, setLeetCode] = useState(coding.leetCodeHandle);
  const [codeforces, setCodeforces] = useState(coding.codeforcesHandle);

  useEffect(() => {
    setLeetCode(coding.leetCodeHandle);
    setCodeforces(coding.codeforcesHandle);
  }, [coding.leetCodeHandle, coding.codeforcesHandle]);

  return (
    <ScreenContainer
      title="Coding"
      subtitle="Dedicated competitive programming section"
    >
      <View style={styles.metricRow}>
        <StatCard label="Solved" value={`${coding.solved}`} />
        <StatCard label="CF Rating" value={`${coding.rating}`} tone="warning" />
      </View>

      <GlassCard>
        <ThemedText variant="title" strong>
          Handle Sync
        </ThemedText>
        <View style={{ gap: 10, marginTop: 12 }}>
          <TextInput
            value={leetCode}
            onChangeText={setLeetCode}
            placeholder="LeetCode handle"
            placeholderTextColor={theme.colors.textMuted}
            style={[
              styles.input,
              {
                color: theme.colors.text,
                borderColor: theme.colors.border,
                backgroundColor: theme.colors.cardAlt,
              },
            ]}
          />
          <TextInput
            value={codeforces}
            onChangeText={setCodeforces}
            placeholder="Codeforces handle"
            placeholderTextColor={theme.colors.textMuted}
            style={[
              styles.input,
              {
                color: theme.colors.text,
                borderColor: theme.colors.border,
                backgroundColor: theme.colors.cardAlt,
              },
            ]}
          />
        </View>

        <PrimaryButton
          label={isSyncingCoding ? "Syncing..." : "Sync Profiles"}
          style={{ marginTop: 12 }}
          disabled={isSyncingCoding}
          onPress={async () => {
            clearLastError();
            await syncCoding({
              leetCodeHandle: leetCode,
              codeforcesHandle: codeforces,
            });
          }}
        />

        <ThemedText variant="body" muted style={{ marginTop: 10 }}>
          {coding.status}
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 4 }}>
          Last synced: {coding.lastSyncedAt ? new Date(coding.lastSyncedAt).toLocaleString() : "Never"}
        </ThemedText>
        {!!lastError && (
          <ThemedText variant="body" style={{ marginTop: 6, color: theme.colors.danger }}>
            {lastError}
          </ThemedText>
        )}
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Analytics
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 8 }}>
          Medium + Hard share
        </ThemedText>
        <ProgressBar value={coding.mediumHard} />
        <ThemedText variant="body" muted style={{ marginTop: 12 }}>
          Problem depth score
        </ThemedText>
        <ProgressBar value={coding.depth} />
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Upcoming Contests
        </ThemedText>
        <View style={{ marginTop: 10, gap: 8 }}>
          {coding.contests.map((contest) => (
            <View
              key={contest.title}
              style={[
                styles.contestItem,
                {
                  borderColor: theme.colors.border,
                  backgroundColor: theme.colors.cardAlt,
                },
              ]}
            >
              <View style={{ flex: 1 }}>
                <ThemedText variant="body" strong>
                  {contest.title}
                </ThemedText>
                <ThemedText variant="body" muted>
                  {contest.time} • {contest.duration}
                </ThemedText>
              </View>
            </View>
          ))}
        </View>
      </GlassCard>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  metricRow: {
    flexDirection: "row",
    gap: 12,
  },
  input: {
    minHeight: 46,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    fontSize: 15,
  },
  contestItem: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
    flexDirection: "row",
    alignItems: "center",
  },
});
