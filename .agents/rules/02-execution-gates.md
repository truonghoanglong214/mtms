---
trigger: always_on
description: "Block coding until explicit signoff, task-level approvals and role boundaries"
---

# Execution gate / governance & Role Boundaries (Cấm vượt quyền)

- G0 remains **Not Started**. Until explicit recorded approval: planning, codebase inspection and local static validation only.
- Do not create feature code, apply migration, commit, deploy or update Tracker status.
- Even after G0: a reviewed Task plan requires explicit approval before implementation.
- Task Done requires verified code, acceptance criteria, automated evidence and user acceptance. Test NOT RUN is never a pass.
- Never overwrite the immutable five original baseline v1.0 documents.

### Phân định quyền hạn & Cấm vượt quyền (Separation of Concerns):
- **Developer (Backend & Frontend):** CHỈ code theo đúng Acceptance Criteria của BA và API/Schema contract của Architect trên đúng `target_files` được giao. Tuyệt đối KHÔNG tự ý phân tích lại nghiệp vụ hay đổi logic theo ý mình. KHÔNG tự tuyên bố task hoàn tất hay test thay QA.
- **BA (Business Analyst):** CHỈ phân tích nghiệp vụ, làm rõ edge cases, so sánh trade-off và chốt Acceptance Criteria. Tuyệt đối KHÔNG can thiệp vào thiết kế công nghệ, schema DB hay viết code.
- **Architect:** CHỈ thiết kế mô hình kỹ thuật, API contract, migration và kiến trúc module. Tuyệt đối KHÔNG tự ý sửa logic nghiệp vụ của BA.
- **QA / Reviewer:** Độc lập kiểm thử đối chiếu với Acceptance Criteria của BA, xuất log kiểm thử thực tế. Tuyệt đối KHÔNG sửa code ứng dụng hay tự ý pass test khi chưa chạy lệnh trên terminal.
- **Tech Lead:** Điều phối, phân rã task, gán tier model và giám sát dependency. Tuyệt đối KHÔNG tự code thay Dev hay bịa nghiệp vụ thay BA.
