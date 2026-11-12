# MTMS Multi-Agent Workflow Protocol (Quy trình Điều phối Đa Agent Tự Động)

Tài liệu này quy định chuẩn mực tiếp nhận, phân loại và triển khai công việc giữa **User (Project Owner)** và **Dàn 6 AI Agent** trong dự án `mtms`.

---

## 1. Cơ chế Tự Động Khép Kín (Autonomous Reverse Callback Protocol)

Hệ thống vận hành theo cơ chế **Sự kiện gọi vòng tròn (Event-driven Loop)**. Mỗi agent khi hoàn thành phần việc của mình **BẮT BUỘC PHẢI TỰ ĐỘNG CHẠY LỆNH GỌI NGƯỢC LẠI TECHLEAD**, không để tiến trình bị đứt gãy hay yêu cầu User phải đánh thức thủ công.

```text
[1. User yêu cầu nghiệp vụ]
          ↓ (Tự động)
[2. TechLead phỏng vấn sơ bộ & chạy lệnh gọi BA:]
    herdr agent prompt mtms-business-analyst "..."
          ↓ (Tự động)
[3. BA phân tích, so sánh phương án & TỰ ĐỘNG BÁO CÁO LẠI TECHLEAD:]
    herdr agent prompt mtms-tech-lead "BA đã phân tích xong. Tóm tắt phương án: [...] Mời Lead trình Owner!"
          ↓ (Tự động)
[4. TechLead tự động thức dậy, tổng hợp bảng phương án cho User chốt]
          ↓ (User chọn: "Chốt phương án X")
[5. TechLead chạy lệnh gọi Architect:]
    herdr agent prompt mtms-architect "..."
          ↓ (Tự động)
[6. Architect thiết kế Schema & API Contract rồi TỰ ĐỘNG BÁO CÁO LẠI TECHLEAD:]
    herdr agent prompt mtms-tech-lead "Architect đã hoàn tất thiết kế API/Schema. Mời Lead chia task!"
          ↓ (Tự động)
[7. TechLead chia task và gọi Devs code song song:]
    herdr agent prompt mtms-backend-engineer "..."
    herdr agent prompt mtms-frontend-engineer "..."
          ↓ (Tự động)
[8. Devs hoàn thành code rồi TỰ ĐỘNG BÁO CÁO LẠI TECHLEAD:]
    herdr agent prompt mtms-tech-lead "Devs đã code xong task. Mời Lead chuyển QA test!"
          ↓ (Tự động)
[9. TechLead gọi QA kiểm thử độc lập:]
    herdr agent prompt mtms-qa-reviewer "..."
          ↓ (Tự động)
[10. QA kiểm thử xong rồi TỰ ĐỘNG BÁO CÁO LẠI TECHLEAD:]
    herdr agent prompt mtms-tech-lead "QA đã kiểm thử xong. Kết quả PASS/FAIL. Mời Lead nghiệm thu!"
          ↓ (Tự động)
[11. TechLead tổng kết toàn bộ chứng cứ và báo User nghiệm thu hoàn tất!]
```

---

## 2. Tiêu chuẩn Báo Cáo Ngược (Callback Commands)

| Vai trò thực thi | Lệnh Shell bắt buộc chạy khi hoàn thành nhiệm vụ |
| :--- | :--- |
| **BA** | `herdr agent prompt mtms-tech-lead "BA đã hoàn tất phân tích. Bản tóm tắt các phương án và khuyến nghị đã sẵn sàng. Mời Lead tổng hợp trình Owner chốt!"` |
| **Architect** | `herdr agent prompt mtms-tech-lead "Architect đã hoàn tất thiết kế API Contract và Schema DB. Đã chốt xong interfaces kỹ thuật. Mời Lead phân chia task và gán target_files cho Developers triển khai!"` |
| **Backend Dev** | `herdr agent prompt mtms-tech-lead "Backend Engineer đã code xong task trên các target_files được giao. Local tests đã pass. Mời Lead chuyển sang QA Reviewer kiểm thử độc lập!"` |
| **Frontend Dev** | `herdr agent prompt mtms-tech-lead "Frontend Engineer đã hoàn thành giao diện cho task trên các target_files được giao. Mời Lead chuyển sang QA Reviewer kiểm thử độc lập!"` |
| **QA Reviewer** | `herdr agent prompt mtms-tech-lead "QA Reviewer đã hoàn tất kiểm thử độc lập cho task. Kết quả: [PASS/FAIL]. Log kiểm chứng thực tế đã xuất. Mời Lead tiến hành bước tiếp theo!"` |

---

## 3. Nguyên tắc Vàng: Không vượt quyền (Anti-Scope-Creep)

1. **Dev:** Chỉ code theo AC của BA và Schema của Architect trên đúng `target_files`. Thấy nghiệp vụ bất hợp lý -> báo cáo lại. Không tự đóng vai QA.
2. **BA:** Tập trung nghiệp vụ, trade-off và AC. Không thiết kế schema DB, không viết code.
3. **Architect:** Thiết kế kiến trúc kỹ thuật. Không tự ý thay đổi quyết định nghiệp vụ của BA và User.
4. **QA:** Độc lập kiểm thử. Không tự sửa code ứng dụng của Dev.
5. **TechLead:** Điều phối, gán model tier, đóng vai trò nhạc trưởng kết nối toàn bộ luồng. Không tự làm thay chuyên gia, không tự duyệt release khi chưa có kết quả kiểm thử từ QA.
