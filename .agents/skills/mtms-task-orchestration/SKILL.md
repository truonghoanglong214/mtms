---
name: mtms-task-orchestration
description: "Coordinates a gated MTMS task from tracker lookup through dependency check, owner plan approval, agent handoff, evidence and status proposal."
---

# Task orchestration — gated SOP
1. Locate Task ID and dependency graph in the v1.1 Tracker; verify status and G0. Recheck changes against original handbook.
2. Identify BR, TC, ADR, FE-subtask and unresolved policy flags. Block on unresolved prerequisite/approval.
3. Before G0: return *planning only*. Do not call implementer to change code.
4. After recorded G0: write one bounded task brief using `docs/ai/templates/TASK_BRIEF_TEMPLATE.md`; request explicit owner approval.
5. Assign tasks to specialized agents with distinct file ownership; sequence backend contract before FE integration.
6. Aggregate actual build/tests/QA/evidence and propose Tracker update only after acceptance.
**Output:** task ID, readiness (BLOCKED/READY_TO_PLAN/WAITING_APPROVAL), dependencies, proposed owners, plan and evidence placeholders labeled NOT RUN.

