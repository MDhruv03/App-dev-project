import React, { useEffect, useMemo, useRef, useState } from "react";
import { ActivityIndicator, Platform, Pressable, StyleSheet, View } from "react-native";
import { useIsFocused } from "@react-navigation/native";
import { Audio } from "expo-av";
import { File } from "expo-file-system";
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

async function loadOptionalFaceDetector(): Promise<OptionalFaceDetectorModule | null> {
  if (Platform.OS === "web") {
    return null;
  }

  try {
    const module = (await import("expo-face-detector")) as unknown as {
      default?: OptionalFaceDetectorModule;
    } & OptionalFaceDetectorModule;
    const candidate = module.default ?? module;
    if (candidate && typeof candidate.detectFacesAsync === "function") {
      return candidate;
    }
  } catch {
    // Optional module unavailable on this runtime.
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

function clampNumber(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}

function targetDurationForDifficulty(difficulty: InterviewDifficulty): number {
  if (difficulty === "Easy") return 45;
  if (difficulty === "Hard") return 95;
  return 70;
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function normalizeSpeechText(value: string): string {
  return value
    .replace(/\bSDE\b/g, "software development engineer")
    .replace(/\bDSA\b/g, "data structures and algorithms")
    .replace(/\bSQL\b/g, "sequel")
    .replace(/\bML\b/g, "machine learning")
    .replace(/\bAPI\b/g, "A P I")
    .replace(/\bCI\/CD\b/g, "C I C D")
    .replace(/\s+/g, " ")
    .trim();
}

function stripSsml(value: string): string {
  return value
    .replace(/<[^>]*>/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function stableHash(value: string): number {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) >>> 0;
  }
  return hash;
}

function buildInterviewerSpeech(params: {
  question: string;
  isFirstQuestion: boolean;
  candidateName: string;
}) {
  const normalizedQuestion = normalizeSpeechText(params.question);
  const transitions = [
    "Let us walk through this carefully.",
    "Take a breath, and think out loud.",
    "I like your momentum, now go deeper.",
    "Great, let us challenge your reasoning a bit.",
  ];
  const closers = [
    "Anchor your answer with one concrete example.",
    "Name one trade-off before you conclude.",
    "Keep your structure clear: context, action, and result.",
    "Use measurable impact where possible.",
  ];

  const seed = stableHash(normalizedQuestion);
  const transition = transitions[seed % transitions.length];
  const closer = closers[seed % closers.length];
  const intro = params.isFirstQuestion
    ? params.candidateName
      ? `Hi ${params.candidateName}, welcome in. We will keep this conversational and practical.`
      : "Hi, welcome in. We will keep this conversational and practical."
    : transition;

  const plain = `${intro} ${normalizedQuestion} ${closer}`.replace(/\s+/g, " ").trim();
  const ssml = `<speak><prosody rate="90%" pitch="+2%">${escapeXml(intro)}<break time="350ms"/>${escapeXml(normalizedQuestion)}<break time="420ms"/>${escapeXml(closer)}</prosody></speak>`;

  return { plain, ssml };
}

export function InterviewScreen() {
  const { theme } = useAppTheme();
  const isFocused = useIsFocused();
  const {
    profile,
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
  const [showHints, setShowHints] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [voiceStatus, setVoiceStatus] = useState("Interviewer voice is idle.");
  const [isSpeakingQuestion, setIsSpeakingQuestion] = useState(false);
  const [framingStatus, setFramingStatus] = useState("Grant camera permission and run a framing check.");
  const [isCheckingFraming, setIsCheckingFraming] = useState(false);
  const [hasAdvancedFaceDetection, setHasAdvancedFaceDetection] = useState<boolean | null>(null);
  const [eyeContactScore, setEyeContactScore] = useState(0);
  const [eyeTrackingSamples, setEyeTrackingSamples] = useState(0);

  const [selectedDomain, setSelectedDomain] = useState<InterviewDomain>("SDE");
  const [selectedDifficulty, setSelectedDifficulty] = useState<InterviewDifficulty>("Medium");
  const [selectedQuestionCount, setSelectedQuestionCount] = useState(5);
  const [selectedFocusTopic, setSelectedFocusTopic] = useState<InterviewTopic | "Mixed">("Mixed");

  const cameraRef = useRef<CameraView | null>(null);
  const interviewerSoundRef = useRef<Audio.Sound | null>(null);
  const interviewerCleanupRef = useRef<null | (() => Promise<void>)>(null);
  const optionalFaceDetectorRef = useRef<OptionalFaceDetectorModule | "unavailable" | null>(null);
  const hasGreetedRef = useRef(false);
  const spokenQuestionIdRef = useRef("");
  const framingIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const getOptionalFaceDetector = async (): Promise<OptionalFaceDetectorModule | null> => {
    if (optionalFaceDetectorRef.current === "unavailable") {
      return null;
    }

    if (optionalFaceDetectorRef.current) {
      return optionalFaceDetectorRef.current;
    }

    const detector = await loadOptionalFaceDetector();
    if (detector) {
      optionalFaceDetectorRef.current = detector;
      setHasAdvancedFaceDetection(true);
      return detector;
    }

    optionalFaceDetectorRef.current = "unavailable";
    setHasAdvancedFaceDetection(false);
    return null;
  };

  const currentQuestion = interview.questions[interview.currentIndex];
  const currentHints = currentQuestion?.hints ?? [];
  const canUseCamera = cameraPermission?.granted === true;
  const targetDurationSec = targetDurationForDifficulty(selectedDifficulty);
  const isLiveInterview = interview.active && !interview.completed;

  const eyeContactHint = useMemo(() => {
    if (!canUseCamera) {
      return "Camera permission is required for eye-contact tracking.";
    }

    if (hasAdvancedFaceDetection === false) {
      return "Strict eye tracking needs a development build. Preview framing remains active.";
    }

    if (hasAdvancedFaceDetection === null || eyeTrackingSamples === 0) {
      return "Running initial eye-contact baseline...";
    }

    if (eyeContactScore >= 80) {
      return "Eye contact looks strong and stable.";
    }

    if (eyeContactScore >= 60) {
      return "Good base. Keep your gaze near the camera lens.";
    }

    return "Look at the camera lens more consistently for better presence.";
  }, [canUseCamera, eyeContactScore, eyeTrackingSamples, hasAdvancedFaceDetection]);

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

  const speakQuestionAloud = async (spokenText?: string, fallbackText?: string) => {
    const prompt = String(spokenText ?? "").trim();
    const plainFallback = String(fallbackText ?? stripSsml(prompt)).trim();

    if (!prompt || !plainFallback) {
      return;
    }

    try {
      setIsSpeakingQuestion(true);
      setVoiceStatus("Interviewer is speaking...");

      await releaseInterviewerAudio();
      await resetAudioMode();
      const audioAsset = await synthesizePollySpeech(prompt);
      interviewerCleanupRef.current = audioAsset.cleanup;

      const { sound } = await Audio.Sound.createAsync(
        { uri: audioAsset.fileUri },
        { shouldPlay: false, volume: 1.0 }
      );

      interviewerSoundRef.current = sound;
      await sound.playAsync();
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
        await resetAudioMode();
        setVoiceStatus(`Polly unavailable (${message}). Using device voice.`);
        setIsSpeakingQuestion(true);
        Speech.speak(plainFallback, {
          language: "en-US",
          rate: 0.9,
          pitch: 1.02,
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
      setFramingStatus("Camera permission is required for interview framing.");
      setEyeTrackingSamples(0);
      setEyeContactScore(0);
      return;
    }

    if (!cameraRef.current || busy || isSubmittingInterviewAnswer) {
      return;
    }

    try {
      setIsCheckingFraming(true);

      const pushEyeSample = (sample: number) => {
        const boundedSample = clampNumber(sample, 0, 100);
        setEyeTrackingSamples((previous) => previous + 1);
        setEyeContactScore((previous) => {
          if (previous <= 0) {
            return boundedSample;
          }
          return clampNumber(Math.round(previous * 0.74 + boundedSample * 0.26), 0, 100);
        });
      };

      const detector = await getOptionalFaceDetector();
      if (!detector) {
        setEyeTrackingSamples(0);
        setEyeContactScore(0);
        setFramingStatus(
          "Camera is live and framing guide is active. Advanced eye tracking is unavailable on this runtime (use a development build for strict eye detection)."
        );
        return;
      }

      const capture = await cameraRef.current.takePictureAsync({ quality: 0.24, skipProcessing: true });
      const detection = await detector.detectFacesAsync(capture.uri, {
        mode: detector.FaceDetectorMode?.fast ?? 1,
        detectLandmarks: detector.FaceDetectorLandmarks?.all ?? 2,
        runClassifications: detector.FaceDetectorClassifications?.all ?? 2,
      });

      const primaryFace = detection.faces[0];
      if (!primaryFace) {
        pushEyeSample(10);
        setFramingStatus("No face detected. Keep your face centered and hold still for 1-2 seconds.");
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

      if (!eyeLandmarksDetected) {
        pushEyeSample(28);
        setFramingStatus("Face detected, but eyes not stable yet. Improve front lighting and keep camera at eye level.");
        return;
      }

      if (faceWidthRatio < 0.17) {
        pushEyeSample(42);
        setFramingStatus("Eyes detected. Move slightly closer for better eye tracking quality.");
        return;
      }

      if (faceWidthRatio > 0.62) {
        pushEyeSample(45);
        setFramingStatus("Eyes detected. Move slightly back so head and shoulders remain visible.");
        return;
      }

      if (!centeredHorizontally || !centeredVertically) {
        pushEyeSample(52);
        setFramingStatus("Eyes detected. Re-center your face inside the framing guide.");
        return;
      }

      pushEyeSample(92);
      setFramingStatus("Face + eyes detection healthy. You are interview-ready.");
    } catch {
      setEyeTrackingSamples(0);
      setEyeContactScore(0);
      setFramingStatus("Camera check failed. Reopen permissions and retry.");
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
    if (isFocused) {
      return;
    }

    clearFramingTimer();
    setIsCheckingFraming(false);
    setIsSpeakingQuestion(false);
    setVoiceStatus("Interviewer paused because Interview tab is not active.");
    void releaseInterviewerAudio();
  }, [isFocused]);

  useEffect(() => {
    if (!isFocused || !interview.active || !canUseCamera) {
      clearFramingTimer();
      return;
    }

    void runFramingCheck();

    clearFramingTimer();
    framingIntervalRef.current = setInterval(() => {
      void runFramingCheck();
    }, 3200);

    return () => {
      clearFramingTimer();
    };
  }, [canUseCamera, interview.active, recording, busy, isFocused, isSubmittingInterviewAnswer]);

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
    hasGreetedRef.current = false;
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
    setShowHints(false);
    setShowHistory(false);
    setEyeContactScore(0);
    setEyeTrackingSamples(0);
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
      setLastClipUri("");
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

  const stopRecording = async () => {
    if (!recording) {
      return;
    }

    try {
      setBusy(true);
      clearRecordingTimer();
      await recording.stopAndUnloadAsync();
      const uri = recording.getURI() ?? "";
      setLastClipUri(uri);
      setStatus(
        uri
          ? "Recording captured. Add a short recap if needed, then evaluate."
          : "Recording stopped, but no audio file was detected. Try again."
      );
    } catch {
      setStatus("Could not stop recording cleanly. Please retry.");
    } finally {
      await resetAudioMode();
      setRecording(null);
      setBusy(false);
    }
  };

  const evaluateCurrentAnswer = async () => {
    if (!interview.active || !currentQuestion) {
      return;
    }

    if (recording) {
      setStatus("Stop recording first, then evaluate your response.");
      return;
    }

    if (!lastClipUri) {
      setStatus("Record your response first, then submit and evaluate.");
      return;
    }

    try {
      setBusy(true);

      const uri = lastClipUri;
      let durationSec = recordedSeconds;
      if (durationSec <= 0) {
        durationSec = targetDurationSec;
      }

      let audioBase64 = "";
      if (uri && !uri.startsWith("typed://")) {
        try {
          const audioFile = new File(uri);
          if (audioFile.exists) {
            audioBase64 = await audioFile.base64();
          }
        } catch {
          // best-effort audio payload for backend transcription
        }
      }

      setStatus("Evaluating answer with AI rubric...");

      const result = await submitInterviewAnswer({
        audioUri: uri,
        durationSec,
        audioBase64: audioBase64 || undefined,
        transcript: "",
      });

      setLatestScore(result.score);
      setLatestFeedback(result.feedback);
      setLatestRubric(result.rubric);
      setLatestStrengths(result.strengths);
      setLatestImprovements(result.improvements);

      setStatus(
        result.completed
          ? "Interview complete. Review your rubric breakdown and restart for another run."
          : "Answer evaluated. Next question is now adapted from this response."
      );

      setAnswerDraft("");
      setRecordedSeconds(0);
      setLastClipUri("");
    } catch {
      setStatus("Could not evaluate current answer. Try again.");
    } finally {
      await resetAudioMode();
      setBusy(false);
    }
  };

  return (
    <ScreenContainer
      title="AI Interview"
      subtitle="Guided, adaptive interview flow with voice and coaching"
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
      ) : null}

      {(interview.active || interview.completed) && (
        <>
          <GlassCard>
            <ThemedText variant="label" muted>
              {interview.completed
                ? "Session Summary"
                : `Question ${interview.currentIndex + 1} of ${interview.questions.length}`}
            </ThemedText>
            <ThemedText variant="title" strong style={{ marginTop: 6 }} numberOfLines={isLiveInterview ? 3 : undefined}>
              {interview.completed ? "Interview finished" : currentQuestion?.prompt ?? "Preparing question..."}
            </ThemedText>
            {!interview.completed && currentQuestion && !isLiveInterview && (
              <View style={{ marginTop: 10, gap: 8 }}>
                <PrimaryButton
                  label={showHints ? "Hide Hints" : "Show Hints"}
                  secondary
                  onPress={() => {
                    setShowHints((prev) => !prev);
                  }}
                />
                {showHints && (
                  <View style={{ gap: 6 }}>
                    {currentHints.map((hint) => (
                      <ThemedText key={hint} variant="body" muted>
                        - {hint}
                      </ThemedText>
                    ))}
                  </View>
                )}
              </View>
            )}
            <ThemedText variant="body" muted style={{ marginTop: 8 }} numberOfLines={isLiveInterview ? 2 : undefined}>
              {status}
            </ThemedText>

            {!interview.completed && currentQuestion && (
              <>
                <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
                  <PrimaryButton
                    label={isSpeakingQuestion ? "Speaking..." : "Read Prompt"}
                    secondary
                    style={{ flex: 1 }}
                    disabled={isSpeakingQuestion || busy || isSubmittingInterviewAnswer}
                    onPress={() => {
                      const isFirstQuestion = interview.currentIndex === 0 && interview.answers.length === 0;
                      const cleanName = profile.name.trim();
                      const shouldUseName = cleanName.length > 0 && cleanName.toLowerCase() !== "your name";
                      const shouldGreet = isFirstQuestion && !hasGreetedRef.current;
                      if (shouldGreet) {
                        hasGreetedRef.current = true;
                      }
                      const speech = buildInterviewerSpeech({
                        question: currentQuestion.prompt,
                        isFirstQuestion: shouldGreet,
                        candidateName: shouldUseName ? cleanName : "",
                      });
                      void speakQuestionAloud(speech.ssml, speech.plain);
                    }}
                  />
                  <PrimaryButton
                    label="Stop"
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
          </GlassCard>

          {isLiveInterview && (
            <GlassCard>
              <View style={styles.cameraHeadRow}>
                <ThemedText variant="title" strong>
                  Response Controls
                </ThemedText>
                <ThemedText variant="body" muted>
                  {recording ? "Recording" : "Ready"}
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
                      Camera permission required for validation.
                    </ThemedText>
                    <PrimaryButton
                      label="Turn Camera On"
                      style={{ marginTop: 12, alignSelf: "stretch" }}
                      onPress={() => {
                        requestCameraPermission();
                      }}
                    />
                  </View>
                )}
              </View>

              <View style={styles.compactActionRow}>
                <PrimaryButton
                  label="Camera + Validate"
                  secondary
                  style={{ flex: 1 }}
                  onPress={requestAllPermissions}
                />
                <PrimaryButton
                  label={isCheckingFraming ? "Validating..." : "Validate Again"}
                  style={{ flex: 1 }}
                  disabled={!canUseCamera || isCheckingFraming || busy || isSubmittingInterviewAnswer}
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

              <View style={styles.metaRow}>
                <ThemedText variant="label" muted>
                  Eye Contact
                </ThemedText>
                <ThemedText variant="body" strong>
                  {hasAdvancedFaceDetection ? `${eyeContactScore}%` : "Preview"}
                </ThemedText>
              </View>

              <View style={[styles.scoreTrack, { backgroundColor: theme.colors.cardAlt }]}>
                <View
                  style={{
                    width: `${Math.max(0, Math.min(100, eyeContactScore))}%`,
                    height: "100%",
                    backgroundColor: theme.colors.accent,
                  }}
                />
              </View>
              <ThemedText variant="body" muted style={{ marginTop: 8 }}>
                {eyeContactHint}
              </ThemedText>

              <View style={{ marginTop: 14 }}>
                <RecordingButton
                  isRecording={!!recording}
                  disabled={busy || !interview.active || isSubmittingInterviewAnswer}
                  startLabel="Record Response"
                  stopLabel="Stop Recording"
                  onPress={() => {
                    void (recording ? stopRecording() : startRecording());
                  }}
                />
              </View>

              <View style={styles.compactActionRow}>
                <PrimaryButton
                  label="Submit & Evaluate"
                  secondary
                  style={{ flex: 1 }}
                  disabled={busy || !!recording || !lastClipUri || !interview.active || isSubmittingInterviewAnswer}
                  onPress={evaluateCurrentAnswer}
                />
              </View>

              <View style={styles.metaRow}>
                <ThemedText variant="label" muted>
                  Duration
                </ThemedText>
                <ThemedText variant="body" strong>
                  {formatSeconds(recordedSeconds)} / {formatSeconds(targetDurationSec)}
                </ThemedText>
              </View>

              <View style={[styles.scoreTrack, { backgroundColor: theme.colors.cardAlt }]}>
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
                <ThemedText variant="body" muted style={{ maxWidth: "62%" }} numberOfLines={1}>
                  {lastClipUri || "No recording yet"}
                </ThemedText>
              </View>

              {isSubmittingInterviewAnswer && (
                <ThemedText variant="body" muted style={{ marginTop: 8 }}>
                  Evaluating response and moving to next question...
                </ThemedText>
              )}
            </GlassCard>
          )}

          {!isLiveInterview && latestScore !== null && latestRubric && (
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

          {!isLiveInterview && (
            <GlassCard>
              <View style={styles.cameraHeadRow}>
                <ThemedText variant="title" strong>
                  Answer History
                </ThemedText>
                <PrimaryButton
                  label={showHistory ? "Hide" : "Show"}
                  secondary
                  onPress={() => {
                    setShowHistory((prev) => !prev);
                  }}
                />
              </View>
              {showHistory && (
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
              )}
            </GlassCard>
          )}
        </>
      )}

      {isLiveInterview || Platform.OS === "ios" ? null : (
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
  liveNoScrollContent: {
    flex: 1,
    gap: 12,
    paddingBottom: 10,
  },
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
    gap: 10,
  },
  studioHeaderRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    gap: 10,
  },
  liveStudioCard: {
    flex: 1,
    minHeight: 0,
  },
  studioSplit: {
    marginTop: 12,
    gap: 12,
    flex: 1,
    minHeight: 0,
  },
  studioSplitWide: {
    flexDirection: "row",
    alignItems: "stretch",
  },
  cameraPane: {
    flex: 1,
    minHeight: 0,
  },
  responsePane: {
    flex: 1,
    minHeight: 0,
  },
  compactActionRow: {
    flexDirection: "row",
    gap: 8,
    marginTop: 10,
  },
  cameraFrame: {
    marginTop: 12,
    height: 218,
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
  scoreTrack: {
    marginTop: 8,
    height: 8,
    borderRadius: 999,
    overflow: "hidden",
  },
  answerInputCompact: {
    marginTop: 10,
    minHeight: 102,
    maxHeight: 132,
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
