

\# MTMS AI Session Rotation Policy



\## Goal

Use short-lived AI conversations without losing project progress.



\## Agent Identity

Each agent has a fixed responsibility.

Models may change depending on task complexity and risk.



\## Session Start

1\. Read AGENTS.md.

2\. Read the approved Task Contract.

3\. Read the relevant Task Checkpoint.

4\. Inspect Git branch, status and diff.

5\. Read only business and architecture sections needed.

6\. Verify unresolved blockers before work.



\## During Work

1\. Stay within task scope.

2\. Run appropriate tests.

3\. Update checkpoint after significant milestones.

4\. Never invent business rules.

5\. Never silently modify unrelated files.



\## Session End

1\. Record files changed.

2\. Record actual tests and their results.

3\. Record current Git commit/diff.

4\. Record outstanding issues.

5\. Record the exact next action.



\## Model Routing

LOW: formatting, summaries, mechanical checks.

MEDIUM: normal feature implementation.

HIGH: finance, payroll, WAC, security, architecture,

&#x20;     closing periods and investor distributions.



Escalate when uncertainty or risk increases.



\## Automation Safety

\- Maximum 3 review/repair rounds per task.

\- Stop on unresolved business decisions.

\- Stop on repeated identical failures.

\- Never auto-approve G0.

\- Never auto-merge to main.

\- Never auto-deploy production.

\- Never execute destructive migrations automatically.

\- Do not treat Herdr idle state alone as task completion.



