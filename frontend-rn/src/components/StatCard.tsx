import React from "react";
import { StyleSheet, View } from "react-native";
import { GlassCard } from "./GlassCard";
import { ThemedText } from "./ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";

type Props = {
  label: string;
  value: string;
  tone?: "default" | "success" | "warning";
};

export function StatCard({ label, value, tone = "default" }: Props) {
  const { theme } = useAppTheme();

  const valueColor =
    tone === "success"
      ? theme.colors.success
      : tone === "warning"
      ? theme.colors.warning
      : theme.colors.accent;

  return (
    <GlassCard style={styles.card}>
      <View style={styles.inner}>
        <ThemedText variant="label" muted>
          {label}
        </ThemedText>
        <ThemedText variant="title" strong style={{ color: valueColor, marginTop: 6 }}>
          {value}
        </ThemedText>
      </View>
    </GlassCard>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
  },
  inner: {
    minHeight: 82,
    justifyContent: "center",
  },
});
