import React from "react";
import { ActivityIndicator, StyleSheet, Text, View } from "react-native";
import { LinearGradient } from "expo-linear-gradient";

type Props = {
  title?: string;
  message?: string;
};

export function AppBootScreen({
  title = "Preparing your workspace",
  message = "Loading profile, coding sync, and interview context...",
}: Props) {
  return (
    <LinearGradient colors={["#0f171b", "#132229", "#16303a"]} style={StyleSheet.absoluteFill}>
      <View style={styles.centered}>
        <ActivityIndicator size="large" color="#2dc8ac" />
        <Text style={styles.title}>
          {title}
        </Text>
        <Text style={styles.message}>
          {message}
        </Text>
      </View>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  centered: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 30,
  },
  title: {
    marginTop: 18,
    textAlign: "center",
    color: "#f2f7f6",
    fontSize: 24,
    fontWeight: "700",
  },
  message: {
    marginTop: 8,
    textAlign: "center",
    color: "#9bb8b1",
    fontSize: 14,
    lineHeight: 20,
  },
});
