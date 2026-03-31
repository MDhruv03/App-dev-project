package com.example.myapplication.ui.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.network.GroqApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Fully automatic AI Interview — human-like conversation loop.
 *
 * Flow per turn:
 *   AI speaks question  →  mic auto-opens  →  user answers
 *   →  700ms "thinking" pause  →  Groq evaluates  →  300ms delay
 *   →  AI speaks short natural reply + next question
 *   →  mic auto-opens  →  repeat
 *
 * Interrupt: tap anywhere on the conversation area while AI speaks
 * to cut it off and start answering immediately.
 */
public class LiveInterviewActivity extends AppCompatActivity {

    // ──────────────────────────────────────────────────────────────────
    //  VARIATION POOLS  (keeps the interviewer from sounding repetitive)
    // ──────────────────────────────────────────────────────────────────
    private static final String[] THINK_FILLER = {
        "Hmm, ", "Alright, ", "Okay so, ", "Right, ", "Got it — ",
        "Sure, ", "Interesting — ", "Yeah, ", "Mm okay — "
    };
    private static final String[] TRANSITION = {
        "building on that — ", "so let's keep going — ", "moving on — ",
        "here's my next one — ", "let's shift gears a bit — ",
        "along those lines — ", "with that in mind — "
    };
    private static final String[] ACKNOWLEDGE = {
        "yeah that makes sense", "nice, I like that", "interesting approach",
        "hmm okay", "that works", "solid point", "good instinct",
        "I like how you framed that", "alright fair enough"
    };

    private final Random rng = new Random();

    // ──────────────────────────────────────────────────────────────────
    //  UI REFERENCES
    // ──────────────────────────────────────────────────────────────────
    private PreviewView previewView;
    private TextView tvLiveDomain, tvInterviewer, tvInterviewerAvatar;
    private TextView tvInterviewerStatus, tvInterviewerFace;
    private TextView tvLiveProgress, tvLiveQuestion;
    private TextView tvTranscript, tvSpeakingPace;
    private TextView tvLiveFeedback, tvLiveMetrics;
    private MaterialButton btnSpeakQuestion, btnStartListening;
    private MaterialButton btnNextLiveQuestion, btnFinishLiveInterview;
    private MaterialCardView cardInterviewerAvatar;
    private MaterialCardView cardMicStatus, cardFeedback;

    // ──────────────────────────────────────────────────────────────────
    //  STATE
    // ──────────────────────────────────────────────────────────────────
    private String domain = "SDE";
    private String interviewerName = "Alex";
    private boolean hasIntroduced = false;
    private boolean interviewFinished = false;
    private boolean isEvaluating = false;       // true while Groq call is in-flight
    private boolean isTtsSpeaking = false;       // true while TTS is speaking

    private final List<InterviewTurn> turns = new ArrayList<>();
    private final List<EvalResult> results = new ArrayList<>();
    private int currentIndex = 0;

    private final JSONArray conversationHistory = new JSONArray();

    // ──────────────────────────────────────────────────────────────────
    //  SPEECH
    // ──────────────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private long speechStartMs = 0L;

    // ──────────────────────────────────────────────────────────────────
    //  ANIMATION / TIMING
    // ──────────────────────────────────────────────────────────────────
    private ObjectAnimator pulseX, pulseY;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private String statusBase = "Getting ready";
    private int dotCount = 0;
    private boolean tickerRunning = false;
    private final Runnable tickerRunnable = new Runnable() {
        @Override public void run() {
            if (!tickerRunning) return;
            dotCount = (dotCount + 1) % 4;
            StringBuilder sb = new StringBuilder(statusBase);
            for (int i = 0; i < dotCount; i++) sb.append('.');
            if (tvInterviewerStatus != null) tvInterviewerStatus.setText(sb);
            uiHandler.postDelayed(this, 450);
        }
    };

    // ──────────────────────────────────────────────────────────────────
    //  PERMISSIONS
    // ──────────────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String[]> permLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            if (Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA))) startCamera();
        });

    // ══════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_interview);

        domain = getIntent().getStringExtra("DOMAIN");
        if (domain == null || domain.trim().isEmpty()) domain = "SDE";

        bindViews();
        buildQuestions();
        initSpeechRecognizer();
        initTts();          // kicks off interview once ready
        wireInterrupt();
        requestPerms();
    }

    @Override
    protected void onDestroy() {
        tickerRunning = false;
        uiHandler.removeCallbacksAndMessages(null);
        if (pulseX != null) pulseX.cancel();
        if (pulseY != null) pulseY.cancel();
        if (speechRecognizer != null) { speechRecognizer.cancel(); speechRecognizer.destroy(); }
        if (tts != null) { tts.stop(); tts.shutdown(); }
        super.onDestroy();
    }

    // ══════════════════════════════════════════════════════════════════
    //  SETUP
    // ══════════════════════════════════════════════════════════════════

    private void bindViews() {
        previewView            = findViewById(R.id.previewView);
        tvLiveDomain           = findViewById(R.id.tvLiveDomain);
        tvInterviewer          = findViewById(R.id.tvInterviewer);
        tvInterviewerAvatar    = findViewById(R.id.tvInterviewerAvatar);
        tvInterviewerStatus    = findViewById(R.id.tvInterviewerStatus);
        tvInterviewerFace      = findViewById(R.id.tvInterviewerFace);
        tvLiveProgress         = findViewById(R.id.tvLiveProgress);
        tvLiveQuestion         = findViewById(R.id.tvLiveQuestion);
        tvTranscript           = findViewById(R.id.tvTranscript);
        tvSpeakingPace         = findViewById(R.id.tvSpeakingPace);
        tvLiveFeedback         = findViewById(R.id.tvLiveFeedback);
        tvLiveMetrics          = findViewById(R.id.tvLiveMetrics);
        btnSpeakQuestion       = findViewById(R.id.btnSpeakQuestion);
        btnStartListening      = findViewById(R.id.btnStartListening);
        btnNextLiveQuestion    = findViewById(R.id.btnNextLiveQuestion);
        btnFinishLiveInterview = findViewById(R.id.btnFinishLiveInterview);
        cardInterviewerAvatar  = findViewById(R.id.cardInterviewerAvatar);
        cardMicStatus          = findViewById(R.id.cardMicStatus);
        cardFeedback           = findViewById(R.id.cardFeedback);

        pulseX = ObjectAnimator.ofFloat(cardInterviewerAvatar, "scaleX", 1f, 1.12f, 1f);
        pulseX.setDuration(650); pulseX.setRepeatCount(ObjectAnimator.INFINITE);
        pulseY = ObjectAnimator.ofFloat(cardInterviewerAvatar, "scaleY", 1f, 1.12f, 1f);
        pulseY.setDuration(650); pulseY.setRepeatCount(ObjectAnimator.INFINITE);
    }

    /** Tap anywhere on the feedback/transcript area to interrupt the AI mid-speech. */
    private void wireInterrupt() {
        View.OnClickListener interruptListener = v -> {
            if (isTtsSpeaking && !isEvaluating && !interviewFinished) {
                stopTts();
                uiHandler.postDelayed(this::startListeningAuto, 300);
            }
        };
        if (cardFeedback   != null) cardFeedback.setOnClickListener(interruptListener);
        if (tvLiveQuestion != null) tvLiveQuestion.setOnClickListener(interruptListener);
        if (tvTranscript   != null) tvTranscript.setOnClickListener(interruptListener);

        // Skip button
        if (btnNextLiveQuestion != null) {
            btnNextLiveQuestion.setOnClickListener(v -> {
                if (isEvaluating) return;
                stopTts();
                if (currentIndex < turns.size()) {
                    cardFeedback.setVisibility(View.GONE);
                    btnNextLiveQuestion.setVisibility(View.GONE);
                    presentQuestion(currentIndex);
                } else {
                    showSummary();
                }
            });
        }
        if (btnFinishLiveInterview != null) {
            btnFinishLiveInterview.setOnClickListener(v -> showSummary());
        }
    }

    private void buildQuestions() {
        interviewerName = pickName(domain);
        tvInterviewer.setText(interviewerName + " · AI Interviewer");
        tvInterviewerAvatar.setText(interviewerName.substring(0, 1).toUpperCase(Locale.US));
        tvLiveDomain.setText("Domain: " + domain);
        turns.addAll(buildTurns(domain));

        // ── SYSTEM PROMPT ──────────────────────────────────────────────
        // This is the single most important thing for naturalness.
        // Short, casual, no structure, flows like real speech.
        // ──────────────────────────────────────────────────────────────
        try {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content",
                "You are " + interviewerName + ", a friendly and experienced tech interviewer. "
                + "You're having a real conversation — not running a formal interview.\n\n"
                + "HOW YOU SPEAK:\n"
                + "- Max 2 sentences total. Never more.\n"
                + "- Sound like a real person: casual, warm, direct.\n"
                + "- Start with a short natural reaction: 'yeah that makes sense', 'nice', 'hmm okay', 'interesting', 'alright', etc.\n"
                + "- Then immediately flow into the next question WITHOUT labelling it.\n"
                + "- Never say 'Great answer', 'Good point', 'That's correct' — too robotic.\n"
                + "- Never say 'Next question', 'Moving on', 'Feedback' — just talk.\n"
                + "- No bullet points. No colons. No markdown. Plain spoken English only.\n"
                + "- Vary your sentence starters every time.\n\n"
                + "EXAMPLE of what you should sound like:\n"
                + "\"Yeah, the indexing part is spot on — and the cache layer is a smart call. "
                + "So how would you handle it if two requests hit the same short URL at the same moment?\"\n\n"
                + "EXAMPLE of what NOT to sound like:\n"
                + "\"Great answer! You correctly identified the key concepts. "
                + "Feedback: You could have mentioned sharding. Next question: How do you handle concurrency?\"\n\n"
                + "You're having a conversation. Keep it flowing."
            );
            conversationHistory.put(sys);
        } catch (JSONException ignored) {}
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                showToast("TTS init failed");
                return;
            }
            // Try Google Neural TTS for a much more natural voice
            String googleEngine = "com.google.android.tts";
            boolean hasGoogle = tts.getEngines().stream()
                .anyMatch(e -> googleEngine.equals(e.name));
            if (hasGoogle) {
                tts = new TextToSpeech(this, s2 -> {
                    if (s2 == TextToSpeech.SUCCESS) configureTts();
                }, googleEngine);
            } else {
                configureTts();
            }
        });
    }

    private void configureTts() {
        tts.setLanguage(Locale.US);
        tts.setSpeechRate(0.90f);  // Slightly slower — 0.9 sounds more thoughtful than 1.0
        tts.setPitch(0.97f);       // Very slightly lower — warmer, less robotic
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {
                runOnUiThread(() -> {
                    isTtsSpeaking = true;
                    setAiState(interviewerName + " is speaking", "🗣", true);
                });
            }
            @Override public void onDone(String id) {
                runOnUiThread(() -> {
                    isTtsSpeaking = false;
                    stopPulse();
                    if (!interviewFinished && !isEvaluating) {
                        // Small gap before mic opens — feels natural
                        uiHandler.postDelayed(LiveInterviewActivity.this::startListeningAuto, 500);
                    }
                });
            }
            @Override public void onError(String id) {
                runOnUiThread(() -> {
                    isTtsSpeaking = false;
                    setAiState("Audio issue", "⚠", false);
                });
            }
        });
        ttsReady = true;
        // Short delay before first question — lets the UI settle
        uiHandler.postDelayed(() -> presentQuestion(0), 1000);
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showToast("Speech recognition not available");
            return;
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                runOnUiThread(() -> {
                    cardMicStatus.setVisibility(View.VISIBLE);
                    setAiState("Listening...", "👂", false);
                });
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rms) {
                // If user starts making sound while AI is still speaking → interrupt
                if (isTtsSpeaking && rms > 3.0f && !isEvaluating) {
                    runOnUiThread(() -> {
                        stopTts();
                        // SpeechRecognizer is already listening at this point, just update UI
                        cardMicStatus.setVisibility(View.VISIBLE);
                        setAiState("Listening...", "👂", false);
                    });
                }
            }
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {
                runOnUiThread(() -> setAiState("Hmm, let me think about that...", "🤔", true));
            }
            @Override public void onError(int error) {
                runOnUiThread(() -> {
                    cardMicStatus.setVisibility(View.GONE);
                    // Silently retry after a beat — don't alarm the user
                    setAiState("Still listening...", "👂", false);
                    uiHandler.postDelayed(LiveInterviewActivity.this::startListeningAuto, 900);
                });
            }
            @Override public void onResults(Bundle bundle) {
                runOnUiThread(() -> {
                    cardMicStatus.setVisibility(View.GONE);
                    ArrayList<String> matches =
                        bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches == null || matches.isEmpty()) {
                        setAiState("Didn't catch that", "🤔", false);
                        uiHandler.postDelayed(LiveInterviewActivity.this::startListeningAuto, 900);
                        return;
                    }
                    String answer = matches.get(0).trim();
                    long dur = Math.max(1L, SystemClock.elapsedRealtime() - speechStartMs);
                    tvTranscript.setText(answer);
                    renderPace(answer, dur);

                    // Human-like pause before "thinking" state
                    uiHandler.postDelayed(() -> evaluateAndContinue(answer, dur), 700);
                });
            }
            @Override public void onPartialResults(Bundle partial) {
                ArrayList<String> p =
                    partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (p != null && !p.isEmpty()) {
                    runOnUiThread(() -> tvTranscript.setText(p.get(0) + "…"));
                }
            }
            @Override public void onEvent(int t, Bundle p) {}
        });

        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        // 2.5s silence = done speaking
        speechIntent.putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
        speechIntent.putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800);
    }

    // ══════════════════════════════════════════════════════════════════
    //  INTERVIEW FLOW
    // ══════════════════════════════════════════════════════════════════

    private void presentQuestion(int index) {
        if (index >= turns.size()) { showSummary(); return; }
        currentIndex = index;
        isEvaluating = false;

        InterviewTurn turn = turns.get(index);
        tvLiveProgress.setText("Q" + (index + 1) + " of " + turns.size());
        tvLiveQuestion.setText(turn.question);
        tvTranscript.setText("Your answer will appear here as you speak…");
        tvSpeakingPace.setText("");
        cardFeedback.setVisibility(View.GONE);
        btnNextLiveQuestion.setVisibility(View.GONE);
        btnFinishLiveInterview.setVisibility(View.GONE);

        String intro;
        if (!hasIntroduced) {
            hasIntroduced = true;
            // Warm, casual single-time intro then immediately into Q1
            intro = "Hey, I'm " + interviewerName + " — I'll be chatting with you today for the "
                + domain + " role. Alright, let's just dive in. " + turn.question;
            appendAssistant(intro);
        } else {
            // Should not normally hit here — Groq smooth-transitions inline
            intro = turn.question;
            appendAssistant(intro);
        }
        speakNow(intro, "q" + index);
    }

    private void startListeningAuto() {
        if (interviewFinished || isEvaluating || speechRecognizer == null) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
            return;
        }
        speechStartMs = SystemClock.elapsedRealtime();
        try {
            speechRecognizer.startListening(speechIntent);
        } catch (Exception ignored) {}
    }

    private void evaluateAndContinue(String answer, long durationMs) {
        isEvaluating = true;
        setAiState("Thinking...", "🧠", true);

        InterviewTurn turn = turns.get(currentIndex);
        appendUser(answer);

        boolean nonAnswer = isNonAnswer(answer);
        int wc = answer.trim().split("\\s+").length;
        double score = nonAnswer ? 8.0 : Math.min(88, Math.max(10, wc * 3.2));
        float wpm   = durationMs <= 0 ? 0 : (wc * 60000f) / durationMs;

        EvalResult er = new EvalResult();
        er.question = turn.question;
        er.answer   = answer;
        er.score    = score;
        er.wpm      = wpm;
        if (results.size() > currentIndex) results.set(currentIndex, er);
        else results.add(er);

        // Build the next-question instruction hidden from the user
        boolean isLast = (currentIndex >= turns.size() - 1);
        String nextInstruction;
        if (isLast) {
            nextInstruction = "This was the last question. Give a warm 1-sentence closing that "
                + "feels like the end of a real conversation, not formal. "
                + "Don't say 'The interview is now over'. Just end it naturally.";
        } else {
            String nextQ = turns.get(currentIndex + 1).question;
            nextInstruction = "After your reaction, naturally flow into asking: \""
                + nextQ + "\". Do NOT say 'next question'. Just ask it as part of the conversation. "
                + "Total response: 2 sentences max.";
        }

        if (nonAnswer) {
            nextInstruction = "They didn't know the answer. In 1 casual sentence, gently acknowledge "
                + "that's fine and give them a tiny hint about what they could have mentioned. "
                + "Then immediately ask the next question: \"" 
                + (isLast ? "(none — close warmly)" : turns.get(currentIndex + 1).question) + "\". "
                + "Keep it under 2 sentences.";
        }

        // Temporary instruction injected just for this turn (not stored in history)
        JSONArray tmp = cloneHistory();
        try {
            JSONObject inst = new JSONObject();
            inst.put("role", "user");
            inst.put("content",
                "[INSTRUCTION — not from candidate] Candidate replied to: '"
                + turn.topic + "'. " + nextInstruction);
            tmp.put(inst);
        } catch (JSONException e) {
            isEvaluating = false;
            return;
        }

        GroqApiClient.conductInterview(tmp, new GroqApiClient.InterviewCallback() {
            @Override
            public void onReply(String reply) {
                runOnUiThread(() -> {
                    er.feedback = reply;
                    appendAssistant(reply);

                    // Show feedback card briefly before AI speaks it
                    tvLiveFeedback.setText(reply);
                    cardFeedback.setVisibility(View.VISIBLE);

                    isEvaluating = false;
                    currentIndex++;
                    boolean last = currentIndex >= turns.size();

                    if (last) {
                        interviewFinished = true;
                        btnFinishLiveInterview.setVisibility(View.VISIBLE);
                        setAiState(interviewerName + " is wrapping up", "🗣", true);
                        // 300ms pause before speaking — feels reflective
                        uiHandler.postDelayed(() -> {
                            speakNow(reply, "done");
                            uiHandler.postDelayed(LiveInterviewActivity.this::showSummary,
                                estimateSpeechDurationMs(reply) + 1200);
                        }, 300);
                    } else {
                        // Update question card to next Q
                        InterviewTurn nextTurn = turns.get(currentIndex);
                        tvLiveQuestion.setText(nextTurn.question);
                        tvLiveProgress.setText("Q" + (currentIndex + 1) + " of " + turns.size());
                        tvTranscript.setText("Your answer will appear here as you speak…");
                        btnNextLiveQuestion.setVisibility(View.VISIBLE);

                        // 300ms pause — feels like the interviewer is composing their reply
                        uiHandler.postDelayed(() -> {
                            speakNow(reply, "q" + currentIndex);
                            setAiState(interviewerName + " is speaking", "🗣", true);
                        }, 300);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isEvaluating = false;
                    currentIndex++;
                    boolean last = currentIndex >= turns.size();
                    // Randomised casual fallback
                    String fb = pick(ACKNOWLEDGE) + ". " + (last
                        ? "That's it from me — thanks for the chat."
                        : turns.get(currentIndex).question);
                    tvLiveFeedback.setText(fb);
                    cardFeedback.setVisibility(View.VISIBLE);

                    if (last) {
                        interviewFinished = true;
                        speakNow(fb, "done");
                        uiHandler.postDelayed(LiveInterviewActivity.this::showSummary,
                            estimateSpeechDurationMs(fb) + 800);
                    } else {
                        tvLiveQuestion.setText(turns.get(currentIndex).question);
                        tvLiveProgress.setText("Q" + (currentIndex + 1) + " of " + turns.size());
                        tvTranscript.setText("Your answer will appear here as you speak…");
                        uiHandler.postDelayed(() -> speakNow(fb, "q" + currentIndex), 300);
                    }
                });
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════
    //  SPEECH HELPERS
    // ══════════════════════════════════════════════════════════════════

    private void speakNow(String text, String uid) {
        if (tts == null || !ttsReady) return;
        // Clean any markdown that might have slipped through
        String clean = text
            .replaceAll("[*#_`>\\[\\]]", "")
            .replaceAll("\\s{2,}", " ")
            .trim();
        tts.stop();
        tts.speak(clean, TextToSpeech.QUEUE_FLUSH, null, uid);
    }

    private void stopTts() {
        if (tts != null) tts.stop();
        isTtsSpeaking = false;
        stopPulse();
    }

    /** Rough estimate of how long TTS will take, used for auto-advancing after closing. */
    private long estimateSpeechDurationMs(String text) {
        int words = text == null ? 0 : text.trim().split("\\s+").length;
        // ~130 wpm at speech rate 0.9 → ~135 wpm effective
        return (long) (words / 135.0 * 60_000);
    }

    // ══════════════════════════════════════════════════════════════════
    //  CONVERSATION HISTORY
    // ══════════════════════════════════════════════════════════════════

    private void appendUser(String content) {
        try {
            JSONObject m = new JSONObject();
            m.put("role", "user"); m.put("content", content);
            conversationHistory.put(m);
        } catch (JSONException ignored) {}
    }

    private void appendAssistant(String content) {
        try {
            JSONObject m = new JSONObject();
            m.put("role", "assistant"); m.put("content", content);
            conversationHistory.put(m);
        } catch (JSONException ignored) {}
    }

    private JSONArray cloneHistory() {
        JSONArray c = new JSONArray();
        for (int i = 0; i < conversationHistory.length(); i++) {
            try { c.put(conversationHistory.get(i)); } catch (JSONException ignored) {}
        }
        return c;
    }

    // ══════════════════════════════════════════════════════════════════
    //  UI STATE MACHINE
    // ══════════════════════════════════════════════════════════════════

    private void setAiState(String status, String face, boolean animate) {
        statusBase = status;
        dotCount = 0;
        if (tvInterviewerStatus != null) tvInterviewerStatus.setText(status);
        if (tvInterviewerFace   != null) tvInterviewerFace.setText(face);
        if (animate) { startPulse(); startTicker(); }
        else         { stopTicker(); stopPulse(); }
    }

    private void startPulse() {
        if (pulseX != null && !pulseX.isStarted()) pulseX.start();
        if (pulseY != null && !pulseY.isStarted()) pulseY.start();
    }
    private void stopPulse() {
        if (pulseX != null) pulseX.cancel();
        if (pulseY != null) pulseY.cancel();
        if (cardInterviewerAvatar != null) {
            cardInterviewerAvatar.setScaleX(1f);
            cardInterviewerAvatar.setScaleY(1f);
        }
    }
    private void startTicker() {
        if (tickerRunning) return;
        tickerRunning = true;
        uiHandler.post(tickerRunnable);
    }
    private void stopTicker() {
        tickerRunning = false;
        uiHandler.removeCallbacks(tickerRunnable);
        if (tvInterviewerStatus != null) tvInterviewerStatus.setText(statusBase);
    }

    private void renderPace(String answer, long ms) {
        int wc = answer == null ? 0 : answer.trim().split("\\s+").length;
        float wpm = ms <= 0 ? 0 : (wc * 60000f) / ms;
        String band = wpm < 90 ? "a bit slow" : wpm > 175 ? "a bit fast" : "good pace";
        tvSpeakingPace.setText(String.format(Locale.US, "%.0f wpm · %s", wpm, band));
    }

    private boolean isNonAnswer(String a) {
        if (a == null || a.trim().length() < 3) return true;
        String n = a.toLowerCase(Locale.US).replaceAll("[^a-z ]", "").trim();
        for (String p : new String[]{
                "i dont know","i do not know","no idea","not sure",
                "idk","pass","skip","cant answer","i have no idea"})
            if (n.equals(p) || n.startsWith(p + " ")) return true;
        return n.split("\\s+").length <= 2;
    }

    private String pick(String[] arr) {
        return arr[rng.nextInt(arr.length)];
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  FINAL SUMMARY
    // ══════════════════════════════════════════════════════════════════

    private void showSummary() {
        interviewFinished = true;
        stopTicker(); stopPulse();
        if (tts != null) tts.stop();
        if (speechRecognizer != null) speechRecognizer.cancel();

        if (results.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                .setTitle("No answers captured")
                .setMessage("Answer at least one question to get your results.")
                .setPositiveButton("OK", null).show();
            return;
        }

        double avgScore = results.stream().mapToDouble(r -> r.score).average().orElse(0);
        double avgWpm   = results.stream().mapToDouble(r -> r.wpm).average().orElse(0);
        String signal   = avgScore >= 80 ? "Strong candidate 🟢"
                        : avgScore >= 60 ? "Promising — a bit more polish needed 🟡"
                        : "Keep practising 🔴";

        new MaterialAlertDialogBuilder(this)
            .setTitle("Interview Wrapped Up")
            .setMessage(
                String.format(Locale.US,
                    "Overall score: %.0f / 100\nSpeaking pace: %.0f wpm\n\n%s\n\n"
                    + "Tip: Keep answers to the point — 60–90 seconds per question is ideal.",
                    avgScore, avgWpm, signal))
            .setPositiveButton("Done", (d, w) -> finish())
            .setCancelable(false).show();
    }

    // ══════════════════════════════════════════════════════════════════
    //  CAMERA
    // ══════════════════════════════════════════════════════════════════

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> fut = ProcessCameraProvider.getInstance(this);
        fut.addListener(() -> {
            try {
                ProcessCameraProvider cp = fut.get();
                Preview prev = new Preview.Builder().build();
                prev.setSurfaceProvider(previewView.getSurfaceProvider());
                CameraSelector sel = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
                cp.unbindAll();
                cp.bindToLifecycle(this, sel, prev);
            } catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(this));
    }

    private void requestPerms() {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.CAMERA);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) missing.add(Manifest.permission.RECORD_AUDIO);
        if (!missing.isEmpty()) permLauncher.launch(missing.toArray(new String[0]));
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) startCamera();
    }

    // ══════════════════════════════════════════════════════════════════
    //  QUESTION BANK
    // ══════════════════════════════════════════════════════════════════

    private List<InterviewTurn> buildTurns(String domain) {
        List<InterviewTurn> list = new ArrayList<>();

        list.add(new InterviewTurn("Intro", "Background",
            "Tell me a bit about yourself — what you've been working on and what brought you to this role."));

        String d = domain == null ? "SDE" : domain.trim().toUpperCase(Locale.US);
        switch (d) {
            case "ANDROID":
                list.add(new InterviewTurn("Tech", "Architecture",
                    "How do you split responsibilities between a ViewModel and a Repository in MVVM? "
                    + "Where does the business logic actually live?"));
                list.add(new InterviewTurn("Tech", "Performance",
                    "What causes jank in a RecyclerView and how do you actually fix it?"));
                list.add(new InterviewTurn("Tech", "Concurrency",
                    "Walk me through how you'd run a database write off the main thread safely."));
                break;
            case "ML":
                list.add(new InterviewTurn("Tech", "Overfitting",
                    "Your model is 98% accurate in training but 67% in production — what's happening and what do you do?"));
                list.add(new InterviewTurn("Tech", "Feature Engineering",
                    "How do you decide which features to drop when you have 400 of them?"));
                list.add(new InterviewTurn("Tech", "Deployment",
                    "What's your approach for monitoring an ML model after it's live?"));
                break;
            case "WEB":
                list.add(new InterviewTurn("Tech", "Performance",
                    "Your page takes 6 seconds to load on mobile — walk me through how you'd debug and fix that."));
                list.add(new InterviewTurn("Tech", "State",
                    "When does local state stop being enough and you need something like Redux or Zustand?"));
                list.add(new InterviewTurn("Tech", "APIs",
                    "REST vs GraphQL — when would you actually pick one over the other in a real project?"));
                break;
            default: // SDE
                list.add(new InterviewTurn("Tech", "System Design",
                    "Let's say you're building a URL shortener at Twitter scale. "
                    + "Walk me through the architecture — just the key decisions."));
                list.add(new InterviewTurn("Tech", "Algorithms",
                    "Two-sum problem — what's the naive approach, and how would you get it down to O(n)?"));
                list.add(new InterviewTurn("Tech", "Concurrency",
                    "What's the difference between a thread and a coroutine, and when would you use each?"));
                break;
        }

        String[] behavioural = {
            "Tell me about a time you disagreed with a teammate on a technical call. What happened?",
            "Give me an example of a project that went sideways near the deadline. What did you actually do?",
            "Describe a time you had to learn something new very quickly to ship something."
        };
        list.add(new InterviewTurn("Behavioural", "Soft Skills",
            behavioural[rng.nextInt(behavioural.length)]));

        list.add(new InterviewTurn("Closing", "Reflection",
            "Honestly — how do you think that went? What would you answer differently?"));

        return list;
    }

    private String pickName(String domain) {
        if (domain == null) return "Alex";
        switch (domain.trim().toUpperCase(Locale.US)) {
            case "ML": return "Priya";
            case "WEB": return "Jordan";
            case "ANDROID": return "Sam";
            case "HR": return "Morgan";
            default: return "Alex";
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  DATA CLASSES
    // ══════════════════════════════════════════════════════════════════

    private static class InterviewTurn {
        final String stage, topic, question;
        InterviewTurn(String s, String t, String q) { stage=s; topic=t; question=q; }
    }

    private static class EvalResult {
        String question, answer, feedback;
        double score, wpm;
    }
}
