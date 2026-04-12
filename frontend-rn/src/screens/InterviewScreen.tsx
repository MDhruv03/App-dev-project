import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  ActivityIndicator,
  AppState,
  type AppStateStatus,
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  View,
} from "react-native";
import { useIsFocused } from "@react-navigation/native";
import { Audio } from "expo-av";
import { File } from "expo-file-system";
import * as Speech from "expo-speech";
import { CameraView, useCameraPermissions } from "expo-camera";
import { SafeAreaView } from "react-native-safe-area-context";
import { GlassCard } from "../components/GlassCard";
import { InterviewHeader } from "../components/InterviewHeader";
import { PrimaryButton } from "../components/PrimaryButton";
import { RecordingButton } from "../components/RecordingButton";
import { ScreenContainer } from "../components/ScreenContainer";
import { ThemedText } from "../components/ThemedText";
import { fetchInterviewReaction } from "../services/interviewService";
import { synthesizePollySpeech } from "../services/pollyService";
import { validateVisionFrame } from "../services/visionClient";
import { useAppTheme } from "../theme/ThemeProvider";
import {
  InterviewDifficulty,
  InterviewDomain,
  InterviewTopic,
  useAppState,
} from "../state/AppState";
import type { InterviewRubric } from "../services/interviewService";

// ─── Constants ────────────────────────────────────────────────────────────────

const DOMAINS: InterviewDomain[] = ["SDE", "Android", "Web", "ML", "HR"];
const DIFFICULTIES: InterviewDifficulty[] = ["Easy", "Medium", "Hard"];
const FOCUS_TOPICS: Array<InterviewTopic | "Mixed"> = [
  "Mixed", "DSA", "System Design", "Behavioral", "Domain",
];
const QUESTION_COUNTS = [3, 5, 7];
const HEARTBEAT_INTERVAL_MS = 3000;
const FAILURE_DELTA = 18;
const HEARTBEAT_ERROR_DECAY = 8;
const LOW_SCORE_THRESHOLD = 30;
const LOW_SCORE_MAX_MS = 10000;
const SCORE_GREEN = 70;
const SCORE_YELLOW = 40;

// ─── Pure helpers ─────────────────────────────────────────────────────────────

function formatSeconds(s: number): string {
  const m = Math.floor(s / 60).toString().padStart(2, "0");
  const sec = Math.floor(s % 60).toString().padStart(2, "0");
  return `${m}:${sec}`;
}

function clampNumber(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v));
}

function targetDurationForDifficulty(d: InterviewDifficulty): number {
  if (d === "Easy") return 45;
  if (d === "Hard") return 95;
  return 70;
}

function escapeXml(v: string): string {
  return v
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

function normalizeSpeechText(v: string): string {
  return v
    .replace(/\bSDE\b/g, "software development engineer")
    .replace(/\bDSA\b/g, "data structures and algorithms")
    .replace(/\bSQL\b/g, "sequel")
    .replace(/\bML\b/g, "machine learning")
    .replace(/\bAPI\b/g, "A P I")
    .replace(/\bCI\/CD\b/g, "C I C D")
    .replace(/\s+/g, " ")
    .trim();
}

function stripSsml(v: string): string {
  return v.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim();
}

const TRANSITIONS = [
  "Let us walk through this carefully.",
  "Take a breath and think out loud.",
  "Good — now let us go a level deeper.",
  "Let us challenge your reasoning a little.",
  "Build on what you just said.",
  "Keep the energy — next question.",
  "Solid foundation. Let us add some nuance.",
  "I like the direction. Push further.",
];

const CLOSERS = [
  "Anchor your answer with a concrete example.",
  "Name one trade-off before you conclude.",
  "Keep it clear: context, action, and result.",
  "Use measurable impact where you can.",
  "Be specific about the outcome.",
  "Show ownership in how you frame the result.",
];

let transitionIdx = Math.floor(Math.random() * TRANSITIONS.length);
let closerIdx = Math.floor(Math.random() * CLOSERS.length);

function buildInterviewerSpeech(params: {
  question: string;
  isFirstQuestion: boolean;
  candidateName: string;
}) {
  const q = normalizeSpeechText(params.question);
  const intro = params.isFirstQuestion
    ? params.candidateName
      ? `Hi ${params.candidateName}, welcome in. We will keep this conversational and practical.`
      : "Hi, welcome in. We will keep this conversational and practical."
    : TRANSITIONS[transitionIdx++ % TRANSITIONS.length];
  const closer = CLOSERS[closerIdx++ % CLOSERS.length];

  const plain = `${intro} ${q} ${closer}`.replace(/\s+/g, " ").trim();
  const ssml = `<speak><prosody rate="90%" pitch="+2%">${escapeXml(intro)}<break time="350ms"/>${escapeXml(q)}<break time="420ms"/>${escapeXml(closer)}</prosody></speak>`;
  return { plain, ssml };
}

// ─── Main Component ───────────────────────────────────────────────────────────

export function InterviewScreen() {
  const { theme } = useAppTheme();
  const isFocused = useIsFocused();
  const {
    profile,
    coding,
    interview,
    startInterview,
    submitInterviewAnswer,
    endInterviewEarly,
    resetInterview,
    isSubmittingInterviewAnswer,
    lastError,
    clearLastError,
  } = useAppState();

  // ── Permissions & camera ──
  const [cameraPermission, requestCameraPermission] = useCameraPermissions();
  const canUseCamera = cameraPermission?.granted === true;
  const [audioPermission, setAudioPermission] = useState(false);
  const [appStateStatus, setAppStateStatus] = useState<AppStateStatus>(AppState.currentState);

  // ── Recording ──
  const [recording, setRecording] = useState<Audio.Recording | null>(null);
  const [recordedSeconds, setRecordedSeconds] = useState(0);
  const [lastClipUri, setLastClipUri] = useState("");

  // ── UI state ──
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("");
  const [showSetupCard, setShowSetupCard] = useState(true);
  const [showQuitConfirm, setShowQuitConfirm] = useState(false);
  const [showHints, setShowHints] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  // ── Voice / TTS ──
  const [isSpeakingQuestion, setIsSpeakingQuestion] = useState(false);
  const [voiceLabel, setVoiceLabel] = useState("");

  // ── Evaluation results ──
  const [latestScore, setLatestScore] = useState<number | null>(null);
  const [latestFeedback, setLatestFeedback] = useState("");
  const [latestRubric, setLatestRubric] = useState<InterviewRubric | null>(null);
  const [latestStrengths, setLatestStrengths] = useState<string[]>([]);
  const [latestImprovements, setLatestImprovements] = useState<string[]>([]);
  const [isEvaluating, setIsEvaluating] = useState(false);

  // ── Eye tracking (real-time face validation) ──
  const [eyeScore, setEyeScore] = useState(0);
  const [eyeSamples, setEyeSamples] = useState(0);
  const [framingHint, setFramingHint] = useState("");
  const [cameraReady, setCameraReady] = useState(false);
  const [cameraMountError, setCameraMountError] = useState<string | null>(null);
  const [isIntegrityCompromised, setIsIntegrityCompromised] = useState(false);

  // ── Session timer ──
  const [sessionElapsedMs, setSessionElapsedMs] = useState(0);

  // ── Session config ──
  const [selectedDomain, setSelectedDomain] = useState<InterviewDomain>("SDE");
  const [selectedDifficulty, setSelectedDifficulty] = useState<InterviewDifficulty>("Medium");
  const [selectedQuestionCount, setSelectedQuestionCount] = useState(5);
  const [selectedFocusTopic, setSelectedFocusTopic] = useState<InterviewTopic | "Mixed">("Mixed");

  const cameraRef = useRef<CameraView | null>(null);
  const soundRef = useRef<Audio.Sound | null>(null);
  const cleanupRef = useRef<null | (() => Promise<void>)>(null);
  const hasGreetedRef = useRef(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const heartbeatTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const heartbeatBusyRef = useRef(false);
  const lowScoreSinceRef = useRef<number | null>(null);

  // ── Derived ──
  const currentQuestion = interview.questions[interview.currentIndex];
  const currentHints = currentQuestion?.hints ?? [];
  const targetDurationSec = targetDurationForDifficulty(selectedDifficulty);
  const isLiveInterview = interview.active && !interview.completed;
  const shouldShowSetup = showSetupCard || (!interview.active && !interview.completed && interview.answers.length === 0);
  const showCameraFeed = canUseCamera;

  const averageScore = useMemo(() => {
    if (interview.answers.length === 0) return 0;
    return Math.round(interview.answers.reduce((s, a) => s + a.score, 0) / interview.answers.length);
  }, [interview.answers]);

  const questionById = useMemo(() => {
    const m = new Map<string, { topic: InterviewTopic; prompt: string }>();
    for (const q of interview.questions) m.set(q.id, { topic: q.topic, prompt: q.prompt });
    return m;
  }, [interview.questions]);

  const eyeRingColor = eyeScore >= SCORE_GREEN
    ? "rgba(50,220,110,0.92)"
    : eyeScore >= SCORE_YELLOW
    ? "rgba(255,200,50,0.92)"
    : "rgba(220,60,60,0.92)";
  const eyeConfidenceLabel = eyeScore >= 90
    ? "excellent"
    : eyeScore >= 60
    ? "warning"
    : eyeScore < SCORE_YELLOW
    ? "poor framing"
    : "monitoring";

  // ─── Helpers ────────────────────────────────────────────────────────────────

  const clearRecordingTimer = () => {
    if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null; }
  };

  const clearHeartbeatTimer = () => {
    if (heartbeatTimerRef.current) { clearInterval(heartbeatTimerRef.current); heartbeatTimerRef.current = null; }
  };

  const resetAudioMode = async () => {
    try {
      await Audio.setAudioModeAsync({
        allowsRecordingIOS: false,
        playsInSilentModeIOS: true,
        shouldDuckAndroid: true,
        playThroughEarpieceAndroid: false,
      });
    } catch { /* best effort */ }
  };

  const releaseAudio = async () => {
    try { await Speech.stop(); } catch { /* ignore */ }
    try {
      if (soundRef.current) { await soundRef.current.unloadAsync(); soundRef.current = null; }
    } catch { /* ignore */ }
    try {
      if (cleanupRef.current) { await cleanupRef.current(); cleanupRef.current = null; }
    } catch { /* ignore */ }
  };

  // ─── Camera presence heartbeat validation (Expo Camera + AppState) ─────────

  const applyScoreDelta = useCallback((delta: number, hint: string) => {
    setEyeSamples((prev) => prev + 1);
    setEyeScore((prev) => clampNumber(prev + delta, 0, 100));
    setFramingHint(hint);
  }, []);

  const applyVisionScore = useCallback((score: number, hint: string) => {
    setEyeSamples((prev) => prev + 1);
    setEyeScore(clampNumber(Math.round(score), 0, 100));
    setFramingHint(hint);
  }, []);

  const runPresenceHeartbeat = useCallback(async () => {
    if (!isLiveInterview || !showCameraFeed || !cameraReady || cameraMountError) {
      return;
    }

    if (!isFocused || appStateStatus !== "active") {
      return;
    }

    if (!cameraRef.current || heartbeatBusyRef.current) {
      return;
    }

    heartbeatBusyRef.current = true;
    try {
      const snapshot = await cameraRef.current.takePictureAsync({
        quality: 0.15,
        skipProcessing: true,
        base64: true,
      });

      const frameBase64 = String(snapshot.base64 || "");
      if (!frameBase64) {
        applyScoreDelta(-HEARTBEAT_ERROR_DECAY, "Camera frame missing. Hold steady in good light.");
        return;
      }

      const vision = await validateVisionFrame(frameBase64);
      const fallbackHint = vision.faceDetected
        ? vision.eyeDetected
          ? "Face centered and eyes visible"
          : "Keep your face fully visible"
        : "No face detected";

      applyVisionScore(vision.score, String(vision.hint || fallbackHint));
    } catch (error) {
      const message = error instanceof Error ? error.message.toLowerCase() : "";
      const hint = message.includes("blocked") ? "Camera blocked" : "Camera feed lost";
      applyScoreDelta(-FAILURE_DELTA, hint);
    } finally {
      heartbeatBusyRef.current = false;
    }
  }, [appStateStatus, applyScoreDelta, applyVisionScore, cameraMountError, cameraReady, isFocused, isLiveInterview, showCameraFeed]);

  useEffect(() => {
    const sub = AppState.addEventListener("change", (nextState) => {
      setAppStateStatus(nextState);
      if (nextState !== "active" && isLiveInterview) {
        clearHeartbeatTimer();
        applyScoreDelta(-FAILURE_DELTA, "Stay on the interview screen");
      }
    });

    return () => {
      sub.remove();
    };
  }, [applyScoreDelta, isLiveInterview]);

  useEffect(() => {
    if (!isLiveInterview) {
      clearHeartbeatTimer();
      lowScoreSinceRef.current = null;
      setIsIntegrityCompromised(false);
      return;
    }

    if (!canUseCamera) {
      clearHeartbeatTimer();
      setEyeScore(0);
      setFramingHint("Camera permission is required.");
      return;
    }

    if (cameraMountError) {
      clearHeartbeatTimer();
      setEyeScore(0);
      setFramingHint("Camera unavailable");
      return;
    }

    if (!cameraReady) {
      clearHeartbeatTimer();
      setFramingHint("Waiting for camera preview...");
      return;
    }

    if (!isFocused || appStateStatus !== "active") {
      clearHeartbeatTimer();
      return;
    }

    void runPresenceHeartbeat();
    clearHeartbeatTimer();
    heartbeatTimerRef.current = setInterval(() => {
      void runPresenceHeartbeat();
    }, HEARTBEAT_INTERVAL_MS);

    return () => {
      clearHeartbeatTimer();
    };
  }, [appStateStatus, cameraMountError, cameraReady, canUseCamera, isFocused, isLiveInterview, runPresenceHeartbeat]);

  useEffect(() => {
    if (!isLiveInterview) {
      lowScoreSinceRef.current = null;
      setIsIntegrityCompromised(false);
      return;
    }

    const ticker = setInterval(() => {
      if (eyeScore < LOW_SCORE_THRESHOLD) {
        if (lowScoreSinceRef.current == null) {
          lowScoreSinceRef.current = Date.now();
          return;
        }

        if (Date.now() - lowScoreSinceRef.current >= LOW_SCORE_MAX_MS) {
          setIsIntegrityCompromised(true);
          setFramingHint((prev) =>
            prev === "Camera unavailable"
              ? prev
              : "Presence score too low. Re-center before submitting.",
          );
        }
      } else {
        lowScoreSinceRef.current = null;
        setIsIntegrityCompromised(false);
      }
    }, 1000);

    return () => {
      clearInterval(ticker);
    };
  }, [eyeScore, isLiveInterview]);

  // ─── Session elapsed timer ───────────────────────────────────────────────────

  useEffect(() => {
    if (!isLiveInterview || interview.startedAt <= 0) { setSessionElapsedMs(0); return; }
    const sync = () => setSessionElapsedMs(Math.max(0, Date.now() - interview.startedAt));
    sync();
    const t = setInterval(sync, 1000);
    return () => clearInterval(t);
  }, [interview.startedAt, isLiveInterview]);

  // ─── Pause when tab loses focus ──────────────────────────────────────────────

  useEffect(() => {
    if (isFocused) return;
    clearHeartbeatTimer();
    setIsSpeakingQuestion(false);
    void releaseAudio();
    if (isLiveInterview) {
      applyScoreDelta(-FAILURE_DELTA, "Stay on the interview screen");
    }
  }, [applyScoreDelta, isFocused, isLiveInterview]);

  // ─── Cleanup on unmount ──────────────────────────────────────────────────────

  useEffect(() => {
    return () => {
      clearRecordingTimer();
      clearHeartbeatTimer();
      void releaseAudio();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─── TTS ─────────────────────────────────────────────────────────────────────

  const speakAloud = async (ssml: string, plain: string) => {
    if (!ssml || !plain) return;
    try {
      setIsSpeakingQuestion(true);
      setVoiceLabel("Speaking…");
      await releaseAudio();
      await resetAudioMode();
      const asset = await synthesizePollySpeech(ssml);
      cleanupRef.current = asset.cleanup;
      const { sound } = await Audio.Sound.createAsync({ uri: asset.fileUri }, { shouldPlay: false, volume: 1.0 });
      soundRef.current = sound;
      await sound.playAsync();
      sound.setOnPlaybackStatusUpdate((s) => {
        if (!s.isLoaded) return;
        if (s.didJustFinish) {
          void releaseAudio();
          setIsSpeakingQuestion(false);
          setVoiceLabel("");
        }
      });
    } catch (err) {
      const msg = err instanceof Error ? err.message : "playback failed";
      try {
        await resetAudioMode();
        setVoiceLabel("Device voice");
        Speech.speak(plain, {
          language: "en-US", rate: 0.9, pitch: 1.02,
          onDone: () => { setIsSpeakingQuestion(false); setVoiceLabel(""); },
          onStopped: () => { setIsSpeakingQuestion(false); setVoiceLabel(""); },
          onError: () => { setIsSpeakingQuestion(false); setVoiceLabel(`Voice error: ${msg}`); },
        });
      } catch {
        setIsSpeakingQuestion(false);
        setVoiceLabel(`Voice unavailable`);
      }
    }
  };

  const readCurrentQuestion = () => {
    if (!currentQuestion) return;
    const isFirst = interview.currentIndex === 0 && interview.answers.length === 0;
    const name = profile.name.trim();
    const useName = name.length > 0 && name.toLowerCase() !== "your name";
    const greet = isFirst && !hasGreetedRef.current;
    if (greet) hasGreetedRef.current = true;
    const speech = buildInterviewerSpeech({
      question: currentQuestion.prompt,
      isFirstQuestion: greet,
      candidateName: useName ? name : "",
    });
    void speakAloud(speech.ssml, speech.plain);
  };

  // ─── Recording ───────────────────────────────────────────────────────────────

  const ensureAudioPermission = async () => {
    if (audioPermission) return true;
    const r = await Audio.requestPermissionsAsync();
    setAudioPermission(r.granted);
    return r.granted;
  };

  const discardRecording = async () => {
    if (!recording) return;
    clearRecordingTimer();
    try { await recording.stopAndUnloadAsync(); } catch { /* ignore */ }
    finally { await resetAudioMode(); setRecording(null); setRecordedSeconds(0); }
  };

  const startRecording = async () => {
    if (!interview.active || recording) return;
    try {
      setBusy(true);
      await releaseAudio();
      setIsSpeakingQuestion(false);
      if (!await ensureAudioPermission()) { setStatus("Microphone permission required."); return; }
      await Audio.setAudioModeAsync({ allowsRecordingIOS: true, playsInSilentModeIOS: true, shouldDuckAndroid: true, playThroughEarpieceAndroid: false });
      const rec = new Audio.Recording();
      await rec.prepareToRecordAsync(Audio.RecordingOptionsPresets.LOW_QUALITY);
      await rec.startAsync();
      setRecording(rec);
      setRecordedSeconds(0);
      setLastClipUri("");
      setStatus("Recording… speak clearly.");
      timerRef.current = setInterval(async () => {
        try {
          const st = await rec.getStatusAsync();
          if (st.isRecording) setRecordedSeconds((st.durationMillis ?? 0) / 1000);
        } catch { clearRecordingTimer(); }
      }, 300);
    } catch { setStatus("Could not start recording. Try again."); clearRecordingTimer(); setRecording(null); }
    finally { setBusy(false); }
  };

  const stopRecording = async () => {
    if (!recording) return;
    try {
      setBusy(true);
      clearRecordingTimer();
      try {
        const st = await recording.getStatusAsync();
        const s = Number((st.durationMillis ?? 0) / 1000);
        if (Number.isFinite(s) && s > 0) setRecordedSeconds(s);
      } catch { /* ignore */ }
      await recording.stopAndUnloadAsync();
      const uri = recording.getURI() ?? "";
      setLastClipUri(uri);
      setStatus(uri ? "Recording captured — submit when ready." : "No audio file was saved. Try again.");
    } catch { setStatus("Could not stop cleanly. Retry."); }
    finally { await resetAudioMode(); setRecording(null); setBusy(false); }
  };

  // ─── Evaluation ──────────────────────────────────────────────────────────────

  const evaluateAnswer = async () => {
    if (!interview.active || !currentQuestion || recording || !lastClipUri) return;
    try {
      setIsEvaluating(true);
      setStatus("Evaluating with AI…");
      let audioBase64 = "";
      if (!lastClipUri.startsWith("typed://")) {
        try {
          const f = new File(lastClipUri);
          if (f.exists) audioBase64 = await f.base64();
        } catch { /* best effort */ }
      }
      const result = await submitInterviewAnswer({
        audioUri: lastClipUri,
        durationSec: recordedSeconds,
        audioBase64: audioBase64 || undefined,
        transcript: "",
      });
      setLatestScore(result.score);
      setLatestFeedback(result.feedback);
      setLatestRubric(result.rubric ?? null);
      setLatestStrengths(result.strengths ?? []);
      setLatestImprovements(result.improvements ?? []);
      setRecordedSeconds(0);
      setLastClipUri("");
      if (result.completed) {
        setStatus("Session complete. Great work.");
      } else {
        // Fetch conversational reaction
        try {
          const name = profile.name.trim();
          const r = await fetchInterviewReaction({
            domain: interview.config.domain,
            topic: currentQuestion.topic,
            score: result.score,
            candidateName: name.length > 0 && name.toLowerCase() !== "your name" ? name : "",
          });
          const phrase = String(r.phrase || "").trim();
          setStatus(phrase || "Next question is ready.");
        } catch {
          setStatus("Next question is ready.");
        }
        setShowHints(false);
      }
    } catch { setStatus("Evaluation failed. Try again."); }
    finally { await resetAudioMode(); setIsEvaluating(false); }
  };

  // ─── Session ─────────────────────────────────────────────────────────────────

  const beginSession = () => {
    clearLastError();
    hasGreetedRef.current = false;
    setShowQuitConfirm(false);
    startInterview({
      domain: selectedDomain,
      difficulty: selectedDifficulty,
      questionCount: selectedQuestionCount,
      focusTopic: selectedFocusTopic,
    });
    setShowSetupCard(false);
    setStatus("Session started — record your answer below.");
    setVoiceLabel("");
    setLatestFeedback("");
    setLatestScore(null);
    setLatestRubric(null);
    setLatestStrengths([]);
    setLatestImprovements([]);
    setRecordedSeconds(0);
    setLastClipUri("");
    setShowHints(false);
    setShowHistory(false);
    setEyeScore(0);
    setEyeSamples(0);
    setFramingHint("Waiting for camera preview...");
    setCameraMountError(null);
    setCameraReady(false);
    lowScoreSinceRef.current = null;
    setIsIntegrityCompromised(false);
    setSessionElapsedMs(0);
  };

  const doReset = async () => {
    clearLastError();
    await discardRecording();
    await releaseAudio();
    resetInterview();
    setShowSetupCard(true);
    setStatus("");
    setLatestFeedback("");
    setLatestScore(null);
    setLatestRubric(null);
    setLatestStrengths([]);
    setLatestImprovements([]);
    setLastClipUri("");
    setEyeScore(0);
    setEyeSamples(0);
    setFramingHint("");
    setCameraMountError(null);
    setCameraReady(false);
    lowScoreSinceRef.current = null;
    setIsIntegrityCompromised(false);
  };

  const doEndEarly = async () => {
    setShowQuitConfirm(false);
    await discardRecording();
    await releaseAudio();
    endInterviewEarly();
    setShowSetupCard(false);
    setIsSpeakingQuestion(false);
    setStatus("Interview ended. Review your feedback below.");
    clearHeartbeatTimer();
    setCameraReady(false);
    lowScoreSinceRef.current = null;
    setIsIntegrityCompromised(false);
  };

  // ─── Render ───────────────────────────────────────────────────────────────────

  // LIVE interview — dedicated focused layout (not inside ScreenContainer)
  if (isLiveInterview) {
    return (
      <SafeAreaView style={[styles.liveSafeArea, { backgroundColor: theme.colors.bgTop }]} edges={["top", "left", "right"]}>
        <View style={styles.liveRoot}>
          {/* Header */}
          <View style={styles.liveHeaderWrap}>
            <InterviewHeader
              questionIndex={interview.currentIndex}
              totalQuestions={interview.questions.length}
              domain={interview.config.domain}
              difficulty={interview.config.difficulty}
              elapsedMs={sessionElapsedMs}
              disabled={busy || isSubmittingInterviewAnswer}
              onQuit={() => { if (!busy && !isSubmittingInterviewAnswer) setShowQuitConfirm(true); }}
            />
          </View>

          <ScrollView
            style={styles.liveScroll}
            contentContainerStyle={styles.liveScrollContent}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
          >
          {/* ── Camera block ── */}
          <View style={[styles.cameraBlock, { borderColor: theme.colors.border }]}>
            {showCameraFeed ? (
              <>
                <CameraView
                  ref={cameraRef}
                  style={StyleSheet.absoluteFill}
                  facing="front"
                  active={isFocused && isLiveInterview}
                  onCameraReady={() => {
                    setCameraReady(true);
                    setCameraMountError(null);
                  }}
                  onMountError={(event) => {
                    setCameraReady(false);
                    setCameraMountError(event.message || "Camera unavailable");
                    setEyeScore(0);
                    setFramingHint("Camera unavailable");
                  }}
                />

                {/* Oval guide ring — colour reflects eye-contact score */}
                <View style={styles.ovalGuideWrap} pointerEvents="none">
                  <View style={[styles.ovalGuide, { borderColor: eyeRingColor }]} />
                </View>

                {/* Eye-contact badge */}
                <View style={styles.eyeBadge}>
                  <View style={[styles.eyeDot, {
                    backgroundColor: eyeSamples === 0 ? "#888" : eyeScore >= 70 ? "#3ddc84" : eyeScore >= 40 ? "#ffc832" : "#e05252",
                  }]} />
                  <ThemedText style={styles.eyeBadgeText}>
                    {eyeSamples > 0 ? `${eyeScore}%` : "--"}
                  </ThemedText>
                  <ThemedText style={styles.eyeBadgeLevel}>{eyeConfidenceLabel}</ThemedText>
                </View>

                {/* Recording badge */}
                {!!recording && (
                  <View style={styles.recBadge}>
                    <View style={styles.recPulse} />
                    <ThemedText style={styles.recText}>{formatSeconds(recordedSeconds)}</ThemedText>
                  </View>
                )}

                {/* Framing hint overlay at bottom of camera */}
                {!!(cameraMountError || framingHint) && (
                  <View style={styles.framingHintBar}>
                    <ThemedText style={styles.framingHintText} numberOfLines={2}>
                      {cameraMountError || framingHint}
                    </ThemedText>
                  </View>
                )}
              </>
            ) : (
              <View style={styles.cameraFallback}>
                <ThemedText variant="body" muted style={{ textAlign: "center" }}>
                  Camera off — enable it for presence validation.
                </ThemedText>
                <PrimaryButton
                  label="Enable Camera"
                  style={{ marginTop: 12 }}
                  onPress={async () => {
                    await requestCameraPermission();
                    await ensureAudioPermission();
                  }}
                />
              </View>
            )}
          </View>

          {/* ── Question card ── */}
          <GlassCard style={styles.questionCard}>
            {/* Topic + status row */}
            <View style={styles.questionMeta}>
              <View style={[styles.topicBadge, { backgroundColor: theme.colors.accentSoft }]}>
                <ThemedText style={[styles.topicBadgeText, { color: theme.colors.accent }]}>
                  {currentQuestion?.topic ?? "Question"}
                </ThemedText>
              </View>
              {!!status && (
                <ThemedText variant="body" muted style={styles.questionStatus} numberOfLines={1}>
                  {status}
                </ThemedText>
              )}
            </View>

            {/* Question text */}
            <ThemedText variant="title" strong style={styles.questionText}>
              {currentQuestion?.prompt ?? "Preparing question…"}
            </ThemedText>

            {/* Read aloud controls */}
            <View style={styles.voiceRow}>
              <PrimaryButton
                label={isSpeakingQuestion ? "Speaking…" : "▶  Read Aloud"}
                secondary
                style={{ flex: 1 }}
                disabled={isSpeakingQuestion || busy || isSubmittingInterviewAnswer || !currentQuestion}
                onPress={readCurrentQuestion}
              />
              {isSpeakingQuestion && (
                <PrimaryButton
                  label="■  Stop"
                  style={{ minWidth: 80 }}
                  onPress={() => { void releaseAudio(); setIsSpeakingQuestion(false); setVoiceLabel(""); }}
                />
              )}
            </View>
            {!!voiceLabel && (
              <ThemedText variant="label" muted style={{ marginTop: 4 }}>{voiceLabel}</ThemedText>
            )}

            {/* Hints */}
            {currentHints.length > 0 && (
              <View style={{ marginTop: 8 }}>
                <Pressable onPress={() => setShowHints((p) => !p)} style={styles.hintToggle}>
                  <ThemedText style={[styles.hintToggleText, { color: theme.colors.accent }]}>
                    {showHints ? "▲ Hide hints" : "▼ Show hints"}
                  </ThemedText>
                </Pressable>
                {showHints && (
                  <View style={styles.hintList}>
                    {currentHints.map((h) => (
                      <ThemedText key={h} variant="body" muted style={styles.hintItem}>· {h}</ThemedText>
                    ))}
                  </View>
                )}
              </View>
            )}
          </GlassCard>

          {/* ── Response controls ── */}
          <GlassCard style={styles.responseCard}>
            {/* Big record button */}
            <RecordingButton
              isRecording={!!recording}
              disabled={busy || !interview.active || isSubmittingInterviewAnswer}
              startLabel="⏺  Record Response"
              stopLabel="⏹  Stop Recording"
              onPress={() => void (recording ? stopRecording() : startRecording())}
            />

            {/* Duration bar — only visible when relevant */}
            {(!!recording || recordedSeconds > 0) && (
              <View style={{ marginTop: 10 }}>
                <View style={styles.durationRow}>
                  <ThemedText variant="label" muted>Duration</ThemedText>
                  <ThemedText variant="label" strong>
                    {formatSeconds(recordedSeconds)} / {formatSeconds(targetDurationSec)}
                  </ThemedText>
                </View>
                <View style={[styles.progressTrack, { backgroundColor: theme.colors.cardAlt }]}>
                  <View
                    style={[
                      styles.progressFill,
                      {
                        width: `${Math.min(100, Math.round((recordedSeconds / targetDurationSec) * 100))}%`,
                        backgroundColor: recordedSeconds > targetDurationSec
                          ? theme.colors.danger ?? "#e05252"
                          : theme.colors.accent,
                      },
                    ]}
                  />
                </View>
              </View>
            )}

            {/* Submit */}
            <PrimaryButton
              label={
                isIntegrityCompromised
                  ? "Validation Too Low"
                  : isEvaluating || isSubmittingInterviewAnswer
                  ? "Evaluating…"
                  : "Submit & Evaluate"
              }
              style={{ marginTop: 14 }}
              disabled={
                busy ||
                !!recording ||
                !lastClipUri ||
                isEvaluating ||
                isSubmittingInterviewAnswer ||
                isIntegrityCompromised
              }
              onPress={evaluateAnswer}
            />

            {isIntegrityCompromised && (
              <ThemedText variant="body" style={{ marginTop: 8, color: theme.colors.danger }}>
                Presence score has stayed below 30 for too long. Re-center and remain visible to continue.
              </ThemedText>
            )}

            {(isEvaluating || isSubmittingInterviewAnswer) && (
              <View style={styles.evalRow}>
                <ActivityIndicator size="small" color={theme.colors.accent} />
                <ThemedText variant="body" muted style={{ marginLeft: 10 }}>
                  Analysing your answer, hang tight…
                </ThemedText>
              </View>
            )}
          </GlassCard>
          </ScrollView>

          {/* Quit modal */}
          <QuitModal
            visible={showQuitConfirm}
            answeredCount={interview.answers.length}
            totalCount={interview.questions.length}
            onKeepGoing={() => setShowQuitConfirm(false)}
            onEnd={() => void doEndEarly()}
            theme={theme}
          />
        </View>
      </SafeAreaView>
    );
  }

  // SETUP / RESULTS view
  return (
    <ScreenContainer title="AI Interview" subtitle="Adaptive practice with voice & eye tracking">
      {/* ── Setup card ── */}
      {shouldShowSetup && (
        <GlassCard>
          <ThemedText variant="title" strong>Session Setup</ThemedText>
          {coding.depth > 0 && (
            <ThemedText variant="body" muted style={{ marginTop: 4 }}>
              Coding depth {coding.depth}% — recommendations adapt to your level.
            </ThemedText>
          )}

          <ConfigSection label="Domain">
            {DOMAINS.map((d) => (
              <Chip key={d} label={d} selected={selectedDomain === d} onPress={() => setSelectedDomain(d)} theme={theme} />
            ))}
          </ConfigSection>

          <ConfigSection label="Difficulty">
            {DIFFICULTIES.map((d) => (
              <Chip key={d} label={d} selected={selectedDifficulty === d} onPress={() => setSelectedDifficulty(d)} theme={theme} />
            ))}
          </ConfigSection>

          <ConfigSection label="Focus">
            {FOCUS_TOPICS.map((t) => (
              <Chip key={t} label={t} selected={selectedFocusTopic === t} onPress={() => setSelectedFocusTopic(t)} theme={theme} />
            ))}
          </ConfigSection>

          <ConfigSection label="Questions">
            {QUESTION_COUNTS.map((n) => (
              <Chip key={n} label={`${n}`} selected={selectedQuestionCount === n} onPress={() => setSelectedQuestionCount(n)} theme={theme} />
            ))}
          </ConfigSection>

          <View style={{ flexDirection: "row", gap: 10, marginTop: 16 }}>
            <PrimaryButton
              label="Start Interview"
              style={{ flex: 1 }}
              disabled={busy || isSubmittingInterviewAnswer}
              onPress={beginSession}
            />
            {(interview.answers.length > 0 || interview.active || interview.completed) && (
              <PrimaryButton
                label="Reset"
                secondary
                style={{ flex: 1 }}
                disabled={busy || isSubmittingInterviewAnswer}
                onPress={() => void doReset()}
              />
            )}
          </View>

          {!!lastError && (
            <ThemedText variant="body" style={{ marginTop: 10, color: theme.colors.danger }}>
              {lastError}
            </ThemedText>
          )}
        </GlassCard>
      )}

      {/* ── Completed / ended summary ── */}
      {(interview.completed || (!interview.active && !shouldShowSetup)) && (
        <GlassCard>
          <ThemedText variant="label" muted>
            {interview.completed ? "Session Complete" : "Session Ended"}
          </ThemedText>
          <ThemedText variant="title" strong style={{ marginTop: 4 }}>
            {interview.completed ? `Final Score: ${averageScore}/100` : "Interview ended early"}
          </ThemedText>
          <ThemedText variant="body" muted style={{ marginTop: 4 }}>
            {interview.answers.length} answer{interview.answers.length === 1 ? "" : "s"} evaluated.
          </ThemedText>
          <View style={{ flexDirection: "row", gap: 10, marginTop: 12 }}>
            <PrimaryButton label="New Session" style={{ flex: 1 }} onPress={beginSession} />
            <PrimaryButton
              label="Change Setup"
              secondary
              style={{ flex: 1 }}
              onPress={() => setShowSetupCard(true)}
            />
          </View>
          {!!lastError && (
            <ThemedText variant="body" style={{ marginTop: 10, color: theme.colors.danger }}>
              {lastError}
            </ThemedText>
          )}
        </GlassCard>
      )}

      {/* ── Latest evaluation card ── */}
      {latestScore !== null && latestRubric && (
        <GlassCard>
          <View style={styles.scoreHeader}>
            <ThemedText variant="title" strong>Last Answer</ThemedText>
            <View style={[styles.scoreBadge, { backgroundColor: latestScore >= 75 ? "#1a4f2a" : latestScore >= 55 ? "#4a3a00" : "#4a1a1a" }]}>
              <ThemedText style={[styles.scoreBadgeText, { color: latestScore >= 75 ? "#3ddc84" : latestScore >= 55 ? "#ffc832" : "#e05252" }]}>
                {latestScore}/100
              </ThemedText>
            </View>
          </View>

          {!!latestFeedback && (
            <ThemedText variant="body" muted style={{ marginTop: 8 }}>{latestFeedback}</ThemedText>
          )}

          <View style={{ marginTop: 14, gap: 10 }}>
            <RubricBar label="Content" value={latestRubric.content} theme={theme} />
            <RubricBar label="Structure" value={latestRubric.structure} theme={theme} />
            <RubricBar label="Clarity" value={latestRubric.clarity} theme={theme} />
            <RubricBar label="Confidence" value={latestRubric.confidence} theme={theme} />
          </View>

          {latestStrengths.length > 0 && (
            <View style={{ marginTop: 12 }}>
              <ThemedText variant="label" muted>Strengths</ThemedText>
              {latestStrengths.map((s) => (
                <ThemedText key={s} variant="body" style={{ marginTop: 4, color: "#3ddc84" }}>✓ {s}</ThemedText>
              ))}
            </View>
          )}

          {latestImprovements.length > 0 && (
            <View style={{ marginTop: 12 }}>
              <ThemedText variant="label" muted>To improve</ThemedText>
              {latestImprovements.map((s) => (
                <ThemedText key={s} variant="body" muted style={{ marginTop: 4 }}>⚡ {s}</ThemedText>
              ))}
            </View>
          )}
        </GlassCard>
      )}

      {/* ── Answer history ── */}
      {interview.answers.length > 0 && (
        <GlassCard>
          <Pressable
            style={styles.historyHeader}
            onPress={() => setShowHistory((p) => !p)}
          >
            <ThemedText variant="title" strong>Answer History</ThemedText>
            <ThemedText style={{ color: theme.colors.accent }}>{showHistory ? "▲ hide" : "▼ show"}</ThemedText>
          </Pressable>

          {showHistory && (
            <View style={{ marginTop: 10, gap: 8 }}>
              {interview.answers.map((ans, i) => {
                const info = questionById.get(ans.questionId);
                return (
                  <View
                    key={`${ans.questionId}-${i}`}
                    style={[styles.historyItem, { borderColor: theme.colors.border, backgroundColor: theme.colors.cardAlt }]}
                  >
                    <View style={styles.historyItemHeader}>
                      <ThemedText variant="body" strong>Q{i + 1} · {info?.topic ?? "Domain"}</ThemedText>
                      <ThemedText variant="body" strong style={{ color: ans.score >= 75 ? "#3ddc84" : ans.score >= 55 ? "#ffc832" : "#e05252" }}>
                        {ans.score}/100
                      </ThemedText>
                    </View>
                    {!!ans.transcript && (
                      <ThemedText variant="body" muted style={{ marginTop: 4 }} numberOfLines={2}>{ans.transcript}</ThemedText>
                    )}
                  </View>
                );
              })}
            </View>
          )}
        </GlassCard>
      )}

      {/* Quit modal (accessible from setup after partial session too) */}
      <QuitModal
        visible={showQuitConfirm}
        answeredCount={interview.answers.length}
        totalCount={interview.questions.length}
        onKeepGoing={() => setShowQuitConfirm(false)}
        onEnd={() => void doEndEarly()}
        theme={theme}
      />
    </ScreenContainer>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function ConfigSection({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <View style={{ marginTop: 14 }}>
      <ThemedText variant="label" muted>{label}</ThemedText>
      <View style={styles.chipRow}>{children}</View>
    </View>
  );
}

function Chip({
  label, selected, onPress, theme,
}: {
  label: string;
  selected: boolean;
  onPress: () => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  theme: any;
}) {
  return (
    <Pressable
      onPress={onPress}
      style={[
        styles.chip,
        {
          borderColor: selected ? theme.colors.accent : theme.colors.border,
          backgroundColor: selected ? theme.colors.accentSoft : theme.colors.cardAlt,
        },
      ]}
    >
      <ThemedText
        variant="body"
        strong={selected}
        style={{ color: selected ? theme.colors.accent : theme.colors.text }}
      >
        {label}
      </ThemedText>
    </Pressable>
  );
}

function RubricBar({ label, value, theme }: { label: string; value: number; theme: any }) {
  return (
    <View>
      <View style={styles.rubricLabelRow}>
        <ThemedText variant="body" muted>{label}</ThemedText>
        <ThemedText variant="body" strong>{value}</ThemedText>
      </View>
      <View style={[styles.rubricTrack, { backgroundColor: theme.colors.cardAlt }]}>
        <View
          style={[
            styles.rubricFill,
            {
              width: `${clampNumber(value, 0, 100)}%`,
              backgroundColor: value >= 70 ? "#3ddc84" : value >= 45 ? theme.colors.accent : "#e05252",
            },
          ]}
        />
      </View>
    </View>
  );
}

function QuitModal({
  visible, answeredCount, totalCount, onKeepGoing, onEnd, theme,
}: {
  visible: boolean;
  answeredCount: number;
  totalCount: number;
  onKeepGoing: () => void;
  onEnd: () => void;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  theme: any;
}) {
  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={onKeepGoing}>
      <View style={styles.modalBackdrop}>
        <View style={[styles.modalCard, { backgroundColor: theme.colors.card, borderColor: theme.colors.border }]}>
          <ThemedText variant="title" strong>End this interview?</ThemedText>
          <ThemedText variant="body" muted style={{ marginTop: 8 }}>
            You have answered {answeredCount} of {totalCount} question{totalCount === 1 ? "" : "s"}.
            Your evaluated answers will be saved.
          </ThemedText>
          <View style={{ flexDirection: "row", gap: 10, marginTop: 16 }}>
            <PrimaryButton label="Keep Going" secondary style={{ flex: 1 }} onPress={onKeepGoing} />
            <PrimaryButton label="End Interview" style={{ flex: 1 }} onPress={onEnd} />
          </View>
        </View>
      </View>
    </Modal>
  );
}

// ─── Styles ───────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  // Live layout
  liveSafeArea: {
    flex: 1,
  },
  liveRoot: {
    flex: 1,
    width: "100%",
    overflow: "hidden",
  },
  liveHeaderWrap: {
    paddingHorizontal: 16,
    paddingTop: 6,
  },
  liveScroll: {
    flex: 1,
  },
  liveScrollContent: {
    paddingTop: 4,
    paddingHorizontal: 16,
    paddingBottom: 32,
    gap: 12,
  },

  // Camera
  cameraBlock: {
    height: 260,
    borderRadius: 20,
    borderWidth: 1,
    overflow: "hidden",
    backgroundColor: "#111",
  },
  ovalGuideWrap: {
    ...StyleSheet.absoluteFillObject,
    alignItems: "center",
    justifyContent: "center",
  },
  ovalGuide: {
    width: "58%",
    aspectRatio: 0.78,
    borderWidth: 2.5,
    borderRadius: 999,
  },
  eyeBadge: {
    position: "absolute",
    top: 10,
    left: 12,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(0,0,0,0.6)",
    borderRadius: 10,
    paddingHorizontal: 8,
    paddingVertical: 4,
    gap: 5,
  },
  eyeDot: {
    width: 8,
    height: 8,
    borderRadius: 99,
  },
  eyeBadgeText: {
    color: "#fff",
    fontSize: 12,
    fontWeight: "700",
  },
  eyeBadgeLevel: {
    color: "#d9d9d9",
    fontSize: 10,
    textTransform: "uppercase",
    letterSpacing: 0.4,
  },
  recBadge: {
    position: "absolute",
    top: 10,
    right: 12,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(210,30,30,0.85)",
    borderRadius: 10,
    paddingHorizontal: 8,
    paddingVertical: 4,
    gap: 5,
  },
  recPulse: {
    width: 7,
    height: 7,
    borderRadius: 99,
    backgroundColor: "#fff",
  },
  recText: {
    color: "#fff",
    fontSize: 12,
    fontWeight: "700",
  },
  framingHintBar: {
    position: "absolute",
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: "rgba(0,0,0,0.55)",
    paddingVertical: 5,
    paddingHorizontal: 12,
  },
  framingHintText: {
    color: "#ddd",
    fontSize: 11,
    textAlign: "center",
  },
  cameraFallback: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 20,
  },

  // Question card
  questionCard: {
    gap: 0,
  },
  questionMeta: {
    flexDirection: "row",
    alignItems: "center",
    flexWrap: "wrap",
    gap: 6,
    marginBottom: 8,
  },
  questionStatus: {
    marginLeft: 10,
    flexShrink: 1,
    minWidth: 0,
  },
  topicBadge: {
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  topicBadgeText: {
    fontSize: 11,
    fontWeight: "700",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  questionText: {
    lineHeight: 26,
    marginBottom: 12,
  },
  voiceRow: {
    flexDirection: "row",
    alignItems: "center",
    flexWrap: "wrap",
    gap: 10,
  },
  hintToggle: {
    paddingVertical: 4,
  },
  hintToggleText: {
    fontSize: 13,
    fontWeight: "600",
  },
  hintList: {
    marginTop: 6,
    gap: 4,
  },
  hintItem: {
    lineHeight: 20,
  },

  // Response card
  responseCard: {
    gap: 0,
  },
  durationRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 6,
  },
  progressTrack: {
    height: 6,
    borderRadius: 99,
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    borderRadius: 99,
  },
  evalRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 12,
  },

  // Setup / results
  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 8,
  },
  chip: {
    borderWidth: 1,
    borderRadius: 999,
    paddingHorizontal: 14,
    paddingVertical: 7,
  },

  // Evaluation card
  scoreHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  scoreBadge: {
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  scoreBadgeText: {
    fontSize: 14,
    fontWeight: "800",
  },
  rubricLabelRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 4,
  },
  rubricTrack: {
    height: 7,
    borderRadius: 99,
    overflow: "hidden",
  },
  rubricFill: {
    height: "100%",
    borderRadius: 99,
  },

  // History
  historyHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  historyItem: {
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
  },
  historyItemHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },

  // Modal
  modalBackdrop: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.6)",
    alignItems: "center",
    justifyContent: "center",
    padding: 24,
  },
  modalCard: {
    width: "100%",
    maxWidth: 420,
    borderWidth: 1,
    borderRadius: 18,
    padding: 20,
  },
});
