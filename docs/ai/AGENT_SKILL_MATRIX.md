# MTMS Agent / Skill matrix

| Agent | Type | Skills assigned | Allowed now (G0 pending) |
|---|---|---|---|
| `mtms-tech-lead` | Primary | `mtms-task-orchestration`, `mtms-requirements-traceability` | Read-only planning & delegation for analysis |
| `mtms-business-analyst` | Subagent | `mtms-requirements-traceability` | BR/TC gap review |
| `mtms-architect` | Subagent | `mtms-architecture-review`, `mtms-api-contract` | Conceptual proposals, no migrations |
| `mtms-backend-engineer` | Subagent | `mtms-spring-boot-backend`, `mtms-postgresql-flyway`, `mtms-api-contract` | Read-only plans |
| `mtms-frontend-engineer` | Subagent | `mtms-react-frontend`, `mtms-api-contract` | FE screen map/plans only |
| `mtms-qa-reviewer` | Subagent | `mtms-test-review` | Test plan/criteria review only |

Rules are not roles. `AGENTS.md` applies continuously; `.agents/rules/*.md` use `trigger: always_on` for hard boundaries or `trigger: model_decision` for topical rules. A Skill is activated based on its description or named explicitly, and does not grant permission to bypass G0.

## Why no Taste/GSAP or MCP yet?
- The FE technology choice is proposed; business workflows and API contracts matter before animation skills. For operational finance UI, favor clarity over motion.
- Existing user-installed global skills may work, but this archive neither installs nor verifies them.
- Database, GitHub and browser MCP can later improve workflow, but would increase permissions before a real need exists.
