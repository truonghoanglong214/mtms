# Requirement–Task–Test traceability — v1.1 working copy

## Counts
- **70** BR requirements; directly mapped in the working copy **68**, pending **2**.
- **56** TC tests; directly mapped **56**, pending **0**.
- Links are planning references, **not proof** of implemented or passing tests.
- Cross-reference edits against existing implementation intent: `BR-HR-04` -> `P0-HR-05`; `BR-FIN-03` -> `P0-CLS-02`; `BR-CAP-06` -> `P1-CAP-05`; `TC-014` -> `P0-HR-05`; `TC-031` -> `P1-MOV-08`; `TC-037` -> `P1-CAP-07`.

## Pending gaps
- `BR-FIN-08` Opening cash/bank/stock/payable/receivable/assets/capital balances: source planning has several partial work items, but no single owned complete opening-balance reconciliation flow. **Owner must approve coverage**.
- `BR-CAP-10` Legal ownership/agreement caution: tracked as external release decision at G5; should not be treated as automatically code-implemented.

## Full matrix
| BR | Stage | Source status | Linked task(s) in v1.1 | Coverage status |
|---|---|---|---|---|
| BR-ORG-01 | P0 | BASELINED | P0-ORG-01; P0-ORG-02; P0-ORG-06 | Linked |
| BR-ORG-02 | P0 | BASELINED | P0-ORG-03 | Linked |
| BR-ORG-03 | P0 | BASELINED | P0-ORG-07 | Linked |
| BR-ORG-04 | P0 | BASELINED | P0-ORG-04; P0-ORG-05; P0-SEC-06 | Linked |
| BR-ORG-05 | P1 | BASELINED | P1-ORG-01; P1-CAP-10 | Linked |
| BR-ORG-06 | P1 | BASELINED | P1-ORG-02 | Linked |
| BR-REV-01 | P0 | BASELINED | P0-REV-02; P0-REV-04 | Linked |
| BR-REV-02 | P0 | BASELINED | P0-REV-01; P0-REV-06 | Linked |
| BR-REV-03 | P0 | BASELINED | P0-REV-03 | Linked |
| BR-REV-04 | P0 | BASELINED | P0-REV-05 | Linked |
| BR-REV-05 | P1 | BASELINED | P1-MOV-10 | Linked |
| BR-FIN-01 | P0 | BASELINED | P0-FIN-01; P0-FIN-08; P0-CLS-02 | Linked |
| BR-FIN-02 | P0 | BASELINED | P0-FIN-05 | Linked |
| BR-FIN-03 | P0 | BASELINED | P0-CLS-02 | Linked |
| BR-FIN-04 | P0 | BASELINED | P0-FIN-07 | Linked |
| BR-FIN-05 | P1 | BASELINED | P1-FIN-01 | Linked |
| BR-FIN-06 | P0 | BASELINED | P0-FIN-06; P1-FIN-03 | Linked |
| BR-FIN-07 | P0 | BASELINED | P0-FIN-03; P0-FIN-04 | Linked |
| BR-FIN-08 | P0 | BASELINED | — | Gap / policy |
| BR-FIN-09 | P0 | BASELINED | P0-FIN-02 | Linked |
| BR-HR-01 | P0 | BASELINED | P0-HR-02 | Linked |
| BR-HR-02 | P0 | BASELINED | P0-HR-03; P0-HR-04 | Linked |
| BR-HR-03 | P0 | BASELINED | P0-HR-01; P0-HR-09 | Linked |
| BR-HR-04 | P0 | BASELINED | P0-HR-05 | Linked |
| BR-HR-05 | P0 | BASELINED | P0-HR-06; P0-HR-08; P1-HR-07 | Linked |
| BR-HR-06 | P0 | BASELINED | P0-HR-07 | Linked |
| BR-HR-07 | P1 | BASELINED | P1-HR-01; P1-HR-02 | Linked |
| BR-HR-08 | P1 | BASELINED | P1-HR-03; P1-HR-04 | Linked |
| BR-HR-09 | P1 | VERIFY | P1-HR-05; P1-HR-06 | Linked |
| BR-HR-10 | P0 | BASELINED | P0-HR-05 | Linked |
| BR-INV-01 | P0 | BASELINED | P0-INV-06 | Linked |
| BR-INV-02 | P0 | BASELINED | P0-INV-04; P0-INV-07 | Linked |
| BR-INV-03 | P0 | BASELINED | P0-INV-05 | Linked |
| BR-INV-04 | P0 | BASELINED | P0-INV-01 | Linked |
| BR-INV-05 | P0 | BASELINED | P0-INV-03; P1-INV-05 | Linked |
| BR-INV-06 | P1 | BASELINED | P1-INV-01 | Linked |
| BR-INV-07 | P1 | BASELINED | P1-INV-02; P1-INV-03 | Linked |
| BR-INV-08 | P0 | BASELINED | P0-INV-02 | Linked |
| BR-INV-09 | P1 | PROPOSED | P1-INV-04 | Linked |
| BR-MOV-01 | P1 | BASELINED | P1-MOV-01 | Linked |
| BR-MOV-02 | P1 | BASELINED | P1-MOV-02; P1-MOV-03 | Linked |
| BR-MOV-03 | P1 | BASELINED | P1-MOV-04; P1-FIN-02 | Linked |
| BR-MOV-04 | P1 | BASELINED | P1-MOV-05; P1-MOV-06 | Linked |
| BR-MOV-05 | P1 | BASELINED | P1-MOV-08 | Linked |
| BR-MOV-06 | P1 | VERIFY | P1-MOV-07; P1-MOV-09 | Linked |
| BR-AST-01 | P0 | BASELINED | P0-AST-01; P1-AST-01 | Linked |
| BR-AST-02 | P1 | VERIFY | P1-AST-02 | Linked |
| BR-AST-03 | P1 | BASELINED | P1-AST-03 | Linked |
| BR-CAP-01 | P0 | BASELINED | P0-CAP-01; P0-CAP-02; P0-CAP-03 | Linked |
| BR-CAP-02 | P1 | BASELINED | P1-CAP-01 | Linked |
| BR-CAP-03 | P1 | BASELINED | P1-CAP-04 | Linked |
| BR-CAP-04 | P1 | BASELINED | P1-FIN-04; P1-CAP-02; P1-CAP-05; P1-CAP-06 | Linked |
| BR-CAP-05 | P1 | BASELINED | P1-CAP-07 | Linked |
| BR-CAP-06 | P1 | BASELINED | P1-CAP-05 | Linked |
| BR-CAP-07 | P1 | BASELINED | P1-CAP-08 | Linked |
| BR-CAP-08 | P1 | BASELINED | P1-CAP-09 | Linked |
| BR-CAP-09 | P1 | BASELINED | P1-CAP-03 | Linked |
| BR-CAP-10 | P1 | VERIFY | — | Gap / policy |
| BR-CLS-01 | P0 | BASELINED | P0-CLS-01; P0-CLS-04 | Linked |
| BR-CLS-02 | P0 | BASELINED | P0-CLS-03 | Linked |
| BR-CLS-03 | P0 | BASELINED | P1-CLS-01 | Linked |
| BR-CLS-04 | P1 | BASELINED | P1-CLS-02 | Linked |
| BR-SEC-01 | P0 | BASELINED | P0-SEC-01 | Linked |
| BR-SEC-02 | P0 | BASELINED | P0-SEC-04 | Linked |
| BR-SEC-03 | P0 | BASELINED | P0-SEC-02; P0-SEC-03 | Linked |
| BR-SEC-04 | P0 | BASELINED | P0-SEC-05; P0-CLS-05 | Linked |
| BR-SEC-05 | P1 | VERIFY | P1-CLS-03; P2-AI-08 | Linked |
| BR-AI-01 | P2 | BASELINED | P2-AI-01; P2-AI-02; P2-AI-03 | Linked |
| BR-AI-02 | P2 | BASELINED | P2-AI-04; P2-AI-05 | Linked |
| BR-AI-03 | P2 | BASELINED | P2-AI-06; P2-AI-07 | Linked |

## Test reference mapping
See `TC Mapping` worksheet, which preserves test descriptions and all mapped Task IDs.
