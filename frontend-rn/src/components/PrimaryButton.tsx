import React from "react";
import { Pressable, StyleSheet, ViewStyle } from "react-native";
import { ThemedText } from "./ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";

type Props = {
  label: string;
  onPress: () => void | Promise<void>;
  secondary?: boolean;
  disabled?: boolean;
  style?: ViewStyle;
};

export function PrimaryButton({ label, onPress, secondary, disabled, style }: Props) {
  const { theme } = useAppTheme();

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.button,
        {
          backgroundColor: secondary ? theme.colors.cardAlt : theme.colors.accent,
          borderColor: secondary ? theme.colors.border : theme.colors.accent,
          opacity: disabled ? 0.55 : pressed ? 0.86 : 1,
          borderRadius: theme.radius.md,
        },
        style,
      ]}
    >
      <ThemedText variant="body" strong style={{ color: secondary ? theme.colors.text : "#ffffff" }}>
        {label}
      </ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    minHeight: 48,
    borderWidth: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 14,
  },
});
