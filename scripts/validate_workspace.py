#!/usr/bin/env python3
"""Read-only MTMS workspace checks; Python standard library only."""
from pathlib import Path
import hashlib, re, sys, zipfile
ROOT=Path(__file__).resolve().parents[1]
SOURCE_HASHES={'docs/business/MTMS_Business_Spec_Roadmap_v1_0.docx': '3f41b333bc4856aecbad668c5b6935fdebfcdb0e9a30b39e55efabd94cc60d29', 'docs/business/MTMS_Implementation_Handbook_v1_0.md': '73100a4e84188d388abad2350c328cbd97b7e6f5aedaf1a792a497a38fdd4ffd', 'docs/architecture/MTMS_ERD_Architecture_Mermaid_v1_0.md': 'f2ed59e79e95e87f2a947c6cf19bbe94bf647b41d1e0bfd5bc478803a5b59984', 'docs/architecture/MTMS_PostgreSQL_Schema_Starter_v1_0.sql': '8edd992d8bfd53468e1783dda39c5aa726c4f916a6a86b005400ae5e914c0538', 'docs/reference/MTMS_Development_Tracker_v1_0.xlsx': '27046f28a25a6d6f05edfc44cc9ca08f3db29dc60c10af981db2d573e01ca0e3'}
EXPECTED_AGENTS={'mtms-frontend-engineer', 'mtms-qa-reviewer', 'mtms-architect', 'mtms-business-analyst', 'mtms-tech-lead', 'mtms-backend-engineer'}
EXPECTED_SKILLS={'mtms-api-contract', 'mtms-requirements-traceability', 'mtms-test-review', 'mtms-architecture-review', 'mtms-task-orchestration', 'mtms-react-frontend', 'mtms-postgresql-flyway', 'mtms-spring-boot-backend'}
errors=[]
def check(condition, message):
    print(('PASS' if condition else 'FAIL') + ': ' + message)
    if not condition: errors.append(message)

def frontmatter(path):
    content=path.read_text(encoding='utf-8')
    m=re.match(r'^---\s*\n(.*?)\n---\s*\n', content, re.S)
    check(m is not None, 'frontmatter '+str(path.relative_to(ROOT)))
    if not m: return {},content
    return dict(re.findall(r'^([a-zA-Z][a-zA-Z0-9]*):\s*(.*?)\s*$',m.group(1),re.M)),content

check((ROOT/'AGENTS.md').is_file(), 'root AGENTS.md')
check('G0 NOT SIGNED' in (ROOT/'AGENTS.md').read_text(encoding='utf-8'), 'G0 remains gated')
check('NO POS' in (ROOT/'AGENTS.md').read_text(encoding='utf-8'), 'NO POS safeguard')
agents=sorted((ROOT/'.agents/agents').glob('*.md'))
skills=sorted((ROOT/'.agents/skills').glob('*/SKILL.md'))
rules=sorted((ROOT/'.agents/rules').glob('*.md'))
check({p.stem for p in agents} == EXPECTED_AGENTS, 'six agents')
check({p.parent.name for p in skills} == EXPECTED_SKILLS, 'eight skills')
check(len(rules)==9,'nine rules')
for f in agents:
    meta, body=frontmatter(f)
    check(meta.get('name')==f.stem and bool(meta.get('description')), 'agent metadata '+f.name)
    refs=re.findall(r'^\s*- skills/([\w-]+)\s*$',body.split('---',2)[1],re.M) if body.startswith('---') else []
    check(bool(refs) and all(s in EXPECTED_SKILLS for s in refs),'agent skill refs '+f.name)
for f in skills:
    meta,_=frontmatter(f)
    check(meta.get('name')==f.parent.name and bool(meta.get('description')), 'skill metadata '+f.parent.name)
for f in rules:
    meta,_=frontmatter(f)
    check(meta.get('trigger') in {'always_on','model_decision','glob','manual'},'rule trigger '+f.name)
for rel,want in SOURCE_HASHES.items():
    p=ROOT/rel
    check(p.is_file() and hashlib.sha256(p.read_bytes()).hexdigest()==want,'source unchanged '+rel)
for rel in ['docs/planning/MTMS_Development_Tracker.xlsx','docs/planning/CURRENT_STATE.md','docs/ai/START_HERE.md','docs/ai/HOW_TO_ADD_FILES.md','docs/ai/prompts/01_READ_ONLY_SMOKE_TEST.md','backend/README.md','frontend/README.md','database/migrations/README.md']:
    check((ROOT/rel).is_file(),'required '+rel)
for p in ROOT.rglob('*.xlsx'):
    check(zipfile.is_zipfile(p) and zipfile.ZipFile(p).testzip() is None,'valid xlsx archive '+str(p.relative_to(ROOT)))
print('\nSUMMARY:', 'PASS' if not errors else f'{len(errors)} FAILURE(S)')
sys.exit(0 if not errors else 1)
