# Task brief — P0-ORG-03 Entity and branch domain (PLANNING)

- **Prepared:** 2026-11-13
- **Gate status:** `G0 SIGNED` on 2026-11-13; `P0-ORG-01` and `P0-ORG-02` completed and verified.
- **Parent Task:** `P0-ORG-03` — Entity and branch domain.
- **BR IDs:** `BR-ORG-01`, `BR-ORG-02` (`BASELINED`) — Separate registered entity from branch; mixed household-business topology (`ADR-002`).
- **TC IDs:** `TC-003` (linked in Tracker; full bank account onboarding is finalized in `P0-ORG-07`; `P0-ORG-03` provides domain entity/branch isolation and composite validation).
- **Dependency IDs + status:** `P0-ORG-02` (PostgreSQL and Flyway baseline `V001`) is satisfied.
- **Owner / reviewers:** Business Owner (approval & acceptance), Tech Lead (coordination & boundaries), Architect (domain model & API contract), Backend Engineer (implementation), QA Reviewer (testing).

---

## 1. Mục tiêu nghiệp vụ (Business Intent) & Định nghĩa Hoàn thành (DoD)

### 1.1 Business Intent
- Xây dựng tầng Domain, Application, Persistence và REST API cho Pháp nhân kinh doanh (`BusinessEntity`) và Chi nhánh (`Branch`).
- Tách bạch rõ rành giữa Pháp nhân (Hộ kinh doanh / Công ty) và Chi nhánh (Quán trà sữa thực tế). Một pháp nhân có thể sở hữu nhiều chi nhánh (`1 - N`).
- Đảm bảo tính toàn vẹn dữ liệu: Không có chi nhánh mồ côi; mã chi nhánh duy nhất trong cùng pháp nhân nhưng cho phép trùng mã ở 2 pháp nhân khác nhau.

### 1.2 Definition of Done (DoD)
1. Tạo được ít nhất 2 pháp nhân và nhiều chi nhánh thông qua API/Service.
2. Kiểm tra ràng buộc duy nhất `entity_code` và `(business_entity_id, branch_code)`.
3. Kiểm tra ràng buộc khóa ngoại: Không cho phép tạo chi nhánh trỏ tới pháp nhân không tồn tại.
4. Kiểm soát ranh giới module bằng ArchUnit test không bị phá vỡ.
5. Unit tests và Controller Integration tests đạt 100% PASS.

---

## 2. Phạm vi thực thi (Scope)

### 2.1 Trong phạm vi (In Scope)
1. **Domain Layer:**
   - Entity & Value Objects: `BusinessEntity`, `Branch`, `BusinessEntityStatus`, `BranchStatus`.
   - Domain invariants: Không chấp nhận code rỗng, tên rỗng, status sai enum.
2. **Persistence Layer:**
   - JPA Entities: `BusinessEntityJpaEntity`, `BranchJpaEntity` ánh xạ vào schema `mtms.business_entities` và `mtms.branches` (đã tạo ở V001).
   - Spring Data JPA Repositories: `BusinessEntityJpaRepository`, `BranchJpaRepository`.
3. **Application Layer:**
   - Use cases / Service: `OrganizationService` cung cấp các phương thức nghiệp vụ:
     - Tạo pháp nhân mới (`createBusinessEntity`).
     - Truy vấn danh sách và chi tiết pháp nhân.
     - Tạo chi nhánh mới thuộc pháp nhân (`createBranch`).
     - Truy vấn danh sách chi nhánh theo pháp nhân và chi tiết chi nhánh.
   - Validation & DTOs: `CreateBusinessEntityCommand`, `BusinessEntityDto`, `CreateBranchCommand`, `BranchDto`.
   - Custom Business Exceptions: `DuplicateEntityCodeException`, `DuplicateBranchCodeException`, `EntityNotFoundException`.
4. **Web / REST API Layer:**
   - `OrganizationController`:
     - `POST /api/v1/organizations/entities`
     - `GET /api/v1/organizations/entities`
     - `GET /api/v1/organizations/entities/{id}`
     - `POST /api/v1/organizations/entities/{entityId}/branches`
     - `GET /api/v1/organizations/entities/{entityId}/branches`
     - `GET /api/v1/organizations/entities/{entityId}/branches/{branchId}`
5. **Testing:**
   - Unit tests cho `OrganizationService`.
   - REST API tests cho `OrganizationController`.

### 2.2 Ngoài phạm vi (Explicit Non-Goals)
- Không làm xác thực người dùng / login (`P0-ORG-04`).
- Không làm phân quyền scoped role grants (`P0-ORG-05`).
- Không tạo tài khoản ngân hàng chi nhánh (`P0-ORG-07`).
- Không tạo giao diện Frontend (`P0-ORG-06`).
- Không làm POS, bán hàng, kiểm kho hay giao dịch tài chính.

---

## 3. Danh sách tệp được phép tác động (Target Files)

### ADD
- `backend/src/main/java/com/mtms/organization/domain/BusinessEntity.java`
- `backend/src/main/java/com/mtms/organization/domain/Branch.java`
- `backend/src/main/java/com/mtms/organization/domain/BusinessEntityStatus.java`
- `backend/src/main/java/com/mtms/organization/domain/BranchStatus.java`
- `backend/src/main/java/com/mtms/organization/application/OrganizationService.java`
- `backend/src/main/java/com/mtms/organization/application/dto/CreateBusinessEntityCommand.java`
- `backend/src/main/java/com/mtms/organization/application/dto/BusinessEntityDto.java`
- `backend/src/main/java/com/mtms/organization/application/dto/CreateBranchCommand.java`
- `backend/src/main/java/com/mtms/organization/application/dto/BranchDto.java`
- `backend/src/main/java/com/mtms/organization/application/exception/DuplicateEntityCodeException.java`
- `backend/src/main/java/com/mtms/organization/application/exception/DuplicateBranchCodeException.java`
- `backend/src/main/java/com/mtms/organization/application/exception/EntityNotFoundException.java`
- `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BusinessEntityJpaEntity.java`
- `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchJpaEntity.java`
- `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BusinessEntityJpaRepository.java`
- `backend/src/main/java/com/mtms/organization/infrastructure/persistence/BranchJpaRepository.java`
- `backend/src/main/java/com/mtms/organization/web/OrganizationController.java`
- `backend/src/test/java/com/mtms/organization/OrganizationServiceTest.java`
- `backend/src/test/java/com/mtms/organization/OrganizationControllerTest.java`

### MODIFY
- `backend/src/main/java/com/mtms/common/error/GlobalExceptionHandler.java` (bổ sung xử lý `EntityNotFoundException`, `DuplicateEntityCodeException`, `DuplicateBranchCodeException` trả về đúng HTTP 404, 409).
- `docs/planning/CURRENT_STATE.md`
- `docs/ai/CHANGELOG.md`

### KEEP
- Toàn bộ các file baseline v1.0, các migration `V001__...`, infra Docker, frontend.
