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
  return [
    {
      opportunityId: "opp-001",
      status: "Interview",
      savedAt: now - 8 * DAY,
      appliedAt: now - 6 * DAY,
      interviewScheduledAt: now + 2 * DAY,
      statusUpdatedAt: now - 1 * DAY,
      responseDate: 0,
      interviewNote: "Emphasize payments reliability and incident handling."
    },
    {
      opportunityId: "opp-002",
      status: "Applied",
      savedAt: now - 5 * DAY,
      appliedAt: now - 3 * DAY,
      interviewScheduledAt: 0,
      statusUpdatedAt: now - 3 * DAY,
      responseDate: 0,
      interviewNote: ""
    },
    {
      opportunityId: "opp-003",
      status: "Saved",
      savedAt: now - 2 * DAY,
      appliedAt: 0,
      interviewScheduledAt: 0,
      statusUpdatedAt: now - 2 * DAY,
      responseDate: 0,
      interviewNote: ""
    }
  ];
}

export function buildDefaultState(now = Date.now()) {
  return {
    version: 1,
    profile: {
      name: "Your Name",
      email: "you@email.com",
      skills: "Kotlin, React Native, SQL, System Design",
      roles: "Mobile Engineer, SDE",
      savedAt: 0
    },
    coding: {
      leetCodeHandle: "",
      codeforcesHandle: "",
      solved: 182,
      mediumHard: 64,
      rating: 1462,
      depth: 71,
      status: "Not synced yet",
      contests: [
        { title: "Codeforces Round #1002", time: "Sat 18:30", duration: "2h" },
        { title: "LeetCode Weekly 447", time: "Sun 08:00", duration: "1h 30m" },
        { title: "Biweekly Contest 157", time: "Sun 20:30", duration: "1h 30m" }
      ],
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
