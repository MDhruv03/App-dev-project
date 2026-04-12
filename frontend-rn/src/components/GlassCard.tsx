import React from "react";
import { StyleSheet, View, ViewStyle } from "react-native";
import { BlurView } from "expo-blur";
import { useAppTheme } from "../theme/ThemeProvider";

type Props = {
  children: React.ReactNode;
  style?: ViewStyle;
};

export function GlassCard({ children, style }: Props) {
  const { theme, mode } = useAppTheme();

  return (
    <View
      style={[
        styles.wrap,
        {
          backgroundColor: theme.colors.card,
          borderColor: theme.colors.border,
          borderRadius: theme.radius.lg,
        },
        style,
      ]}
    >
      <BlurView intensity={mode === "dark" ? 24 : 16} tint={mode === "dark" ? "dark" : "light"} style={styles.blur}>
        {children}
      </BlurView>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    borderWidth: 1,
    overflow: "hidden",
  },
  blur: {
    padding: 16,
  },
});
