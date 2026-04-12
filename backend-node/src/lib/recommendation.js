function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value));
}

function normalizeSkills(skills) {
  if (!skills) return [];
  return String(skills)
    .split(",")
    .map((item) => item.trim().toLowerCase())
    .filter(Boolean);
}

function scoreBySkills(opportunity, wantedSkills) {
  if (!wantedSkills.length) {
    return opportunity.matchScore;
  }

  const opportunitySkills = Array.isArray(opportunity.skills)
    ? opportunity.skills.map((item) => String(item).toLowerCase())
    : [];

  let overlap = 0;
  for (const skill of wantedSkills) {
    if (opportunitySkills.some((candidate) => candidate.includes(skill) || skill.includes(candidate))) {
      overlap += 1;
    }
  }

  const bonus = Math.min(20, overlap * 5);
  return clamp(opportunity.matchScore + bonus, 0, 99);
}

export function personalizeAndRankOpportunities(opportunities, profileSkills) {
  const wantedSkills = normalizeSkills(profileSkills);

  const updated = opportunities.map((opportunity) => {
    const matchScore = scoreBySkills(opportunity, wantedSkills);
    const recommendedScore = Math.round(matchScore * 0.68 + (opportunity.popularity || 50) * 0.32);

    return {
      ...opportunity,
      matchScore,
      recommendedScore
    };
  });

  updated.sort((a, b) => {
    const byRecommended = (b.recommendedScore || 0) - (a.recommendedScore || 0);
    if (byRecommended !== 0) return byRecommended;
    return (a.deadlineEpoch || 0) - (b.deadlineEpoch || 0);
  });

  return updated.map(({ recommendedScore, ...opportunity }) => opportunity);
}
