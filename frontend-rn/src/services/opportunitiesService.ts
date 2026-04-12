import { USE_MOCK_SERVICES, hasApiBaseUrl } from "../config/env";
import { apiRequest } from "./apiClient";

export type OpportunityType = "internship" | "job" | "hackathon";

export type OpportunityRecord = {
  id: string;
  title: string;
  company: string;
  location: string;
  remote: boolean;
  paid: boolean;
  type: OpportunityType;
  experienceLevel: "fresher" | "junior" | "mid";
  salaryRange: string;
  skills: string[];
  deadlineEpoch: number;
  popularity: number;
  matchScore: number;
  description: string;
};

const DAY = 24 * 60 * 60 * 1000;

const fallbackOpportunities: OpportunityRecord[] = [
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
    deadlineEpoch: Date.now() + 6 * DAY,
    popularity: 84,
    matchScore: 88,
    description: "Build internal tooling and payment reliability workflows with strong mentorship.",
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
    deadlineEpoch: Date.now() + 11 * DAY,
    popularity: 79,
    matchScore: 91,
    description: "Own critical product flows and improve startup + runtime quality across mobile clients.",
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
    deadlineEpoch: Date.now() + 4 * DAY,
    popularity: 82,
    matchScore: 78,
    description: "Design APIs and event-driven backend systems for payment and settlement workflows.",
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
    deadlineEpoch: Date.now() + 9 * DAY,
    popularity: 75,
    matchScore: 73,
    description: "Build production-grade UI + edge workflow prototypes with real judges and mentorship.",
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
    deadlineEpoch: Date.now() + 14 * DAY,
    popularity: 88,
    matchScore: 67,
    description: "Work on model optimization and evaluation pipelines for edge deployment scenarios.",
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
    deadlineEpoch: Date.now() + 15 * DAY,
    popularity: 91,
    matchScore: 72,
    description: "Scale collaborative editing and rendering quality across millions of users.",
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
    deadlineEpoch: Date.now() + 7 * DAY,
    popularity: 86,
    matchScore: 83,
    description: "Three-stage challenge for full-time and internship opportunities.",
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
    deadlineEpoch: Date.now() + 12 * DAY,
    popularity: 74,
    matchScore: 89,
    description: "Own cold start, frame timing, and reliability improvements on consumer apps.",
  },
];

function normalizeSkills(skills: string): string[] {
  return skills
    .split(",")
    .map((skill) => skill.trim().toLowerCase())
    .filter(Boolean);
}

function personalizeMatch(
  opportunities: OpportunityRecord[],
  profileSkills: string
): OpportunityRecord[] {
  const wantedSkills = normalizeSkills(profileSkills);
  if (wantedSkills.length === 0) {
    return opportunities;
  }

  return opportunities.map((opportunity) => {
    const overlap = opportunity.skills.filter((skill) =>
      wantedSkills.some((wanted) => wanted === skill.toLowerCase())
    ).length;
    const bonus = Math.min(12, overlap * 4);

    return {
      ...opportunity,
      matchScore: Math.min(99, opportunity.matchScore + bonus),
    };
  });
}

export async function fetchOpportunityFeed(profileSkills: string): Promise<OpportunityRecord[]> {
  if (USE_MOCK_SERVICES || !hasApiBaseUrl()) {
    await new Promise((resolve) => setTimeout(resolve, 520));
    return personalizeMatch(fallbackOpportunities, profileSkills);
  }

  try {
    const payload = await apiRequest<OpportunityRecord[]>("/opportunities/feed", {
      method: "POST",
      body: JSON.stringify({ skills: profileSkills }),
    });
    return payload;
  } catch {
    return personalizeMatch(fallbackOpportunities, profileSkills);
  }
}
