import React from "react";
import { Pressable, ScrollView, StyleSheet, View, ViewStyle } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { LinearGradient } from "expo-linear-gradient";
import { Feather } from "@expo/vector-icons";
import { ThemedText } from "./ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";

type Props = {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  contentStyle?: ViewStyle;
  scrollable?: boolean;
};

export function ScreenContainer({
  title,
  subtitle,
  children,
  contentStyle,
  scrollable = true,
}: Props) {
  const { theme, mode, toggleMode } = useAppTheme();

  const content = scrollable ? (
    <ScrollView
      contentContainerStyle={[
        styles.scrollContent,
        { paddingHorizontal: theme.spacing.lg, paddingBottom: theme.spacing.xxl },
        contentStyle,
      ]}
      showsVerticalScrollIndicator={false}
    >
      {children}
    </ScrollView>
  ) : (
    <View
      style={[
        styles.fixedContent,
        { paddingHorizontal: theme.spacing.lg, paddingBottom: theme.spacing.xxl },
        contentStyle,
      ]}
    >
      {children}
    </View>
  );

  return (
    <LinearGradient
      colors={[theme.colors.bgTop, theme.colors.bgMid, theme.colors.bgBottom]}
      style={StyleSheet.absoluteFill}
    >
      <SafeAreaView style={styles.safeArea}>
        <View style={styles.headerRow}>
          <View style={styles.headerTextWrap}>
            <ThemedText variant="display">{title}</ThemedText>
            {!!subtitle && (
              <ThemedText variant="body" muted style={styles.subtitle}>
                {subtitle}
              </ThemedText>
            )}
          </View>
          <Pressable
            onPress={toggleMode}
            style={[
              styles.themeButton,
              {
                backgroundColor: theme.colors.card,
                borderColor: theme.colors.border,
              },
            ]}
          >
            <Feather
              name={mode === "dark" ? "sun" : "moon"}
              size={18}
              color={theme.colors.text}
            />
          </Pressable>
        </View>
        {content}
      </SafeAreaView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "flex-start",
    justifyContent: "space-between",
    paddingHorizontal: 22,
    paddingTop: 6,
    marginBottom: 12,
  },
  headerTextWrap: {
    flex: 1,
    paddingRight: 14,
  },
  subtitle: {
    marginTop: 4,
  },
  themeButton: {
    width: 42,
    height: 42,
    borderRadius: 14,
    borderWidth: 1,
    alignItems: "center",
    justifyContent: "center",
    marginTop: 2,
  },
  scrollContent: {
    gap: 14,
  },
  fixedContent: {
    flex: 1,
    gap: 14,
  },
});
