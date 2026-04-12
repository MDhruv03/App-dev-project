import React, { useEffect, useMemo, useState } from "react";
import { Pressable, StyleSheet, TextInput, View } from "react-native";
import * as DocumentPicker from "expo-document-picker";
import { File } from "expo-file-system";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { useAppTheme } from "../theme/ThemeProvider";
import { useAppState } from "../state/AppState";
import { useAuth } from "../state/AuthState";
import { parseUploadedResume } from "../services/resumeService";

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

const knownRoles = [
  "Software Engineer",
  "SDE",
  "Android Developer",
  "Mobile Engineer",
  "Frontend Engineer",
  "Backend Engineer",
  "Full Stack Engineer",
  "Machine Learning Engineer",
  "Data Scientist",
  "DevOps Engineer",
  "Platform Engineer",
  "QA Engineer",
  "Product Manager",
];

function normalizeResumeText(raw: string): string {
  return raw
    .replace(/[\u0000-\u0008\u000B\u000C\u000E-\u001F]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function inferMimeType(fileName: string): string {
  const lower = fileName.toLowerCase();
  if (lower.endsWith(".pdf")) return "application/pdf";
  if (lower.endsWith(".txt")) return "text/plain";
  if (lower.endsWith(".doc")) return "application/msword";
  if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
  return "application/octet-stream";
}

function extractNameCandidate(text: string): string {
  const lines = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, 12);

  const candidate = lines.find((line) => {
    if (line.includes("@")) return false;
    if (line.length < 5 || line.length > 50) return false;
    return /^[A-Za-z][A-Za-z .'-]{3,}$/.test(line);
  });

  return candidate ?? "";
}

function extractResumeInfo(rawResume: string) {
  const text = normalizeResumeText(rawResume);
  const lower = text.toLowerCase();

  const matchedSkills = Array.from(
    new Set(knownSkills.filter((skill) => lower.includes(skill.toLowerCase()))),
  );

  const matchedRoles = Array.from(
    new Set(knownRoles.filter((role) => lower.includes(role.toLowerCase()))),
  );

  const emailMatch = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i);

  return {
    normalizedText: text,
    skills: matchedSkills,
    roles: matchedRoles,
    email: emailMatch?.[0] ?? "",
    name: extractNameCandidate(rawResume),
  };
}

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
  const [resumeSource, setResumeSource] = useState("pasted text");
  const [isParsingUpload, setIsParsingUpload] = useState(false);
  const [extractedSkills, setExtractedSkills] = useState<string[]>([]);
  const [extractedRoles, setExtractedRoles] = useState<string[]>([]);
  const [extractedName, setExtractedName] = useState("");
  const [extractedEmail, setExtractedEmail] = useState("");

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

  const parseResumeContent = (content: string, sourceLabel: string) => {
    const extracted = extractResumeInfo(content);

    setResumeText(extracted.normalizedText);
    setResumeSource(sourceLabel);
    setExtractedSkills(extracted.skills);
    setExtractedRoles(extracted.roles);
    setExtractedName(extracted.name);
    setExtractedEmail(extracted.email);

    if (extracted.skills.length === 0 && extracted.roles.length === 0 && !extracted.email) {
      setStatus(`Parsed ${sourceLabel}, but no strong profile signals were found.`);
      return;
    }

    setStatus(
      `Parsed ${sourceLabel}: ${extracted.skills.length} skills, ${extracted.roles.length} roles${
        extracted.email ? ", email found" : ""
      }.`,
    );
  };

  const parseResumeSkills = () => {
    if (!resumeText.trim()) {
      setStatus("Paste resume text or upload a resume file first.");
      return;
    }
    parseResumeContent(resumeText, "pasted text");
  };

  const parseResumeFromUpload = async () => {
    try {
      setIsParsingUpload(true);
      const result = await DocumentPicker.getDocumentAsync({
        copyToCacheDirectory: true,
        multiple: false,
        type: ["text/*", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"],
      });

      if (result.canceled) {
        setStatus("Resume upload cancelled.");
        return;
      }

      const picked = result.assets[0];
      const fileBase64 = await new File(picked.uri).base64();
      const fileName = picked.name || "uploaded-resume";
      const mimeType = picked.mimeType || inferMimeType(fileName);

      if (!fileBase64.trim()) {
        setStatus("Could not read uploaded file bytes. Try another file.");
        return;
      }

      const parsed = await parseUploadedResume({
        fileName,
        mimeType,
        fileBase64,
      });

      if (!parsed.text.trim()) {
        setStatus("Uploaded resume was parsed, but no readable text was extracted.");
        return;
      }

      parseResumeContent(parsed.text, fileName);
    } catch (error) {
      const message = error instanceof Error ? error.message : "unknown error";
      setStatus(`Could not parse uploaded resume: ${message}`);
    } finally {
      setIsParsingUpload(false);
    }
  };

  const applyExtractedSkills = () => {
    if (extractedSkills.length === 0) {
      setStatus("Parse resume text first to extract skills.");
      return;
    }

    mergeSkills(extractedSkills);
    setStatus("Extracted skills merged into profile skills field.");
  };

  const applyExtractedProfileInfo = () => {
    if (!extractedName && !extractedEmail && extractedRoles.length === 0) {
      setStatus("No extracted name/email/roles to apply yet.");
      return;
    }

    if (extractedName) {
      setName(extractedName);
    }
    if (extractedEmail) {
      setEmail(extractedEmail);
    }
    if (extractedRoles.length > 0) {
      const currentRoles = roles
        .split(",")
        .map((entry) => entry.trim())
        .filter(Boolean);
      const mergedRoles = Array.from(new Set([...currentRoles, ...extractedRoles]));
      setRoles(mergedRoles.join(", "));
    }

    setStatus("Extracted profile info applied to form fields.");
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
          Upload a resume file or paste resume text to extract skills and profile details.
        </ThemedText>

        <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
          <PrimaryButton
            label={isParsingUpload ? "Uploading..." : "Upload & Parse Resume"}
            style={{ flex: 1 }}
            disabled={isParsingUpload}
            onPress={() => {
              void parseResumeFromUpload();
            }}
          />
        </View>

        <ThemedText variant="label" muted style={{ marginTop: 8 }}>
          Source: {resumeSource}
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

        <PrimaryButton
          label="Apply Extracted Profile Info"
          secondary
          style={{ marginTop: 10 }}
          onPress={applyExtractedProfileInfo}
        />

        <View style={{ marginTop: 12, gap: 6 }}>
          <ThemedText variant="label" muted>Extracted Name</ThemedText>
          <ThemedText variant="body" muted>{extractedName || "Not detected"}</ThemedText>
          <ThemedText variant="label" muted style={{ marginTop: 6 }}>Extracted Email</ThemedText>
          <ThemedText variant="body" muted>{extractedEmail || "Not detected"}</ThemedText>
          <ThemedText variant="label" muted style={{ marginTop: 6 }}>Extracted Roles</ThemedText>
          <ThemedText variant="body" muted>
            {extractedRoles.length > 0 ? extractedRoles.join(", ") : "No roles detected"}
          </ThemedText>
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
