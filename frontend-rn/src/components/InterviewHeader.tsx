import React from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { ThemedText } from "./ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";

type Props = {
  questionIndex: number;
  totalQuestions: number;
  domain: string;
  difficulty: string;
  elapsedMs: number;
  onQuit: () => void;
  disabled?: boolean;
};

function formatClock(elapsedMs: number): string {
  const totalSeconds = Math.max(0, Math.floor(elapsedMs / 1000));
  const minutes = Math.floor(totalSeconds / 60)
    .toString()
    .padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

export function InterviewHeader({
  questionIndex,
  totalQuestions,
  domain,
  difficulty,
  elapsedMs,
  onQuit,
  disabled,
}: Props) {
  const { theme } = useAppTheme();
  const safeTotal = Math.max(1, totalQuestions);
  const current = Math.min(Math.max(1, questionIndex + 1), safeTotal);

  return (
    <View
      style={[
        styles.wrap,
        {
          borderColor: theme.colors.border,
          backgroundColor: theme.colors.card,
        },
      ]}
    >
      <View style={styles.left}>
        <ThemedText variant="label" muted>
          Live Interview
        </ThemedText>
        <ThemedText variant="title" strong style={{ marginTop: 3 }}>
          Q{current}/{safeTotal}
        </ThemedText>
      </View>

      <View style={styles.metaRow}>
        <View style={[styles.metaPill, { borderColor: theme.colors.border }]}>
          <ThemedText variant="label" muted>
            {domain}
          </ThemedText>
        </View>
        <View style={[styles.metaPill, { borderColor: theme.colors.border }]}>
          <ThemedText variant="label" muted>
            {difficulty}
          </ThemedText>
        </View>
        <View style={[styles.metaPill, { borderColor: theme.colors.border }]}>
          <ThemedText variant="label" strong>
            {formatClock(elapsedMs)}
          </ThemedText>
        </View>
      </View>

      <Pressable
        onPress={onQuit}
        disabled={disabled}
        style={({ pressed }) => [
          styles.quitButton,
          {
            borderColor: theme.colors.danger,
            backgroundColor: theme.colors.cardAlt,
            opacity: disabled ? 0.56 : pressed ? 0.84 : 1,
          },
        ]}
      >
        <ThemedText variant="body" strong style={{ color: theme.colors.danger }}>
          End
        </ThemedText>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    borderWidth: 1,
    borderRadius: 16,
    paddingHorizontal: 14,
    paddingVertical: 12,
    flexDirection: "row",
    alignItems: "center",
    gap: 10,
  },
  left: {
    minWidth: 80,
  },
  metaRow: {
    flex: 1,
    flexDirection: "row",
    gap: 8,
    alignItems: "center",
    flexWrap: "wrap",
  },
  metaPill: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  quitButton: {
    minWidth: 72,
    minHeight: 42,
    borderWidth: 1,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 10,
  },
});
