function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function isStatus(record, status) {
  return String(record?.status || "").toLowerCase() === status.toLowerCase();
}

export function computeAnalytics(state) {
  const applications = Array.isArray(state.applications) ? state.applications : [];
  const answers = Array.isArray(state?.interview?.answers) ? state.interview.answers : [];

  const totalApplications = applications.filter((item) => !isStatus(item, "Saved")).length;
  const savedCount = applications.filter((item) => isStatus(item, "Saved")).length;
  const interviewCount = applications.filter((item) => isStatus(item, "Interview")).length;
  const offerCount = applications.filter((item) => isStatus(item, "Accepted")).length;
  const rejectedCount = applications.filter((item) => isStatus(item, "Rejected")).length;

  const decided = offerCount + rejectedCount;
  const successRate = decided > 0 ? Math.round((offerCount / decided) * 100) : 0;

  const interviewAttempts = answers.length;
  const avgInterviewScore =
    interviewAttempts > 0
      ? Math.round(answers.reduce((sum, answer) => sum + (Number(answer.score) || 0), 0) / interviewAttempts)
      : 0;

  return {
    totalApplications,
    savedCount,
    interviewCount,
    offerCount,
    successRate,
    avgInterviewScore,
    interviewAttempts
  };
}

export function computeReadiness(state, analytics) {
  const coding = state.coding || {};
  const codingDepth = Number(coding.depth) || 0;
  const codingRating = Number(coding.rating) || 0;

  const codingSignal = codingDepth * 0.44 + Math.min(48, codingRating / 28);
  const interviewSignal = analytics.avgInterviewScore > 0 ? analytics.avgInterviewScore : 55;
  const pipelineSignal =
    analytics.totalApplications > 0
      ? clamp(
          analytics.interviewCount * 14 +
            analytics.offerCount * 22 +
            Math.min(24, analytics.totalApplications * 4),
          0,
          100
        )
      : 28;

  return clamp(Math.round(codingSignal * 0.4 + interviewSignal * 0.35 + pipelineSignal * 0.25), 0, 100);
}

export function computeRoadmapTasks(state, analytics) {
  const coding = state.coding || {};
  const profile = state.profile || {};
  const interview = state.interview || {};

  const tasks = [];

  if ((Number(coding.mediumHard) || 0) < 65) {
    tasks.push("Problem depth sprint: complete 12 medium-hard questions this week.");
  } else {
    tasks.push("Contest execution: 2 contests + 1 upsolve deep-dive this week.");
  }

  if ((analytics.interviewAttempts || 0) < 3) {
    tasks.push("Run 3 full mock interview rounds with structured answer reviews.");
  } else {
    tasks.push("Refine weak rubric dimensions from latest interview results.");
  }

  if ((analytics.totalApplications || 0) < 6) {
    tasks.push("Expand pipeline: add 5 high-match opportunities this week.");
  } else {
    tasks.push("Pipeline hygiene: move stale applications to clear outcomes.");
  }

  if (!String(profile.skills || "").toLowerCase().includes("system design")) {
    tasks.push("Add system design project proof to profile and resume evidence.");
  } else {
    tasks.push("System design drill: one architecture walkthrough with trade-off narration.");
  }

  const latestAnswer = Array.isArray(interview.answers) && interview.answers.length > 0
    ? interview.answers[interview.answers.length - 1]
    : null;

  if (latestAnswer && Array.isArray(latestAnswer.improvements) && latestAnswer.improvements.length > 0) {
    tasks.push(`Interview weakness focus: ${latestAnswer.improvements[0]}`);
  }

  tasks.push("Weekly reflection: convert activity logs into 3 measurable improvements.");

  return tasks;
}
