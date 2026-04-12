import React from "react";
import { Pressable, StyleSheet, View } from "react-native";
import { useAppTheme } from "../theme/ThemeProvider";
import { ThemedText } from "./ThemedText";

type Props = {
  isRecording: boolean;
  onPress: () => void | Promise<void>;
  disabled?: boolean;
  startLabel?: string;
  stopLabel?: string;
};

export function RecordingButton({
  isRecording,
  onPress,
  disabled,
  startLabel = "Start Recording",
  stopLabel = "Stop Recording",
}: Props) {
  const { theme } = useAppTheme();

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.button,
        {
          backgroundColor: isRecording ? theme.colors.danger : theme.colors.accent,
          borderColor: isRecording ? theme.colors.danger : theme.colors.accent,
          opacity: disabled ? 0.55 : pressed ? 0.84 : 1,
        },
      ]}
    >
      <View style={styles.innerDot} />
      <ThemedText variant="body" strong style={styles.label}>
        {isRecording ? stopLabel : startLabel}
      </ThemedText>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    minHeight: 56,
    borderRadius: 18,
    borderWidth: 1,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    paddingHorizontal: 16,
  },
  innerDot: {
    width: 12,
    height: 12,
    borderRadius: 999,
    backgroundColor: "#ffffff",
    marginRight: 10,
  },
  label: {
    color: "#ffffff",
  },
});
