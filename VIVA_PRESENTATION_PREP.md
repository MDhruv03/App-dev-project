# OpportunityHub Presentation + Viva + Cross Question Prep

Use this as your final rehearsal sheet.

## 1. 60-Second Opening Pitch
Good morning everyone. Our project is OpportunityHub, a unified mobile-first platform that combines opportunity discovery, application tracking, and AI-assisted interview preparation in one flow.

Today, students usually use separate tools for finding jobs, tracking applications, and practicing interviews. This causes context switching, missed deadlines, and weak preparation continuity.

OpportunityHub solves that by giving one integrated pipeline: discover opportunities, personalize ranking by profile and skills, track status from saved to applied to interview, and practice interviews with scoring, rubric feedback, and readiness analytics.

Technically, we implemented a React Native frontend with a Node backend, persistent user state, AI-powered interview evaluation, voice-enabled live interview mode, and reliability features like fallback paths and sync-first architecture.

Our core contribution is integration with reliability: even when one dependency fails, the app continues with graceful fallback behavior.

## 2. 5-7 Minute Presentation Flow (What To Say)

### Minute 0-1: Problem Statement
- Students use multiple disconnected tools for jobs, tracking, and interview prep.
- This fragmentation reduces consistency and decision quality.
- Need: one continuous workflow.

### Minute 1-2: Proposed Solution
- OpportunityHub unifies:
- Opportunity discovery and personalized recommendations.
- Application tracking pipeline.
- AI interview practice with rubric scoring.
- Profile management and analytics.

### Minute 2-3: System Architecture (Simple Language)
- Frontend: React Native app (Expo).
- Backend: Node + Express API.
- Data: SQLite persistence in backend.
- AI: Groq API for interview evaluation.
- Voice: Polly-compatible flow with fallback behavior.
- Auth: Signup/login with bearer token session.

### Minute 3-4: Main Features
- Profile-aware opportunity ranking.
- Saved/applied/interview status updates.
- Interview session setup by domain/difficulty.
- Answer evaluation with rubric: content, structure, clarity, confidence.
- Readiness and activity analytics.

### Minute 4-5: Reliability + Engineering Depth
- Local-first resilience and fallback logic for external failures.
- Backend tunnel workflow for mobile testing.
- Camera presence validation in live interview mode.
- Recent quality hardening:
- Fixed runaway camera score inflation.
- Added backend PDF resume parsing endpoint.
- Stabilized ngrok tunnel workflow and API URL sync.

### Minute 5-6: Results and Impact
- End-to-end career workflow in one app.
- Reduced context switching for users.
- Better interview readiness through measurable feedback.
- Practical architecture for incremental production hardening.

### Minute 6-7: Conclusion
- OpportunityHub is not just listings or mock interviews.
- It is a connected career execution system.
- Next steps include stronger recommendation intelligence and richer recruiter-side insights.

## 3. Live Demo Script (If They Ask "Show It")
1. Login quickly with prepared credentials.
2. Open dashboard and show readiness summary.
3. Open opportunities and explain recommendation logic.
4. Save one opportunity and move status to applied/interview.
5. Open interview screen, show domain/difficulty setup.
6. Start session, show camera presence score and question read-aloud.
7. Submit one sample answer and show rubric feedback.
8. Open profile and show resume upload parse flow.
9. End by showing tracker or analytics screen.

## 4. Strong Technical Points To Emphasize
- Integration across discovery, tracking, and preparation is the main novelty.
- Clear service boundaries: UI state, service layer, backend API.
- Reliability engineering is intentional, not accidental.
- Failures are handled with user-safe fallbacks, not crashes.
- Architecture is extensible for production-scale upgrades.

## 5. Likely Viva Questions With Crisp Answers

### Concept and Motivation
Q1. Why did you choose this problem?
A: Students and freshers lose momentum because career workflow is fragmented. We targeted continuity from discovery to interview readiness.

Q2. What is the novelty in your work?
A: The novelty is integrated workflow plus resilience. Most tools solve only one part. We connect all parts with fallback-safe behavior.

Q3. Who are target users?
A: College students, final-year candidates, and early professionals applying to internships/jobs/hackathons.

### Architecture and Stack
Q4. Why React Native instead of pure native Android?
A: Faster iteration, single codebase, and enough performance for this product stage.

Q5. Why Node backend?
A: Lightweight API development, rapid iteration, and clear integration with auth, persistence, and AI services.

Q6. Why keep backend state instead of frontend-only storage?
A: Multi-screen consistency, sync integrity, centralized business logic, and cleaner scaling path.

Q7. Why SQLite in backend?
A: Simple embedded persistence, zero external infra for local deployment, and deterministic dev setup.

### Recommendation and Logic
Q8. How do recommendations work?
A: Weighted signals from profile fields such as skills and role preference are combined into a ranking score.

Q9. Is it ML or rule-based?
A: Current implementation is weighted heuristic ranking, chosen for explainability and predictable behavior.

Q10. Why not deep learning recommendations?
A: Need historical interaction data first. Heuristic model gives reliable baseline now and can later be replaced by learned ranking.

### Interview Module
Q11. How is interview scoring done?
A: AI evaluation returns score, rubric dimensions, strengths, and improvements through backend API.

Q12. What if AI API fails?
A: We surface controlled error/fallback behavior so flow does not break. Reliability was prioritized.

Q13. Why include rubric dimensions?
A: Single score is not actionable. Rubric dimensions tell the candidate exactly what to improve.

Q14. How do you ensure fairness in scoring?
A: Prompt and schema constraints are fixed, and outputs are normalized by backend. It is guidance-oriented, not final hiring judgment.

### Camera and Presence Validation
Q15. What was the issue in camera framing score?
A: Over-strict stale-frame checks caused score to stay low or inflate incorrectly in some conditions.

Q16. How did you fix it?
A: Softened stale-frame penalties, adjusted motion threshold, and rewarded confirmed camera presence before applying decay.

Q17. Why not use full face detection always?
A: Expo Go runtime constraints and device variability. Presence-based heartbeat approach is more portable in this setup.

### Resume Parsing
Q18. Why parse resume on backend instead of frontend?
A: PDF parsing on mobile runtime is unreliable. Backend parsing is more robust and easier to standardize.

Q19. Which formats are supported?
A: PDF and text-based files via uploaded bytes, then normalized extraction for profile suggestions.

Q20. How do you handle parse failures?
A: Return explicit API status and user-readable message, so UI can recover gracefully.

### Security and Reliability
Q21. How is authentication managed?
A: Signup/login returns bearer token, and protected endpoints enforce auth middleware.

Q22. How do you protect sensitive data?
A: Basic token-based access control and scoped user state. Production hardening plan includes stronger token lifecycle and secure secret management.

Q23. What happens if network is unstable?
A: We use timeout controls, fallback paths, and managed tunnel workflow for development reliability.

Q24. Biggest reliability issue you faced?
A: Intermittent tunnel failures and unstable camera scoring behavior. Both were fixed with explicit engineering controls.

### Testing and Quality
Q25. What testing did you do?
A: Type checks, endpoint verification, startup health checks, and manual end-to-end feature validation.

Q26. One bug that improved your design thinking?
A: Camera score runaway and pinned-low issues forced us to redesign from binary pass/fail to graduated scoring logic.

### Limitations and Future Work
Q27. Current limitations?
A: Heuristic recommendation baseline, limited recruiter-side workflows, and need for broader automated test coverage.

Q28. Next improvements?
A: Learned recommendation ranking, richer analytics, better anti-cheat interview presence checks, and production-grade observability.

Q29. How will you scale this?
A: Move SQLite to managed DB, introduce caching and queues, add stateless service instances, and formal monitoring.

Q30. If given 2 more months, what would you deliver?
A: Stronger recommendation engine, recruiter dashboard, and full CI-based integration testing.

## 6. Cross Question Defense Lines (Fast Responses)
- We prioritized integration and reliability over flashy features.
- We chose explainable heuristics first, then designed for future ML upgrades.
- The system is intentionally modular: UI, service layer, backend, AI integration.
- Our design objective was continuity of user workflow, not isolated feature demos.
- Every major failure mode has a graceful fallback or clear user message.

## 7. If They Ask About "Report vs Current Code"
Say this clearly:
- The core research objective remains the same: unified career workflow.
- Implementation evolved into a React Native + Node production-style stack for faster iteration and integration.
- Architectural principles from the report are preserved: layered design, persistence, sync/reliability focus, and interview intelligence.

## 8. Last 10-Minute Rehearsal Checklist (Tonight)
- Practice opening pitch 3 times with timer.
- Keep one clean demo path and one backup path.
- Keep test credentials and backend running before presentation.
- Prepare 3 technical highlights and 3 limitations honestly.
- End with impact + future scope, not just features.

## 9. Final Closing Line
OpportunityHub demonstrates how a practical, reliable, and integrated mobile system can improve career execution from opportunity discovery to interview readiness in one continuous workflow.
