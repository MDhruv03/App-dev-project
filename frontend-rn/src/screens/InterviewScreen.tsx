import React, { useEffect, useMemo, useRef, useState } from "react";
import { ActivityIndicator, Platform, Pressable, StyleSheet, TextInput, View } from "react-native";
import { Audio } from "expo-av";
import * as Speech from "expo-speech";
import { CameraView, useCameraPermissions } from "expo-camera";
import { GlassCard } from "../components/GlassCard";
import { PrimaryButton } from "../components/PrimaryButton";
import { RecordingButton } from "../components/RecordingButton";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { synthesizePollySpeech } from "../services/pollyService";
import { useAppTheme } from "../theme/ThemeProvider";
import {
  InterviewDifficulty,
  InterviewDomain,
  InterviewTopic,
  useAppState,
} from "../state/AppState";
import type { InterviewRubric } from "../services/interviewService";

const domains: InterviewDomain[] = ["SDE", "Android", "Web", "ML", "HR"];
const difficulties: InterviewDifficulty[] = ["Easy", "Medium", "Hard"];
const focusTopics: Array<InterviewTopic | "Mixed"> = ["Mixed", "DSA", "System Design", "Behavioral", "Domain"];
const questionCounts = [3, 5, 7];

type OptionalFaceDetectorModule = {
  detectFacesAsync: (uri: string, options?: Record<string, unknown>) => Promise<{
    faces: Array<{
      bounds: {
        origin: { x: number; y: number };
        size: { width: number; height: number };
      };
      leftEyePosition?: { x: number; y: number };
      rightEyePosition?: { x: number; y: number };
      leftEyeOpenProbability?: number;
      rightEyeOpenProbability?: number;
    }>;
    image: { width: number; height: number };
  }>;
  FaceDetectorMode?: { fast?: number };
  FaceDetectorLandmarks?: { all?: number };
  FaceDetectorClassifications?: { all?: number };
};

function loadOptionalFaceDetector(): OptionalFaceDetectorModule | null {
  try {
    const mod = require("expo-face-detector") as OptionalFaceDetectorModule;
    if (mod && typeof mod.detectFacesAsync === "function") {
      return mod;
    }
  } catch {
    // Expo Go may not include this native module; fallback path is handled in UI.
  }
  return null;
}

function formatSeconds(seconds: number) {
  const mins = Math.floor(seconds / 60)
    .toString()
    .padStart(2, "0");
  const secs = Math.floor(seconds % 60)
    .toString()
    .padStart(2, "0");
  return `${mins}:${secs}`;
}

function targetDurationForDifficulty(difficulty: InterviewDifficulty): number {
  if (difficulty === "Easy") return 45;
  if (difficulty === "Hard") return 95;
  return 70;
}

export function InterviewScreen() {
  const { theme } = useAppTheme();
  const {
    coding,
    interview,
    startInterview,
    submitInterviewAnswer,
    resetInterview,
    isSubmittingInterviewAnswer,
    lastError,
    clearLastError,
  } = useAppState();

  const [cameraPermission, requestCameraPermission] = useCameraPermissions();
  const [audioPermission, setAudioPermission] = useState(false);
  const [recording, setRecording] = useState<Audio.Recording | null>(null);
  const [recordedSeconds, setRecordedSeconds] = useState(0);
  const [lastClipUri, setLastClipUri] = useState("");
  const [answerDraft, setAnswerDraft] = useState("");
  const [status, setStatus] = useState("Choose configuration and start a real interview run.");
  const [busy, setBusy] = useState(false);
  const [latestScore, setLatestScore] = useState<number | null>(null);
  const [latestFeedback, setLatestFeedback] = useState("");
  const [latestRubric, setLatestRubric] = useState<InterviewRubric | null>(null);
  const [latestStrengths, setLatestStrengths] = useState<string[]>([]);
  const [latestImprovements, setLatestImprovements] = useState<string[]>([]);
  const [showSetupCard, setShowSetupCard] = useState(true);
  const [voiceStatus, setVoiceStatus] = useState("Interviewer voice is idle.");
  const [isSpeakingQuestion, setIsSpeakingQuestion] = useState(false);
  const [framingStatus, setFramingStatus] = useState("Run a check to verify face + eyes detection.");
  const [isCheckingFraming, setIsCheckingFraming] = useState(false);

  const [selectedDomain, setSelectedDomain] = useState<InterviewDomain>("SDE");
  const [selectedDifficulty, setSelectedDifficulty] = useState<InterviewDifficulty>("Medium");
  const [selectedQuestionCount, setSelectedQuestionCount] = useState(5);
  const [selectedFocusTopic, setSelectedFocusTopic] = useState<InterviewTopic | "Mixed">("Mixed");

  const optionalFaceDetector = useMemo(() => loadOptionalFaceDetector(), []);
  const hasAdvancedFaceDetection = Boolean(optionalFaceDetector);

  const cameraRef = useRef<CameraView | null>(null);
  const interviewerSoundRef = useRef<Audio.Sound | null>(null);
  const interviewerCleanupRef = useRef<null | (() => Promise<void>)>(null);
  const spokenQuestionIdRef = useRef("");
  const framingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const currentQuestion = interview.questions[interview.currentIndex];
  const currentHints = currentQuestion?.hints ?? [];
  const canUseCamera = cameraPermission?.granted === true;
  const targetDurationSec = targetDurationForDifficulty(selectedDifficulty);

  const averageScore = useMemo(() => {
    if (interview.answers.length === 0) return 0;
    const total = interview.answers.reduce((sum, item) => sum + item.score, 0);
    return Math.round(total / interview.answers.length);
  }, [interview.answers]);

  const questionById = useMemo(() => {
    const map = new Map<string, { topic: InterviewTopic; prompt: string }>();
    for (const question of interview.questions) {
      map.set(question.id, { topic: question.topic, prompt: question.prompt });
    }
    return map;
  }, [interview.questions]);

  const clearRecordingTimer = () => {
    if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
  };

  const clearFramingTimer = () => {
    if (framingIntervalRef.current) {
      clearInterval(framingIntervalRef.current);
      framingIntervalRef.current = null;
    }
  };

  const releaseInterviewerAudio = async () => {
    try {
      await Speech.stop();
    } catch {
      // best effort stop
    }

    try {
      if (interviewerSoundRef.current) {
        await interviewerSoundRef.current.unloadAsync();
        interviewerSoundRef.current = null;
      }
    } catch {
      // best effort release
    }

    try {
      if (interviewerCleanupRef.current) {
        await interviewerCleanupRef.current();
        interviewerCleanupRef.current = null;
      }
    } catch {
      // best effort release
    }
  };

  const speakQuestionAloud = async (text: string) => {
    const prompt = text.trim();
    if (!prompt) {
      return;
    }

    try {
      setIsSpeakingQuestion(true);
      setVoiceStatus("Interviewer is speaking...");

      await releaseInterviewerAudio();
      const audioAsset = await synthesizePollySpeech(prompt);
      interviewerCleanupRef.current = audioAsset.cleanup;

      const { sound } = await Audio.Sound.createAsync(
        { uri: audioAsset.fileUri },
        { shouldPlay: true, volume: 1.0 }
      );

      interviewerSoundRef.current = sound;
      sound.setOnPlaybackStatusUpdate((playback) => {
        if (!playback.isLoaded) {
          return;
        }

        if (playback.didJustFinish) {
          void releaseInterviewerAudio();
          setIsSpeakingQuestion(false);
          setVoiceStatus(`Last question played using ${audioAsset.voiceId} voice.`);
        }
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : "Voice playback failed";
      try {
        setVoiceStatus(`Polly unavailable (${message}). Using device voice.`);
        setIsSpeakingQuestion(true);
        Speech.speak(prompt, {
          language: "en-US",
          rate: 0.95,
          pitch: 1.0,
          onDone: () => {
            setIsSpeakingQuestion(false);
            setVoiceStatus("Question played using device voice.");
          },
          onStopped: () => {
            setIsSpeakingQuestion(false);
            setVoiceStatus("Interviewer voice stopped.");
          },
          onError: () => {
            setIsSpeakingQuestion(false);
            setVoiceStatus("Both Polly and device voice playback failed.");
          },
        });
      } catch {
        setIsSpeakingQuestion(false);
        setVoiceStatus(`Interviewer voice unavailable: ${message}`);
      }
    }
  };

  const runFramingCheck = async () => {
    if (!canUseCamera) {
      setFramingStatus("Camera permission is required for face + eyes detection.");
      return;
    }

    if (!optionalFaceDetector) {
      setFramingStatus(
        "Advanced face detection is unavailable in Expo Go. Camera guide is active; use a development build for strict eye detection."
      );
      return;
    }

    if (!cameraRef.current || busy || isSubmittingInterviewAnswer || recording) {
      return;
    }

    try {
      setIsCheckingFraming(true);
      const capture = await cameraRef.current.takePictureAsync({ quality: 0.28, skipProcessing: true });
      const detection = await optionalFaceDetector.detectFacesAsync(capture.uri, {
        mode: optionalFaceDetector.FaceDetectorMode?.fast ?? 1,
        detectLandmarks: optionalFaceDetector.FaceDetectorLandmarks?.all ?? 2,
        runClassifications: optionalFaceDetector.FaceDetectorClassifications?.all ?? 2,
      });

      const primaryFace = detection.faces[0];
      if (!primaryFace) {
        setFramingStatus("No face detected. Keep your full face in frame and hold still.");
        return;
      }

      const imageWidth = detection.image.width || 1;
      const imageHeight = detection.image.height || 1;
      const centerX = primaryFace.bounds.origin.x + primaryFace.bounds.size.width / 2;
      const centerY = primaryFace.bounds.origin.y + primaryFace.bounds.size.height / 2;
      const centeredHorizontally = Math.abs(centerX - imageWidth / 2) <= imageWidth * 0.2;
      const centeredVertically = Math.abs(centerY - imageHeight / 2) <= imageHeight * 0.22;
      const faceWidthRatio = primaryFace.bounds.size.width / imageWidth;
      const eyeLandmarksDetected = Boolean(primaryFace.leftEyePosition && primaryFace.rightEyePosition);
      const eyeProbabilitiesAvailable =
        typeof primaryFace.leftEyeOpenProbability === "number" &&
        typeof primaryFace.rightEyeOpenProbability === "number";

      if (!eyeLandmarksDetected) {
        setFramingStatus("Face detected, but eyes were not detected. Improve lighting and keep camera at eye level.");
        return;
      }

      if (faceWidthRatio < 0.17) {
        setFramingStatus("Face + eyes detected. Move closer to camera for better tracking.");
        return;
      }

      if (faceWidthRatio > 0.62) {
        setFramingStatus("Face + eyes detected. Move slightly back to keep head and shoulders in frame.");
        return;
      }

      if (!centeredHorizontally || !centeredVertically) {
        setFramingStatus("Face + eyes detected. Re-center your face inside the frame guide.");
        return;
      }

      if (eyeProbabilitiesAvailable) {
        const leftOpen = (primaryFace.leftEyeOpenProbability ?? 0) > 0.25;
        const rightOpen = (primaryFace.rightEyeOpenProbability ?? 0) > 0.25;
        if (!leftOpen || !rightOpen) {
          setFramingStatus("Face + eyes detected. Keep both eyes visible and avoid looking down.");
          return;
        }
      }

      setFramingStatus("Face + eyes detection is healthy. Framing looks interview-ready.");
    } catch {
      setFramingStatus("Framing check failed. Reopen camera permissions and try again.");
    } finally {
      setIsCheckingFraming(false);
    }
  };

  useEffect(() => {
    return () => {
      clearRecordingTimer();
      clearFramingTimer();
      void releaseInterviewerAudio();
    };
  }, []);

  useEffect(() => {
    if (!interview.active || !currentQuestion) {
      return;
    }

    if (spokenQuestionIdRef.current === currentQuestion.id) {
      return;
    }

    spokenQuestionIdRef.current = currentQuestion.id;
    void speakQuestionAloud(currentQuestion.prompt);
  }, [currentQuestion, interview.active]);

  useEffect(() => {
    if (!interview.active || !canUseCamera) {
      clearFramingTimer();
      return;
    }

    clearFramingTimer();
    framingIntervalRef.current = setInterval(() => {
      void runFramingCheck();
    }, 9000);

    return () => {
      clearFramingTimer();
    };
  }, [canUseCamera, interview.active, recording, busy, isSubmittingInterviewAnswer]);

  const ensureAudioPermission = async () => {
    if (audioPermission) return true;
    const response = await Audio.requestPermissionsAsync();
    setAudioPermission(response.granted);
    return response.granted;
  };

  const requestAllPermissions = async () => {
    if (!cameraPermission?.granted) {
      await requestCameraPermission();
    }
    await ensureAudioPermission();
    void runFramingCheck();
  };

  const resetAudioMode = async () => {
    try {
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: false,
        playsInSilentModeIOS: true,
        shouldDuckAndroid: true,
        playThroughEarpieceAndroid: false,
      });
    } catch {
      // Best effort cleanup path.
    }
  };

  const beginSession = () => {
    clearLastError();
    spokenQuestionIdRef.current = "";
    startInterview({
      domain: selectedDomain,
      difficulty: selectedDifficulty,
      questionCount: selectedQuestionCount,
      focusTopic: selectedFocusTopic,
    });
    setShowSetupCard(false);
    setStatus("Session started. Record or type your answer, then evaluate.");
    setVoiceStatus("Interviewer will read the current question aloud.");
    setLatestFeedback("");
    setLatestScore(null);
    setLatestRubric(null);
    setLatestStrengths([]);
    setLatestImprovements([]);
    setRecordedSeconds(0);
    setAnswerDraft("");
    setLastClipUri("");
  };

  const discardRecording = async () => {
    if (!recording) {
      return;
    }

    clearRecordingTimer();
    try {
      await recording.stopAndUnloadAsync();
    } catch {
      // ignore cleanup failure
    } finally {
      await resetAudioMode();
      setRecording(null);
      setRecordedSeconds(0);
    }
  };

  const startRecording = async () => {
    if (!interview.active) {
      setStatus("Start a session first to answer questions.");
      return;
    }

    if (recording) {
      return;
    }

    try {
      setBusy(true);
      await releaseInterviewerAudio();
      setIsSpeakingQuestion(false);
      const micGranted = await ensureAudioPermission();
      if (!micGranted) {
        setStatus("Microphone permission is required to capture answers.");
        return;
      }

      await Audio.setAudioModeAsync({
        allowsRecordingIOS: true,
        playsInSilentModeIOS: true,
        shouldDuckAndroid: true,
        playThroughEarpieceAndroid: false,
      });

      const rec = new Audio.Recording();
      await rec.prepareToRecordAsync(Audio.RecordingOptionsPresets.HIGH_QUALITY);
      await rec.startAsync();
      setRecording(rec);
      setRecordedSeconds(0);
      setStatus("Recording started. Speak with structure, then stop to evaluate.");

      const timer = setInterval(async () => {
        try {
          const stat = await rec.getStatusAsync();
          if (stat.isRecording) {
            setRecordedSeconds((stat.durationMillis ?? 0) / 1000);
          }
        } catch {
          clearRecordingTimer();
        }
      }, 300);

      timerRef.current = timer;
    } catch {
      clearRecordingTimer();
      setRecording(null);
      setStatus("Could not start recording. Try again.");
    } finally {
      setBusy(false);
    }
  };

  const evaluateCurrentAnswer = async () => {
    if (!interview.active || !currentQuestion) {
      return;
    }

    if (!recording && answerDraft.trim().length < 20) {
      setStatus("Write at least a short answer draft if you are not using audio recording.");
      return;
    }

    try {
      setBusy(true);

      let uri = lastClipUri;
      let durationSec = recordedSeconds;

      if (recording) {
        clearRecordingTimer();
        await recording.stopAndUnloadAsync();
        uri = recording.getURI() ?? "";
        setLastClipUri(uri);
      }

      if (!uri) {
        uri = `typed://manual-${Date.now()}`;
      }

      if (durationSec <= 0) {
        durationSec = Math.max(12, Math.min(120, Math.round(answerDraft.trim().split(/\s+/).length / 1.8)));
      }

      setStatus("Evaluating answer with AI rubric...");

      const result = await submitInterviewAnswer({
        audioUri: uri,
        durationSec,
        transcript: answerDraft.trim(),
      });

      setLatestScore(result.score);
      setLatestFeedback(result.feedback);
      setLatestRubric(result.rubric);
      setLatestStrengths(result.strengths);
      setLatestImprovements(result.improvements);

      setStatus(
        result.completed
          ? "Interview complete. Review your rubric breakdown and restart for another run."
          : "Answer evaluated. Move to next question and keep improving weak dimensions."
      );

      setAnswerDraft("");
      setRecordedSeconds(0);
    } catch {
      setStatus("Could not evaluate current answer. Try again.");
    } finally {
      await resetAudioMode();
      setRecording(null);
      setBusy(false);
    }
  };

  return (
    <ScreenContainer
      title="AI Interview"
      subtitle="Rewritten flow: better prompts, hints, rubric, strengths, and actionables"
    >
      {(!interview.active && !interview.completed) || showSetupCard ? (
      <GlassCard>
        <ThemedText variant="title" strong>
          Session Setup
        </ThemedText>
        <ThemedText variant="body" muted style={{ marginTop: 6 }}>
          Coding depth {coding.depth}% is connected to readiness and adaptive recommendations.
        </ThemedText>

        <ThemedText variant="label" muted style={{ marginTop: 12 }}>
          Domain
        </ThemedText>
        <View style={styles.chipWrap}>
          {domains.map((domain) => (
            <ChoiceChip
              key={domain}
              label={domain}
              selected={selectedDomain === domain}
              onPress={() => setSelectedDomain(domain)}
            />
          ))}
        </View>

        <ThemedText variant="label" muted style={{ marginTop: 12 }}>
          Difficulty
        </ThemedText>
        <View style={styles.chipWrap}>
          {difficulties.map((difficulty) => (
            <ChoiceChip
              key={difficulty}
              label={difficulty}
              selected={selectedDifficulty === difficulty}
              onPress={() => setSelectedDifficulty(difficulty)}
            />
          ))}
        </View>

        <ThemedText variant="label" muted style={{ marginTop: 12 }}>
          Focus Topic
        </ThemedText>
        <View style={styles.chipWrap}>
          {focusTopics.map((topic) => (
            <ChoiceChip
              key={topic}
              label={topic}
              selected={selectedFocusTopic === topic}
              onPress={() => setSelectedFocusTopic(topic)}
            />
          ))}
        </View>

        <ThemedText variant="label" muted style={{ marginTop: 12 }}>
          Question Count
        </ThemedText>
        <View style={styles.chipWrap}>
          {questionCounts.map((count) => (
            <ChoiceChip
              key={count}
              label={`${count}`}
              selected={selectedQuestionCount === count}
              onPress={() => setSelectedQuestionCount(count)}
            />
          ))}
        </View>

        <View style={{ flexDirection: "row", gap: 10, marginTop: 14 }}>
          <PrimaryButton
            label="Start Session"
            style={{ flex: 1 }}
            disabled={busy || isSubmittingInterviewAnswer}
            onPress={beginSession}
          />
          <PrimaryButton
            label={showSetupCard && interview.active ? "Hide Setup" : "Reset"}
            secondary
            style={{ flex: 1 }}
            disabled={busy || isSubmittingInterviewAnswer}
            onPress={() => {
              void (async () => {
                clearLastError();
                if (showSetupCard && interview.active) {
                  setShowSetupCard(false);
                  return;
                }
                await discardRecording();
                await releaseInterviewerAudio();
                resetInterview();
                spokenQuestionIdRef.current = "";
                setShowSetupCard(true);
                setStatus("Session reset. Configure and start again.");
                setLatestFeedback("");
                setLatestScore(null);
                setLatestRubric(null);
                setLatestStrengths([]);
                setLatestImprovements([]);
                setAnswerDraft("");
                setLastClipUri("");
              })();
            }}
          />
        </View>

        {!!lastError && (
          <ThemedText variant="body" style={{ marginTop: 10, color: theme.colors.danger }}>
            {lastError}
          </ThemedText>
        )}
      </GlassCard>
      ) : (
        <GlassCard>
          <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center", gap: 10 }}>
            <View style={{ flex: 1 }}>
              <ThemedText variant="label" muted>
                Active Setup
              </ThemedText>
              <ThemedText variant="body" strong style={{ marginTop: 4 }}>
                {selectedDomain} • {selectedDifficulty} • {selectedFocusTopic} • {selectedQuestionCount} question(s)
              </ThemedText>
            </View>
            <PrimaryButton
              label="Edit"
              secondary
              onPress={() => {
                setShowSetupCard(true);
              }}
            />
          </View>
        </GlassCard>
      )}

      {(interview.active || interview.completed) && (
        <>
          <GlassCard>
            <ThemedText variant="label" muted>
              {interview.completed
                ? "Session Summary"
                : `Question ${interview.currentIndex + 1} of ${interview.questions.length}`}
            </ThemedText>
            <ThemedText variant="title" strong style={{ marginTop: 6 }}>
              {interview.completed ? "Interview finished" : currentQuestion?.prompt ?? "Preparing question..."}
            </ThemedText>
            {!interview.completed && currentQuestion && (
              <View style={{ marginTop: 10, gap: 6 }}>
                <ThemedText variant="label" muted>
                  Hints
                </ThemedText>
                {currentHints.map((hint) => (
                  <ThemedText key={hint} variant="body" muted>
                    - {hint}
                  </ThemedText>
                ))}
              </View>
            )}
            <ThemedText variant="body" muted style={{ marginTop: 8 }}>
              {status}
            </ThemedText>

            {!interview.completed && currentQuestion && (
              <>
                <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
                  <PrimaryButton
                    label={isSpeakingQuestion ? "Speaking..." : "Read Question Aloud"}
                    secondary
                    style={{ flex: 1 }}
                    disabled={isSpeakingQuestion || busy || isSubmittingInterviewAnswer}
                    onPress={() => {
                      void speakQuestionAloud(currentQuestion.prompt);
                    }}
                  />
                  <PrimaryButton
                    label="Stop Voice"
                    style={{ flex: 1 }}
                    onPress={() => {
                      void (async () => {
                        await releaseInterviewerAudio();
                        setIsSpeakingQuestion(false);
                        setVoiceStatus("Interviewer voice stopped.");
                      })();
                    }}
                  />
                </View>
                <ThemedText variant="body" muted style={{ marginTop: 8 }}>
                  {voiceStatus}
                </ThemedText>
              </>
            )}

            {!interview.completed && (
              <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
                <PrimaryButton
                  label="Clear Draft"
                  secondary
                  style={{ flex: 1 }}
                  onPress={() => {
                    setAnswerDraft("");
                    setStatus("Draft cleared. Capture or type a fresh answer.");
                  }}
                />
                <PrimaryButton
                  label="Grant Camera + Mic"
                  style={{ flex: 1 }}
                  onPress={requestAllPermissions}
                />
              </View>
            )}
          </GlassCard>

          <GlassCard>
            <View style={styles.cameraHeadRow}>
              <ThemedText variant="title" strong>
                Face Framing
              </ThemedText>
              <ThemedText variant="body" muted>
                {canUseCamera ? (hasAdvancedFaceDetection ? "Live + Detect" : "Live guide") : "Permission needed"}
              </ThemedText>
            </View>

            <View
              style={[
                styles.cameraFrame,
                {
                  borderColor: theme.colors.border,
                  backgroundColor: theme.colors.cardAlt,
                },
              ]}
            >
              {canUseCamera ? (
                <>
                  <CameraView ref={cameraRef} style={StyleSheet.absoluteFill} facing="front" />
                  <View style={styles.framingGuideOuter}>
                    <View style={styles.framingGuideInner} />
                  </View>
                </>
              ) : (
                <View style={styles.cameraFallback}>
                  <ThemedText variant="body" muted>
                    Camera permission required for live framing.
                  </ThemedText>
                  <PrimaryButton
                    label="Allow Camera"
                    style={{ marginTop: 12, alignSelf: "stretch" }}
                    onPress={() => {
                      requestCameraPermission();
                    }}
                  />
                </View>
              )}
            </View>

            <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
              <PrimaryButton
                label={isCheckingFraming ? "Checking..." : "Run Face + Eye Check"}
                style={{ flex: 1 }}
                disabled={!canUseCamera || !hasAdvancedFaceDetection || isCheckingFraming || busy || isSubmittingInterviewAnswer}
                onPress={() => {
                  void runFramingCheck();
                }}
              />
            </View>
            <View style={styles.framingStatusRow}>
              {isCheckingFraming && <ActivityIndicator size="small" color={theme.colors.accent} />}
              <ThemedText variant="body" muted style={{ flex: 1 }}>
                {framingStatus}
              </ThemedText>
            </View>
          </GlassCard>

          <GlassCard>
            <ThemedText variant="title" strong>
              Answer Workspace
            </ThemedText>
            <ThemedText variant="body" muted style={{ marginTop: 6 }}>
              Use voice recording or type a transcript draft. AI evaluation uses both if available.
            </ThemedText>

            <View style={{ marginTop: 14 }}>
              <RecordingButton
                isRecording={!!recording}
                disabled={busy || !interview.active || isSubmittingInterviewAnswer}
                onPress={() => {
                  void (recording ? evaluateCurrentAnswer() : startRecording());
                }}
              />
            </View>

            <PrimaryButton
              label="Evaluate Typed Draft"
              secondary
              style={{ marginTop: 10 }}
              disabled={busy || !!recording || !interview.active || isSubmittingInterviewAnswer}
              onPress={evaluateCurrentAnswer}
            />

            {isSubmittingInterviewAnswer && (
              <ThemedText variant="body" muted style={{ marginTop: 10 }}>
                Evaluating answer quality and generating improvements...
              </ThemedText>
            )}

            <View style={styles.metaRow}>
              <ThemedText variant="label" muted>
                Duration
              </ThemedText>
              <ThemedText variant="body" strong>
                {formatSeconds(recordedSeconds)} / {formatSeconds(targetDurationSec)}
              </ThemedText>
            </View>

            <View
              style={{
                marginTop: 8,
                height: 8,
                borderRadius: 999,
                backgroundColor: theme.colors.cardAlt,
                overflow: "hidden",
              }}
            >
              <View
                style={{
                  width: `${Math.min(100, Math.round((recordedSeconds / targetDurationSec) * 100))}%`,
                  height: "100%",
                  backgroundColor: theme.colors.accent,
                }}
              />
            </View>

            <View style={styles.metaRow}>
              <ThemedText variant="label" muted>
                Last Clip
              </ThemedText>
              <ThemedText variant="body" muted style={{ maxWidth: "65%" }} numberOfLines={1}>
                {lastClipUri || "No recording yet"}
              </ThemedText>
            </View>

            <TextInput
              value={answerDraft}
              onChangeText={setAnswerDraft}
              multiline
              placeholder="Type your answer draft or transcript here for better AI evaluation quality..."
              placeholderTextColor={theme.colors.textMuted}
              style={[
                styles.answerInput,
                {
                  borderColor: theme.colors.border,
                  color: theme.colors.text,
                  backgroundColor: theme.colors.cardAlt,
                },
              ]}
            />
          </GlassCard>

          {latestScore !== null && latestRubric && (
            <GlassCard>
              <ThemedText variant="title" strong>
                Evaluation
              </ThemedText>
              <ThemedText variant="title" strong style={{ marginTop: 6 }}>
                Score {latestScore}/100
              </ThemedText>
              <ThemedText variant="body" muted style={{ marginTop: 6 }}>
                {latestFeedback}
              </ThemedText>

              <View style={{ marginTop: 12, gap: 10 }}>
                <RubricRow label="Content" value={latestRubric.content} />
                <RubricRow label="Structure" value={latestRubric.structure} />
                <RubricRow label="Clarity" value={latestRubric.clarity} />
                <RubricRow label="Confidence" value={latestRubric.confidence} />
              </View>

              <View style={{ marginTop: 12 }}>
                <ThemedText variant="label" muted>
                  Strengths
                </ThemedText>
                {latestStrengths.length === 0 ? (
                  <ThemedText variant="body" muted style={{ marginTop: 6 }}>
                    No strengths detected for this attempt.
                  </ThemedText>
                ) : (
                  latestStrengths.map((entry) => (
                    <ThemedText key={entry} variant="body" muted style={{ marginTop: 4 }}>
                      - {entry}
                    </ThemedText>
                  ))
                )}
              </View>

              <View style={{ marginTop: 12 }}>
                <ThemedText variant="label" muted>
                  Improvements
                </ThemedText>
                {latestImprovements.length === 0 ? (
                  <ThemedText variant="body" muted style={{ marginTop: 6 }}>
                    No improvement hints generated.
                  </ThemedText>
                ) : (
                  latestImprovements.map((entry) => (
                    <ThemedText key={entry} variant="body" muted style={{ marginTop: 4 }}>
                      - {entry}
                    </ThemedText>
                  ))
                )}
              </View>

              {interview.completed && (
                <View style={styles.summaryBox}>
                  <ThemedText variant="title" strong>
                    Final Session Score: {averageScore}/100
                  </ThemedText>
                  <ThemedText variant="body" muted style={{ marginTop: 6 }}>
                    {interview.answers.length} answers evaluated with rubric dimensions.
                  </ThemedText>
                  <PrimaryButton label="Start New Session" style={{ marginTop: 12 }} onPress={beginSession} />
                </View>
              )}
            </GlassCard>
          )}

          <GlassCard>
            <ThemedText variant="title" strong>
              Answer History
            </ThemedText>
            <View style={{ marginTop: 10, gap: 8 }}>
              {interview.answers.length === 0 ? (
                <ThemedText variant="body" muted>
                  No answers evaluated yet.
                </ThemedText>
              ) : (
                interview.answers.map((answer, index) => {
                  const info = questionById.get(answer.questionId);
                  return (
                    <View
                      key={`${answer.questionId}-${index}`}
                      style={[
                        styles.answerItem,
                        {
                          borderColor: theme.colors.border,
                          backgroundColor: theme.colors.cardAlt,
                        },
                      ]}
                    >
                      <ThemedText variant="body" strong>
                        Q{index + 1} ({info?.topic ?? "Domain"}) - Score {answer.score}
                      </ThemedText>
                      <ThemedText variant="body" muted style={{ marginTop: 4 }} numberOfLines={2}>
                        {answer.transcript || "No transcript captured"}
                      </ThemedText>
                    </View>
                  );
                })
              )}
            </View>
          </GlassCard>
        </>
      )}

      {Platform.OS === "ios" ? null : (
        <GlassCard>
          <ThemedText variant="body" muted>
            Tip: Android emulator preview is usually weaker than physical iPhone front camera quality.
          </ThemedText>
        </GlassCard>
      )}
    </ScreenContainer>
  );
}

type ChoiceChipProps = {
  label: string;
  selected: boolean;
  onPress: () => void;
};

function ChoiceChip({ label, selected, onPress }: ChoiceChipProps) {
  const { theme } = useAppTheme();

  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.choiceChip,
        {
          borderColor: selected ? theme.colors.accent : theme.colors.border,
          backgroundColor: selected ? theme.colors.accentSoft : theme.colors.cardAlt,
        },
      ]}
    >
      <ThemedText variant="body" strong={selected} style={{ color: selected ? theme.colors.accent : theme.colors.text }}>
        {label}
      </ThemedText>
    </Pressable>
  );
}

function RubricRow({ label, value }: { label: string; value: number }) {
  const { theme } = useAppTheme();

  return (
    <View>
      <View style={{ flexDirection: "row", justifyContent: "space-between", alignItems: "center" }}>
        <ThemedText variant="body" muted>
          {label}
        </ThemedText>
        <ThemedText variant="body" strong>
          {value}
        </ThemedText>
      </View>
      <View
        style={{
          marginTop: 4,
          height: 8,
          borderRadius: 999,
          backgroundColor: theme.colors.cardAlt,
          overflow: "hidden",
        }}
      >
        <View
          style={{
            width: `${Math.max(0, Math.min(value, 100))}%`,
            height: "100%",
            backgroundColor: theme.colors.accent,
          }}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  chipWrap: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 8,
  },
  choiceChip: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  cameraHeadRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  cameraFrame: {
    marginTop: 12,
    height: 240,
    borderWidth: 1,
    borderRadius: 16,
    overflow: "hidden",
  },
  framingGuideOuter: {
    ...StyleSheet.absoluteFillObject,
    alignItems: "center",
    justifyContent: "center",
    pointerEvents: "none",
  },
  framingGuideInner: {
    width: "62%",
    aspectRatio: 1,
    borderWidth: 2,
    borderColor: "rgba(255,255,255,0.85)",
    borderRadius: 999,
  },
  framingStatusRow: {
    marginTop: 10,
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  cameraFallback: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 16,
  },
  metaRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 12,
  },
  answerInput: {
    marginTop: 12,
    minHeight: 120,
    borderWidth: 1,
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 10,
    textAlignVertical: "top",
    fontSize: 15,
  },
  summaryBox: {
    marginTop: 14,
    borderRadius: 12,
    padding: 12,
    backgroundColor: "rgba(50, 112, 255, 0.12)",
  },
  answerItem: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
  },
});
