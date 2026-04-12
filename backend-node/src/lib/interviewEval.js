function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function tokenize(text) {
  return String(text || "")
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, " ")
    .split(/\s+/)
    .map((token) => token.trim())
    .filter(Boolean);
}

function computeDurationSignal(durationSec) {
  if (durationSec < 12) return 34;
  if (durationSec < 22) return 48;
  if (durationSec < 38) return 62;
  if (durationSec < 55) return 74;
  if (durationSec < 78) return 84;
  if (durationSec < 105) return 88;
  return 82;
}

function computeTranscriptSignals(transcript) {
  const text = String(transcript || "").trim();
  if (!text) {
    return {
      lengthScore: 10,
      structureScore: 10,
      evidenceScore: 8,
      relevanceScore: 10,
      confidenceScore: 12
    };
  }

  const tokens = tokenize(text);
  const normalized = text.toLowerCase();

  const lengthScore = clamp(Math.round(tokens.length * 1.2), 10, 28);

  const structureMarkers = ["first", "second", "then", "finally", "because", "therefore", "so that"];
  const structureHits = structureMarkers.filter((marker) => normalized.includes(marker)).length;
  const structureScore = clamp(12 + structureHits * 4, 10, 26);

  const evidenceMarkers = [
    "metric",
    "impact",
    "result",
    "%",
    "latency",
    "throughput",
    "error rate",
    "incident",
    "rollback",
    "trade-off"
  ];
  const evidenceHits = evidenceMarkers.filter((marker) => normalized.includes(marker)).length;
  const evidenceScore = clamp(8 + evidenceHits * 4, 8, 28);

  const relevanceMarkers = ["user", "system", "design", "scale", "testing", "monitor", "outcome", "ownership"];
  const relevanceHits = relevanceMarkers.filter((marker) => normalized.includes(marker)).length;
  const relevanceScore = clamp(10 + relevanceHits * 3, 10, 24);

  const hedgeWords = ["maybe", "probably", "i guess", "kind of", "sort of", "not sure"];
  const hedgeHits = hedgeWords.filter((word) => normalized.includes(word)).length;
  const confidenceWords = ["implemented", "designed", "measured", "debugged", "led", "improved"];
  const confidenceHits = confidenceWords.filter((word) => normalized.includes(word)).length;
  const confidenceScore = clamp(14 + confidenceHits * 3 - hedgeHits * 4, 8, 24);

  return {
    lengthScore,
    structureScore,
    evidenceScore,
    relevanceScore,
    confidenceScore
  };
}

function buildLocalEvaluation(input) {
  const durationSec = Number(input.durationSec || 0);
  const transcript = String(input.transcript || "");
  const transcriptProvided = Boolean(transcript.trim());
  const durationSignal = computeDurationSignal(durationSec);
  const signals = computeTranscriptSignals(transcript);

  const transcriptSignal =
    signals.lengthScore +
    signals.structureScore +
    signals.evidenceScore +
    signals.relevanceScore +
    signals.confidenceScore;

  const score = transcriptProvided
    ? clamp(Math.round(durationSignal * 0.36 + clamp(Math.round((transcriptSignal / 130) * 100), 0, 100) * 0.64), 35, 97)
    : clamp(Math.round(durationSignal * 0.75 + 17), 35, 82);

  const rubric = {
    content: clamp(Math.round(score * 0.45 + signals.evidenceScore + signals.relevanceScore * 0.35), 20, 99),
    structure: clamp(Math.round(score * 0.38 + signals.structureScore * 1.25), 20, 99),
    clarity: clamp(Math.round(score * 0.4 + signals.lengthScore * 0.85), 20, 99),
    confidence: clamp(
      Math.round(score * 0.38 + (transcriptProvided ? signals.confidenceScore : 12) * 1.2),
      20,
      99
    )
  };

  const strengths = [];
  if (score >= 82) strengths.push("Strong interview signal with clear ownership and impact framing");
  if (rubric.structure >= 72) strengths.push("Good answer flow with a coherent beginning, middle, and end");
  if (transcript.trim().length > 120) strengths.push("Used specific technical vocabulary rather than generic statements");
  if (durationSec >= 38 && durationSec <= 85) strengths.push("Response length was in a strong interview-ready range");
  if (strengths.length === 0) strengths.push("Maintained response continuity under interview pressure");

  const improvements = [];
  if (!transcriptProvided) improvements.push("Add a quick recap in 'What you said' after recording for higher scoring accuracy");
  if (rubric.content < 68) improvements.push("Name one measurable outcome to prove business or user impact");
  if (rubric.structure < 68) improvements.push("Use a clearer sequence: context, your action, trade-off, and result");
  if (durationSec < 28) improvements.push("Expand your answer to around 45-75 seconds to cover depth and reasoning");
  if (String(input.topic || "") === "Behavioral") improvements.push("Use STAR explicitly so your behavioral story stays structured");
  if (improvements.length === 0) improvements.push("Maintain this quality and add one stronger metric in your closing sentence");

  let feedback = "Current response quality is below interview bar. Slow down, structure your points, and anchor with evidence.";
  if (score >= 85) {
    feedback = "Excellent response quality. Keep the same structure and keep emphasizing measurable outcomes.";
  } else if (score >= 72) {
    feedback = "Good response. Tighten your trade-off explanation and finish with one concrete result metric.";
  } else if (score >= 60) {
    feedback = transcriptProvided
      ? "Fair response. Improve structure and add more evidence from your real project decisions."
      : "Fair voice response. Add a short recap so scoring can reflect your full content more accurately.";
  }

  return {
    score,
    feedback,
    rubric,
    strengths: strengths.slice(0, 3),
    improvements: improvements.slice(0, 4)
  };
}

function extractJsonObject(content) {
  const text = String(content || "").trim();
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start >= 0 && end > start) {
    return text.slice(start, end + 1);
  }
  return text;
}

async function evaluateWithGroq(input, apiKey, model) {
  const systemPrompt = [
    "You are a strict technical interview evaluator.",
    "Return only valid JSON with keys:",
    "score (0-100 integer),",
    "feedback (string),",
    "rubric { content, structure, clarity, confidence } each 0-100,",
    "strengths (array of <=3 short strings),",
    "improvements (array of <=4 short strings)."
  ].join(" ");

  const userPrompt = {
    domain: input.domain,
    difficulty: input.difficulty,
    topic: input.topic,
    prompt: input.prompt,
    durationSec: input.durationSec,
    transcript: String(input.transcript || "").trim(),
    audioUri: input.audioUri
  };

  const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model,
      temperature: 0.25,
      max_tokens: 350,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: JSON.stringify(userPrompt) }
      ]
    })
  });

  if (!response.ok) {
    throw new Error(`Groq HTTP ${response.status}`);
  }

  const payload = await response.json();
  const content = payload?.choices?.[0]?.message?.content;
  if (!content) {
    throw new Error("Groq response missing content");
  }

  const parsed = JSON.parse(extractJsonObject(content));
  return {
    score: clamp(Math.round(Number(parsed.score || 0)), 0, 100),
    feedback: String(parsed.feedback || "Interview evaluated."),
    rubric: {
      content: clamp(Math.round(Number(parsed?.rubric?.content || 0)), 0, 100),
      structure: clamp(Math.round(Number(parsed?.rubric?.structure || 0)), 0, 100),
      clarity: clamp(Math.round(Number(parsed?.rubric?.clarity || 0)), 0, 100),
      confidence: clamp(Math.round(Number(parsed?.rubric?.confidence || 0)), 0, 100)
    },
    strengths: Array.isArray(parsed.strengths)
      ? parsed.strengths.map((item) => String(item)).filter(Boolean).slice(0, 3)
      : [],
    improvements: Array.isArray(parsed.improvements)
      ? parsed.improvements.map((item) => String(item)).filter(Boolean).slice(0, 4)
      : []
  };
}

export async function evaluateInterviewAnswer(input, options) {
  const local = buildLocalEvaluation(input);

  if (!options?.groqApiKey) {
    return local;
  }

  try {
    const ai = await evaluateWithGroq(input, options.groqApiKey, options.groqModel || "llama-3.1-8b-instant");
    if (!ai.strengths.length) {
      ai.strengths = local.strengths;
    }
    if (!ai.improvements.length) {
      ai.improvements = local.improvements;
    }
    if (!ai.feedback) {
      ai.feedback = local.feedback;
    }
    return ai;
  } catch {
    return {
      ...local,
      feedback: `${local.feedback} (Local evaluator fallback used.)`
    };
  }
}
