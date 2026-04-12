import React, { useState } from "react";
import { StyleSheet, TextInput, View } from "react-native";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState } from "../state/AppState";

const knownSkills = [
  "Kotlin",
  "Java",
  "React Native",
  "React",
  "TypeScript",
  "JavaScript",
  "SQL",
  "System Design",
  "Microservices",
  "Redis",
  "Kafka",
  "Android",
  "Compose",
  "Python",
  "PyTorch",
  "Docker",
  "CI/CD",
  "GraphQL",
  "Node.js",
  "AWS",
];

export function ProfileScreen() {
  const { theme } = useAppTheme();
  const { profile, updateProfile } = useAppState();
  const [name, setName] = useState(profile.name);
  const [email, setEmail] = useState(profile.email);
  const [skills, setSkills] = useState(profile.skills);
  const [roles, setRoles] = useState(profile.roles);
  const [status, setStatus] = useState("Unsaved changes");
  const [resumeText, setResumeText] = useState("");
  const [extractedSkills, setExtractedSkills] = useState<string[]>([]);

  const parseResumeSkills = () => {
    const text = resumeText.toLowerCase();
    const matches = knownSkills.filter((skill) => text.includes(skill.toLowerCase()));
    const deduped = Array.from(new Set(matches));
    setExtractedSkills(deduped);

    if (deduped.length === 0) {
      setStatus("No known skills detected from pasted resume text.");
      return;
    }

    setStatus(`Detected ${deduped.length} skills from resume text.`);
  };

  const applyExtractedSkills = () => {
    if (extractedSkills.length === 0) {
      setStatus("Parse resume text first to extract skills.");
      return;
    }

    const current = skills
      .split(",")
      .map((entry) => entry.trim())
      .filter(Boolean);
    const merged = Array.from(new Set([...current, ...extractedSkills]));
    setSkills(merged.join(", "));
    setStatus("Extracted skills merged into profile skills field.");
  };

  return (
    <ScreenContainer title="Profile" subtitle="Clean profile only. Coding handles live in Coding tab.">
      <GlassCard>
        <ThemedText variant="title" strong>
          Personal
        </ThemedText>

        <View style={styles.fieldsWrap}>
          <TextInput
            value={name}
            onChangeText={setName}
            placeholder="Name"
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />
          <TextInput
            value={email}
            onChangeText={setEmail}
            placeholder="Email"
            keyboardType="email-address"
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />
          <TextInput
            value={skills}
            onChangeText={setSkills}
            placeholder="Skills"
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />
          <TextInput
            value={roles}
            onChangeText={setRoles}
            placeholder="Preferred roles"
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />
        </View>

        <PrimaryButton
          label="Save Profile"
          style={{ marginTop: 12 }}
          onPress={() => {
            updateProfile({
              name: name.trim(),
              email: email.trim(),
              skills: skills.trim(),
              roles: roles.trim(),
            });
            setStatus("Saved successfully");
          }}
        />
        <ThemedText variant="body" muted style={{ marginTop: 8 }}>
          {status}
        </ThemedText>
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Resume
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 8 }}>
          Paste resume text to extract skills and merge into your profile.
        </ThemedText>

        <TextInput
          value={resumeText}
          onChangeText={setResumeText}
          multiline
          placeholder="Paste resume text here for quick skill extraction..."
          placeholderTextColor={theme.colors.textMuted}
          style={[
            styles.resumeInput,
            {
              borderColor: theme.colors.border,
              color: theme.colors.text,
              backgroundColor: theme.colors.cardAlt,
            },
          ]}
        />

        <View style={{ flexDirection: "row", gap: 10, marginTop: 10 }}>
          <PrimaryButton label="Parse Skills" style={{ flex: 1 }} onPress={parseResumeSkills} />
          <PrimaryButton label="Apply Skills" secondary style={{ flex: 1 }} onPress={applyExtractedSkills} />
        </View>

        <View style={{ marginTop: 10, gap: 6 }}>
          {extractedSkills.length === 0 ? (
            <ThemedText variant="body" muted>
              No extracted skills yet.
            </ThemedText>
          ) : (
            extractedSkills.map((skill) => (
              <ThemedText key={skill} variant="body" muted>
                - {skill}
              </ThemedText>
            ))
          )}
        </View>
      </GlassCard>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  fieldsWrap: {
    marginTop: 12,
    gap: 10,
  },
  input: {
    minHeight: 46,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    fontSize: 15,
  },
  resumeInput: {
    marginTop: 12,
    minHeight: 120,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    textAlignVertical: "top",
    fontSize: 15,
  },
});
