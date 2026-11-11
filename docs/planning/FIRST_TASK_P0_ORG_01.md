# First coding slice after G0: P0-ORG-01

**Not yet authorized:** G0 `Not Started`; this document is a bounded implementation brief, not evidence of code.

## Source
- Parent `P0-ORG-01` — Project skeleton, conventions (Task Tracker v1.0).
- Requirements: `BR-ORG-01` (multiple registered entities).
- Source test: `TC-001` (negative branch permission) — full feature-specific test will be executable after authorization features, not necessarily during project scaffold. Do not claim TC-001 passed at scaffold phase.
- Proposed FE mapping: `FE-P0-01` (responsive shell/foundation) to be sized by FE lead.

## Sequence (after G0 signoff)
1. Set up mono-repo folders: `backend/`, `frontend/`, `infra/docker/`, `docs/`, `.github/workflows/` and protected `.env.example` (no actual secrets).
2. Backend: create bootable Java 21/Maven/Spring Boot modular-monolith skeleton; keep module package boundaries without fabricating domain logic.
3. Frontend: create React/TypeScript/Vite skeleton with routing, design tokens, accessibility baseline and mobile/desktop layout. No hardcoded business data or POS components.
4. Infrastructure: local compose for developer services as applicable; no schema migration yet (that's `P0-ORG-02`).
5. Quality: backend smoke test, frontend lint/typecheck/build, CI baseline and documented local run commands. `TC-001` should be marked **Not Run** until branch-security prerequisites exist.
6. QA: inspect diff for secrets, unrelated files, invented entities or conflation of Business Entity vs Branch. Attach runnable build logs and screenshots using fictional data.
7. Update only `P0-ORG-01` after acceptance evidence; next task `P0-ORG-02` requires successful completion and review.

## Acceptance
- [ ] Spring Boot service builds and starts in dev.
- [ ] React dev server starts; typecheck/lint/build pass.
- [ ] Repo docs show exact reproduction steps and environment variables.
- [ ] All code uses well-defined Java/TS module naming and avoids hardcoded business assumptions.
- [ ] Secrets/private records excluded from git, fictional examples only.
- [ ] CI smoke checks run or explicitly report unavailable integrations.
- [ ] Evidence available in Tracker Task row; **do not mark Done before actual checks**.
