# Thiết kế tổng thể — quiz-service

**Trạng thái:** Thiết kế cho MVP v1 (xem phạm vi ở `00-tong-quan-san-pham.md`). Chưa code — tài liệu này là input để viết `dev/*.md` và sau đó code thật.

## 1. Kiến trúc

`quiz-service` là bản clone từ `api-service` (đã đổi `rootProject.name` = `quiz-service`, module business `example` → `api`, dùng chung sibling project `base` — xem `../README.md` kế thừa từ `api-service`). Về mặt kỹ thuật: Spring Boot 4.0, MyBatis (không JPA), Lombok, đa hệ CSDL qua `base` — toàn bộ hạ tầng chung (ORM/query DSL, `BaseCtl`/`ApiResponse`, `BusinessException`/`GlobalExceptionHandler`, Flyway migration theo `database/<engine>/`) giữ nguyên như đã có, xem `base/README.md`.

**Package nghiệp vụ:** đổi từ `vn.org.thn.service.app.example.*` (kế thừa từ demo `api-service`) sang **`vn.org.thn.service.app.quiz.*`** — quyết định đã chốt 2026-08-31. Khi bắt đầu code, việc đầu tiên là dọn 4 entity demo (`Category`/`StockLevel`/`Tag`/`Article`) và đổi package, trước khi viết entity thật.

**Điểm khác biệt lớn nhất so với `api-service`/`base` hiện tại — auth/phân quyền chưa có sẵn:** theo `claude/base-module-status.md`, phần Security/JWT đã bị **xoá hẳn khỏi `base`** ở giai đoạn port ban đầu (không cần cho service demo lúc đó). `quiz-service` là ứng dụng multi-tenant thật (nhiều gia đình dùng chung 1 hệ thống, dữ liệu của gia đình này không được lộ cho gia đình khác) nên **bắt buộc phải có auth** — đây là phần hạ tầng mới hoàn toàn, không có sẵn trong `base`, xem mục 3.

## 2. Data Model đầy đủ

Tất cả entity dưới đây `extends BaseEntity` trừ khi ghi chú khác (tự khai `@Id` theo đúng quy ước `base` đã có — xem `base/README.md` mục "Defining an entity").

### Parent (bảng `parent`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| fullName | String | |
| email | String | unique, dùng đăng nhập |
| password | String | hash (BCrypt), không bao giờ trả về trong response |
| phone | String | optional |

### Student (bảng `student`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| parentId | Long | FK → Parent, bắt buộc |
| fullName | String | |
| grade | String | "Lớp" — ví dụ "Lớp 3", tự do (không enum cứng ở v1) |
| username | String | unique toàn hệ thống, do phụ huynh đặt |
| password | String | hash (BCrypt), do phụ huynh đặt/reset |

### Subject — Môn học (bảng `subject`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| parentId | Long | FK → Parent — mỗi phụ huynh có danh sách môn riêng, không dùng chung giữa các gia đình ở v1 |
| name | String | ví dụ "Tiếng Anh", "Toán" |

### Lesson — Bài học (bảng `lesson`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| subjectId | Long | FK → Subject |
| name | String | ví dụ "Unit 1 - Thì hiện tại đơn" |

### Question — Câu hỏi (bảng `question`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| lessonId | Long | FK → Lesson |
| content | String | nội dung câu hỏi |
| knowledgeTag | String | optional, tag nhóm kiến thức tự do (ví dụ "Do/Does") |

### Choice — Lựa chọn của câu hỏi (bảng `choice`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| questionId | Long | FK → Question |
| content | String | nội dung lựa chọn |
| correct | Boolean | đúng/sai — mỗi Question phải có đúng 1 Choice `correct=true` |

*(Choice không cần `extends BaseEntity` — không cần audit fields cho dữ liệu con phụ thuộc hoàn toàn vòng đời Question.)*

### Test — Bài kiểm tra (bảng `test`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| parentId | Long | FK → Parent, ai tạo |
| studentId | Long | FK → Student, giao cho ai |
| name | String | tên bài kiểm tra |
| status | String | `ASSIGNED` / `COMPLETED` — set `COMPLETED` khi Student đã nộp Attempt |

### TestQuestion — liên kết Test–Question, giữ thứ tự (bảng `test_question`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| testId | Long | FK → Test |
| questionId | Long | FK → Question |
| orderIndex | Integer | thứ tự hiển thị câu hỏi trong bài |

*(Không cần `extends BaseEntity`.)*

### Attempt — 1 lần học sinh làm 1 Test (bảng `attempt`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| testId | Long | FK → Test |
| studentId | Long | FK → Student |
| startedAt | LocalDateTime | lúc bắt đầu làm |
| submittedAt | LocalDateTime | null nếu chưa nộp |
| correctCount | Integer | số câu đúng, tính lúc chấm |
| totalQuestions | Integer | tổng số câu của Test tại thời điểm làm |

*(MVP v1: 1 Test chỉ cho phép 1 Attempt — không hỗ trợ làm lại, xem `dev/06-hoc-sinh-lam-bai.md`.)*

### AttemptAnswer — câu trả lời trong 1 lần làm (bảng `attempt_answer`)

| Field | Kiểu | Ghi chú |
|---|---|---|
| id | Long | `@Id @GeneratedValue(IDENTITY)` |
| attemptId | Long | FK → Attempt |
| questionId | Long | FK → Question |
| choiceId | Long | FK → Choice đã chọn, null nếu bỏ trống câu đó |
| correct | Boolean | tính khi chấm, so `choiceId` với Choice có `correct=true` của Question |

*(Không cần `extends BaseEntity`.)*

### Sơ đồ quan hệ

```text
Parent 1───N Student
Parent 1───N Subject 1───N Lesson 1───N Question 1───N Choice
Parent 1───N Test ───N Student (giao cho 1 con)
Test N───N Question (qua TestQuestion, giữ thứ tự)
Test 1───N Attempt (v1: thực chất 1───1, chặn tạo Attempt thứ 2)
Attempt 1───N AttemptAnswer ── Question, Choice
```

## 3. Auth & phân quyền (hạ tầng mới, cần bổ sung vào `base` hoặc viết riêng trong `quiz`)

**Quyết định đã chốt (2026-08-31):** Student đăng nhập bằng **username + password** (giống Parent, không dùng PIN) — dùng chung 1 cơ chế auth cho cả 2 role, đơn giản hoá triển khai.

**Cơ chế đề xuất:** JWT stateless — Parent/Student đăng nhập trả về 1 token, các API sau đó gửi kèm header `Authorization: Bearer <token>`. Token payload chứa `userId` + `role` (`PARENT`/`STUDENT`). Cần viết mới:

- 1 filter xác thực token (theo mẫu `RequestContextFilter` đã có trong `base`, dùng MDC/request attribute lưu "current user" — không phải Spring Security đầy đủ, giữ đơn giản đúng tinh thần `base` hiện tại).
- Password hash bằng BCrypt (thêm dependency `spring-security-crypto` là đủ, không cần full Spring Security nếu muốn tối giản).
- Helper lấy "current user" (userId + role) trong tầng Service, tương tự cách `BaseCtl.getClientIp()` đọc từ MDC hiện có.

**Nguyên tắc phân quyền dữ liệu (áp dụng cho mọi API, ghi rõ trong từng `dev/*.md`):**
- Mọi API dưới `/api/parent/**` chỉ thao tác được trên dữ liệu thuộc **chính Parent đang đăng nhập** — mọi query phải lọc theo `parentId` (trực tiếp, hoặc gián tiếp qua Student/Lesson/Question thuộc Parent đó). Truy cập dữ liệu của Parent khác → `BusinessException(FORBIDDEN)`.
- Mọi API dưới `/api/student/**` chỉ thao tác được trên dữ liệu thuộc **chính Student đang đăng nhập** (bài được giao cho mình, attempt của mình).

## 4. Quy tắc API chung

- Base path: `/api/parent/**` (role Parent) và `/api/student/**` (role Student), tách biệt rõ theo role — dev đọc path là biết ngay role nào được gọi.
- Response: dùng `ApiResponse<T>` chuẩn của `base` (`ok(...)`/`fail(...)` qua `BaseCtl`) — không tự chế response format riêng.
- Danh sách có phân trang: dùng `PageRequest`/`PageResponse` có sẵn trong `base`.
- Lỗi nghiệp vụ: `BusinessException` với `ErrorCode` riêng cho `quiz` (enum mới implements `ErrorCode`, tách khỏi `CommonErrorCode` — theo đúng quy ước `api-service/README.md` mục 4).
- Upload file (import câu hỏi Excel/CSV — xem `dev/04-ngan-hang-cau-hoi.md`): dùng `multipart/form-data` chuẩn Spring MVC, không tự chế cơ chế upload riêng. Đọc file bằng thư viện đọc có cấu trúc chuẩn (Apache POI cho `.xlsx`, Apache Commons CSV cho `.csv`) — cần thêm dependency mới vào `api/build.gradle`, `base` hiện chưa có sẵn 2 thư viện này.

## 5. Tài liệu chi tiết theo chức năng

Xem `dev/` — mỗi file 1 chức năng/task, đủ chi tiết (entity, API, business rule, acceptance criteria) để code trực tiếp. Thứ tự nên làm: `01` (auth, mọi API khác phụ thuộc) → `02` → `03` → `04` → `05` → `06` → `07`.
