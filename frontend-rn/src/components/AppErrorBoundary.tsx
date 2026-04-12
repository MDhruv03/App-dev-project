import React from "react";
import { Pressable, SafeAreaView, StyleSheet, Text, View } from "react-native";
import { LinearGradient } from "expo-linear-gradient";

type Props = {
  children: React.ReactNode;
  onReset?: () => void;
};

type State = {
  hasError: boolean;
};

export class AppErrorBoundary extends React.Component<Props, State> {
  state: State = {
    hasError: false,
  };

  static getDerivedStateFromError(): State {
    return {
      hasError: true,
    };
  }

  componentDidCatch(error: unknown) {
    console.error("[AppErrorBoundary]", error);
  }

  handleReset = () => {
    this.setState({ hasError: false });
    this.props.onReset?.();
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <LinearGradient colors={["#130f10", "#1d1415", "#27191b"]} style={StyleSheet.absoluteFill}>
        <SafeAreaView style={styles.safeArea}>
          <View style={styles.card}>
            <Text style={styles.title}>Something broke at runtime.</Text>
            <Text style={styles.message}>
              The app recovered into a safe mode. Tap Retry to remount the application.
            </Text>
            <Pressable onPress={this.handleReset} style={styles.button}>
              <Text style={styles.buttonText}>Retry App</Text>
            </Pressable>
          </View>
        </SafeAreaView>
      </LinearGradient>
    );
  }
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 20,
  },
  card: {
    width: "100%",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#5a3135",
    backgroundColor: "rgba(24, 14, 16, 0.85)",
    padding: 18,
  },
  title: {
    color: "#f8e6e8",
    fontSize: 20,
    fontWeight: "700",
  },
  message: {
    color: "#debdc0",
    marginTop: 8,
    fontSize: 14,
    lineHeight: 20,
  },
  button: {
    marginTop: 16,
    height: 44,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#ff8d98",
    backgroundColor: "#6a2e36",
    alignItems: "center",
    justifyContent: "center",
  },
  buttonText: {
    color: "#ffe7ea",
    fontWeight: "700",
  },
});
