import React, { useMemo, useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  TextInput,
  View,
  useWindowDimensions,
} from "react-native";
import { LinearGradient } from "expo-linear-gradient";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
import { ThemedText } from "../components/ThemedText";
import { useAuth } from "../state/AuthState";
import { useAppTheme } from "../theme/ThemeProvider";

type Mode = "login" | "signup";

const landingHighlights = [
  {
    title: "Signal-First",
    detail: "No fake seeded progress. Your dashboard starts clean and truthful.",
  },
  {
    title: "Interview Studio",
    detail: "Voice, adaptive prompts, and rubric coaching in one practical flow.",
  },
  {
    title: "Private Scope",
    detail: "Every profile, application, and interview stays account-specific.",
  },
];

function isEmailLike(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

export function AuthScreen() {
  const { theme } = useAppTheme();
  const { width } = useWindowDimensions();
  const { login, signup, isAuthenticating, authError, clearAuthError } = useAuth();
  const isWideLayout = width >= 940;
  const formCardStyle = isWideLayout ? styles.formCardWide : styles.formCard;

  const [mode, setMode] = useState<Mode>("login");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [localError, setLocalError] = useState<string | null>(null);

  const title = mode === "login" ? "Welcome Back" : "Create Your Studio Account";
  const subtitle = mode === "login"
    ? "Continue with your own interview history, coding depth, and curated opportunities."
    : "Start with a clean slate and build your own signal from day one.";

  const actionLabel = mode === "login" ? "Enter Workspace" : "Create Workspace";
  const helperLine =
    mode === "login"
      ? "Your previous state remains isolated to your account."
      : "Fresh account means no placeholder data and no noisy defaults.";

  const canSubmit = useMemo(() => {
    if (!isEmailLike(email)) {
      return false;
    }
    if (password.length < 8) {
      return false;
    }
    if (mode === "signup" && name.trim().length < 2) {
      return false;
    }
    return true;
  }, [email, mode, name, password.length]);

  const handleSubmit = async () => {
    clearAuthError();
    setLocalError(null);

    if (!isEmailLike(email)) {
      setLocalError("Enter a valid email address.");
      return;
    }

    if (password.length < 8) {
      setLocalError("Password must be at least 8 characters.");
      return;
    }

    if (mode === "signup" && name.trim().length < 2) {
      setLocalError("Name should be at least 2 characters.");
      return;
    }

    const ok = mode === "login"
      ? await login({ email: email.trim(), password })
      : await signup({ name: name.trim(), email: email.trim(), password });

    if (!ok) {
      return;
    }

    setPassword("");
  };

  return (
    <LinearGradient
      colors={
        theme.mode === "dark"
          ? ["#0f1a1f", "#15282e", "#1a353e"]
          : ["#f8f1e3", "#e7ebe2", "#dae6e6"]
      }
      start={{ x: 0, y: 0 }}
      end={{ x: 1, y: 1 }}
      style={styles.gradient}
    >
      <View
        pointerEvents="none"
        style={[
          styles.ambientOrb,
          styles.ambientOrbTop,
          { backgroundColor: theme.mode === "dark" ? "rgba(45,200,172,0.20)" : "rgba(15,122,104,0.16)" },
        ]}
      />
      <View
        pointerEvents="none"
        style={[
          styles.ambientOrb,
          styles.ambientOrbBottom,
          { backgroundColor: theme.mode === "dark" ? "rgba(104,204,154,0.13)" : "rgba(33,108,138,0.11)" },
        ]}
      />

      <KeyboardAvoidingView
        behavior={Platform.OS === "ios" ? "padding" : undefined}
        style={styles.keyboardWrap}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={[styles.content, isWideLayout && styles.contentWide]}
          showsVerticalScrollIndicator={false}
        >
          <View style={[styles.heroWrap, isWideLayout && styles.heroWrapWide]}>
            <ThemedText variant="label" strong style={{ color: theme.colors.accent }}>
              OpportunityHub
            </ThemedText>
            <ThemedText variant="display" strong style={styles.heroHeadline}>
              Build Signal. Not Noise.
            </ThemedText>
            <ThemedText variant="body" muted style={styles.heroSubline}>
              Interview prep, coding momentum, and opportunity tracking in one focused workspace.
            </ThemedText>

            <View style={styles.highlightStack}>
              {landingHighlights.map((item) => (
                <View
                  key={item.title}
                  style={[
                    styles.highlightBlock,
                    {
                      borderColor: theme.colors.border,
                      backgroundColor: theme.colors.card,
                    },
                  ]}
                >
                  <ThemedText variant="label" strong style={{ color: theme.colors.accent }}>
                    {item.title}
                  </ThemedText>
                  <ThemedText variant="body" muted style={{ marginTop: 4 }}>
                    {item.detail}
                  </ThemedText>
                </View>
              ))}
            </View>

            <View style={styles.metricRow}>
              <View style={[styles.metricCard, { borderColor: theme.colors.border, backgroundColor: theme.colors.cardAlt }]}>
                <ThemedText variant="title" strong>
                  100%
                </ThemedText>
                <ThemedText variant="label" muted>
                  User-scoped state
                </ThemedText>
              </View>
              <View style={[styles.metricCard, { borderColor: theme.colors.border, backgroundColor: theme.colors.cardAlt }]}>
                <ThemedText variant="title" strong>
                  0
                </ThemedText>
                <ThemedText variant="label" muted>
                  Fake starter data
                </ThemedText>
              </View>
            </View>
          </View>

          <GlassCard style={formCardStyle}>
            <View style={styles.modeSwitchRow}>
              <Pressable
                onPress={() => {
                  clearAuthError();
                  setLocalError(null);
                  setMode("login");
                }}
                style={[
                  styles.modeSwitch,
                  {
                    borderColor: mode === "login" ? theme.colors.accent : theme.colors.border,
                    backgroundColor: mode === "login" ? theme.colors.accentSoft : theme.colors.cardAlt,
                  },
                ]}
              >
                <ThemedText variant="body" strong={mode === "login"}>
                  Login
                </ThemedText>
              </Pressable>
              <Pressable
                onPress={() => {
                  clearAuthError();
                  setLocalError(null);
                  setMode("signup");
                }}
                style={[
                  styles.modeSwitch,
                  {
                    borderColor: mode === "signup" ? theme.colors.accent : theme.colors.border,
                    backgroundColor: mode === "signup" ? theme.colors.accentSoft : theme.colors.cardAlt,
                  },
                ]}
              >
                <ThemedText variant="body" strong={mode === "signup"}>
                  Sign Up
                </ThemedText>
              </Pressable>
            </View>

            <ThemedText variant="title" strong>
              {title}
            </ThemedText>
            <ThemedText variant="body" muted style={{ marginTop: 8 }}>
              {subtitle}
            </ThemedText>
            <ThemedText variant="label" muted style={{ marginTop: 10 }}>
              {helperLine}
            </ThemedText>

            {mode === "signup" && (
              <View style={{ marginTop: 14 }}>
                <ThemedText variant="label" muted>
                  Full Name
                </ThemedText>
                <TextInput
                  value={name}
                  onChangeText={setName}
                  placeholder="Your name"
                  autoCapitalize="words"
                  autoCorrect={false}
                  placeholderTextColor={theme.colors.textMuted}
                  style={[
                    styles.input,
                    {
                      borderColor: theme.colors.border,
                      color: theme.colors.text,
                      backgroundColor: theme.colors.cardAlt,
                    },
                  ]}
                />
              </View>
            )}

            <View style={{ marginTop: 14 }}>
              <ThemedText variant="label" muted>
                Email
              </ThemedText>
              <TextInput
                value={email}
                onChangeText={setEmail}
                placeholder="you@example.com"
                keyboardType="email-address"
                autoCapitalize="none"
                autoCorrect={false}
                placeholderTextColor={theme.colors.textMuted}
                style={[
                  styles.input,
                  {
                    borderColor: theme.colors.border,
                    color: theme.colors.text,
                    backgroundColor: theme.colors.cardAlt,
                  },
                ]}
              />
            </View>

            <View style={{ marginTop: 14 }}>
              <ThemedText variant="label" muted>
                Password
              </ThemedText>
              <TextInput
                value={password}
                onChangeText={setPassword}
                placeholder="At least 8 characters"
                autoCapitalize="none"
                autoCorrect={false}
                secureTextEntry
                placeholderTextColor={theme.colors.textMuted}
                style={[
                  styles.input,
                  {
                    borderColor: theme.colors.border,
                    color: theme.colors.text,
                    backgroundColor: theme.colors.cardAlt,
                  },
                ]}
              />
            </View>

            {(localError || authError) && (
              <View
                style={[
                  styles.errorBox,
                  {
                    borderColor: theme.colors.danger,
                    backgroundColor: theme.mode === "dark" ? "rgba(226,117,107,0.18)" : "rgba(184,77,69,0.10)",
                  },
                ]}
              >
                <ThemedText variant="body" style={{ color: theme.colors.danger }}>
                  {localError || authError}
                </ThemedText>
              </View>
            )}

            <PrimaryButton
              label={isAuthenticating ? "Please wait..." : actionLabel}
              disabled={isAuthenticating || !canSubmit}
              style={{ marginTop: 14 }}
              onPress={() => {
                void handleSubmit();
              }}
            />

            <Pressable
              onPress={() => {
                clearAuthError();
                setLocalError(null);
                setMode((prev) => (prev === "login" ? "signup" : "login"));
              }}
              style={{ marginTop: 12, alignSelf: "center" }}
            >
              <ThemedText variant="body" muted>
                {mode === "login" ? "No account? Create one" : "Already have an account? Login"}
              </ThemedText>
            </Pressable>
          </GlassCard>
        </ScrollView>
      </KeyboardAvoidingView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  gradient: {
    flex: 1,
  },
  ambientOrb: {
    position: "absolute",
    width: 260,
    height: 260,
    borderRadius: 999,
  },
  ambientOrbTop: {
    top: -90,
    right: -70,
  },
  ambientOrbBottom: {
    bottom: -120,
    left: -90,
  },
  keyboardWrap: {
    flex: 1,
  },
  content: {
    flexGrow: 1,
    justifyContent: "center",
    paddingHorizontal: 20,
    paddingVertical: 28,
    gap: 16,
  },
  contentWide: {
    flexDirection: "row",
    alignItems: "stretch",
    justifyContent: "center",
    gap: 18,
  },
  heroWrap: {
    gap: 10,
  },
  heroWrapWide: {
    width: 420,
    justifyContent: "center",
  },
  heroHeadline: {
    marginTop: 6,
    fontSize: 46,
    lineHeight: 48,
    letterSpacing: 0.35,
  },
  heroSubline: {
    marginTop: 6,
  },
  highlightStack: {
    marginTop: 14,
    gap: 9,
  },
  highlightBlock: {
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  metricRow: {
    marginTop: 6,
    flexDirection: "row",
    gap: 10,
  },
  metricCard: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 14,
    paddingHorizontal: 12,
    paddingVertical: 10,
    gap: 4,
  },
  formCard: {
    width: "100%",
    maxWidth: 560,
    alignSelf: "center",
  },
  formCardWide: {
    width: 520,
    marginTop: 20,
    alignSelf: "stretch",
  },
  modeSwitchRow: {
    flexDirection: "row",
    gap: 8,
    marginBottom: 14,
  },
  modeSwitch: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 12,
    paddingVertical: 10,
    alignItems: "center",
    justifyContent: "center",
  },
  errorBox: {
    marginTop: 12,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
  },
  input: {
    marginTop: 8,
    minHeight: 46,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
  },
});
