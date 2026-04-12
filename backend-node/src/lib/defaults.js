const DAY = 24 * 60 * 60 * 1000;

function buildDefaultOpportunities(now = Date.now()) {
  return [
    {
      id: "opp-001",
      title: "Software Engineer Intern",
      company: "Stripe",
      location: "Bengaluru",
      remote: true,
      paid: true,
      type: "internship",
      experienceLevel: "fresher",
      salaryRange: "INR 65k/mo",
      skills: ["Java", "Kotlin", "SQL", "System Design"],
      deadlineEpoch: now + 6 * DAY,
      popularity: 84,
      matchScore: 88,
      description: "Build internal tooling and payment reliability workflows with strong mentorship."
    },
    {
      id: "opp-002",
      title: "Android Engineer",
      company: "Notion",
      location: "Remote",
      remote: true,
      paid: true,
      type: "job",
      experienceLevel: "junior",
      salaryRange: "USD 115k - 130k",
      skills: ["Android", "Kotlin", "Compose", "Performance"],
      deadlineEpoch: now + 11 * DAY,
      popularity: 79,
      matchScore: 91,
      description: "Own critical product flows and improve startup + runtime quality across mobile clients."
    },
    {
      id: "opp-003",
      title: "SDE-1 Backend",
      company: "Razorpay",
      location: "Bengaluru",
      remote: false,
      paid: true,
      type: "job",
      experienceLevel: "junior",
      salaryRange: "INR 16-22 LPA",
      skills: ["Java", "Microservices", "Redis", "Kafka"],
      deadlineEpoch: now + 4 * DAY,
      popularity: 82,
      matchScore: 78,
      description: "Design APIs and event-driven backend systems for payment and settlement workflows."
    },
    {
      id: "opp-004",
      title: "Frontend Hackathon",
      company: "Vercel",
      location: "Remote",
      remote: true,
      paid: false,
      type: "hackathon",
      experienceLevel: "fresher",
      salaryRange: "Prize pool USD 20k",
      skills: ["React", "TypeScript", "Animation", "DX"],
      deadlineEpoch: now + 9 * DAY,
      popularity: 75,
      matchScore: 73,
      description: "Build production-grade UI + edge workflow prototypes with real judges and mentorship."
    },
    {
      id: "opp-005",
      title: "Machine Learning Intern",
      company: "NVIDIA",
      location: "Pune",
      remote: false,
      paid: true,
      type: "internship",
      experienceLevel: "fresher",
      salaryRange: "INR 70k/mo",
      skills: ["Python", "PyTorch", "MLOps", "Evaluation"],
      deadlineEpoch: now + 14 * DAY,
      popularity: 88,
      matchScore: 67,
      description: "Work on model optimization and evaluation pipelines for edge deployment scenarios."
    },
    {
      id: "opp-006",
      title: "Web Platform Engineer",
      company: "Figma",
      location: "San Francisco",
      remote: true,
      paid: true,
      type: "job",
      experienceLevel: "mid",
      salaryRange: "USD 145k - 180k",
      skills: ["TypeScript", "Rendering", "Accessibility", "Perf"],
      deadlineEpoch: now + 15 * DAY,
      popularity: 91,
      matchScore: 72,
      description: "Scale collaborative editing and rendering quality across millions of users."
    },
    {
      id: "opp-007",
      title: "Campus Hiring Challenge",
      company: "Atlassian",
      location: "Remote",
      remote: true,
      paid: true,
      type: "hackathon",
      experienceLevel: "fresher",
      salaryRange: "Pre-placement offer",
      skills: ["DSA", "System Design", "Communication"],
      deadlineEpoch: now + 7 * DAY,
      popularity: 86,
      matchScore: 83,
      description: "Three-stage challenge for full-time and internship opportunities."
    },
    {
      id: "opp-008",
      title: "Mobile Performance Engineer",
      company: "Swiggy",
      location: "Bengaluru",
      remote: false,
      paid: true,
      type: "job",
      experienceLevel: "junior",
      salaryRange: "INR 20-28 LPA",
      skills: ["Android", "React Native", "Profiling", "CI/CD"],
      deadlineEpoch: now + 12 * DAY,
      popularity: 74,
      matchScore: 89,
      description: "Own cold start, frame timing, and reliability improvements on consumer apps."
    }
  ];
}

function buildInitialApplications(now = Date.now()) {
  return [];
}

export function buildDefaultState(now = Date.now()) {
  return {
    version: 2,
    profile: {
      name: "",
      email: "",
      skills: "",
      roles: "",
      savedAt: 0
    },
    coding: {
      leetCodeHandle: "",
      codeforcesHandle: "",
      solved: 0,
      mediumHard: 0,
      rating: 0,
      depth: 0,
      status: "Connect your coding handles to sync real progress.",
      contests: [],
      lastSyncedAt: 0
    },
    opportunities: buildDefaultOpportunities(now),
    applications: buildInitialApplications(now),
    interview: {
      active: false,
      completed: false,
      config: {
        domain: "SDE",
        difficulty: "Medium",
        questionCount: 4,
        focusTopic: "Mixed"
      },
      questions: [],
      answers: [],
      currentIndex: 0,
      startedAt: 0,
      endedAt: 0
    },
    activityLog: []
  };
}
