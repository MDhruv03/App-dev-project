import React, { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, TextInput, View } from "react-native";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState } from "../state/AppState";
import { useAuth } from "../state/AuthState";

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
  const { logout, user } = useAuth();
  const [name, setName] = useState(profile.name);
  const [email, setEmail] = useState(profile.email);
  const [skills, setSkills] = useState(profile.skills);
  const [roles, setRoles] = useState(profile.roles);
  const [status, setStatus] = useState("Unsaved changes");
  const [resumeText, setResumeText] = useState("");
  const [extractedSkills, setExtractedSkills] = useState<string[]>([]);

  useEffect(() => {
    setName(profile.name);
    setEmail(profile.email);
    setSkills(profile.skills);
    setRoles(profile.roles);
  }, [profile.email, profile.name, profile.roles, profile.skills]);

  const selectedSkillSet = useMemo(() => {
    return new Set(
      skills
        .split(",")
        .map((entry) => entry.trim().toLowerCase())
        .filter(Boolean)
    );
  }, [skills]);

  const mergeSkills = (incoming: string[]) => {
    const current = skills
      .split(",")
      .map((entry) => entry.trim())
      .filter(Boolean);
    const merged = Array.from(new Set([...current, ...incoming.map((item) => item.trim())].filter(Boolean)));
    setSkills(merged.join(", "));
  };

  const toggleQuickSkill = (skill: string) => {
    const current = skills
      .split(",")
      .map((entry) => entry.trim())
      .filter(Boolean);
    const exists = current.some((entry) => entry.toLowerCase() === skill.toLowerCase());
    const next = exists
      ? current.filter((entry) => entry.toLowerCase() !== skill.toLowerCase())
      : [...current, skill];
    setSkills(next.join(", "));
  };

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

    mergeSkills(extractedSkills);
    setStatus("Extracted skills merged into profile skills field.");
  };

  return (
    <ScreenContainer title="Profile" subtitle="Structured profile editor with cleaner input flow.">
      <GlassCard>
        <ThemedText variant="title" strong>
          Identity
        </ThemedText>

        <View style={styles.fieldsWrap}>
          <ThemedText variant="label" muted>
            Full Name
          </ThemedText>
          <TextInput
            value={name}
            onChangeText={setName}
            placeholder="Your full name"
            autoCapitalize="words"
            autoCorrect={false}
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />

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
            style={[styles.input, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />
        </View>
      </GlassCard>

      <GlassCard>
        <ThemedText variant="title" strong>
          Career Focus
        </ThemedText>

        <View style={styles.fieldsWrap}>
          <ThemedText variant="label" muted>
            Skills (comma separated)
          </ThemedText>
          <TextInput
            value={skills}
            onChangeText={setSkills}
            multiline
            textAlignVertical="top"
            placeholder="Kotlin, React Native, SQL, System Design"
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.textArea, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />

          <ThemedText variant="label" muted>
            Preferred Roles
          </ThemedText>
          <TextInput
            value={roles}
            onChangeText={setRoles}
            multiline
            textAlignVertical="top"
            placeholder="Mobile Engineer, SDE"
            placeholderTextColor={theme.colors.textMuted}
            style={[styles.textArea, { borderColor: theme.colors.border, color: theme.colors.text, backgroundColor: theme.colors.cardAlt }]}
          />
        </View>

        <ThemedText variant="label" muted style={{ marginTop: 12 }}>
          Quick Skill Chips
        </ThemedText>
        <View style={styles.chipWrap}>
          {knownSkills.slice(0, 12).map((skill) => {
            const selected = selectedSkillSet.has(skill.toLowerCase());
            return (
              <Pressable
                key={skill}
                onPress={() => {
                  toggleQuickSkill(skill);
                }}
                style={[
                  styles.skillChip,
                  {
                    borderColor: selected ? theme.colors.accent : theme.colors.border,
                    backgroundColor: selected ? theme.colors.accentSoft : theme.colors.cardAlt,
                  },
                ]}
              >
                <ThemedText variant="body" strong={selected} style={{ color: selected ? theme.colors.accent : theme.colors.text }}>
                  {skill}
                </ThemedText>
              </Pressable>
            );
          })}
        </View>

        <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
          <PrimaryButton
            label="Save Profile"
            style={{ flex: 1 }}
            onPress={() => {
              updateProfile({
                name: name.trim(),
                email: email.trim(),
                skills: skills.trim(),
                roles: roles.trim(),
              });
              setStatus("Profile saved successfully.");
            }}
          />
          <PrimaryButton
            label="Reset"
            secondary
            style={{ flex: 1 }}
            onPress={() => {
              setName(profile.name);
              setEmail(profile.email);
              setSkills(profile.skills);
              setRoles(profile.roles);
              setStatus("Form reset to saved values.");
            }}
          />
        </View>

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

      <GlassCard>
        <ThemedText variant="title" strong>
          Account
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 8 }}>
          Logged in as {user?.email ?? profile.email}
        </ThemedText>
        <PrimaryButton
          label="Sign Out"
          secondary
          style={{ marginTop: 12 }}
          onPress={() => {
            void logout();
          }}
        />
      </GlassCard>
    </ScreenContainer>
  );
}

const styles = StyleSheet.create({
  fieldsWrap: {
    marginTop: 12,
    gap: 10,
  },
  chipWrap: {
    marginTop: 8,
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
  },
  skillChip: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  input: {
    minHeight: 46,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
  },
  textArea: {
    minHeight: 88,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
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
