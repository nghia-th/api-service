# Quy ước chung cho dev — quiz-service

Đọc file này trước khi làm bất kỳ task nào trong thư mục `dev/`. Không lặp lại quy ước đã có ở 2 nơi sau — đọc trước:

- `../../README.md` (thừa hưởng từ `api-service`, cần cập nhật lại cho `quiz-service` khi bắt đầu code) — quy tắc thêm nghiệp vụ mới, layer/naming, Lombok, `BaseEntity`, `BusinessException`, migration Flyway.
- `../../../base/README.md` — API đầy đủ của `base` (ORM/query DSL, `BaseCtl`/`ApiResponse`, đa hệ CSDL, error handling).

## Riêng cho quiz-service

**Package gốc:** `vn.org.thn.service.app.quiz.*` (đã chốt 2026-08-31, thay cho `example` kế thừa từ demo `api-service`). Cấu trúc layer y hệt quy ước cũ:

```
api/src/main/java/vn/org/thn/service/app/
└── quiz/
    ├── api/          *Api.java       — REST controller, extends BaseCtl
    ├── service/      *Service.java   — business logic, extends IBase
    ├── repository/   *Repository.java — extends BaseRepositoryImpl<Entity, IdType>
    ├── entity/       *.java           — @Entity @Table
    ├── dto/          *Request.java    — request body
    ├── security/     — JwtFilter, JwtUtil, CurrentUser helper (mới, xem dev/01)
    └── exception/    QuizErrorCode.java — enum implements ErrorCode, mã lỗi riêng quiz-service
```

**Việc đầu tiên khi bắt đầu code (trước khi viết entity thật) — ĐÃ LÀM (2026-08-31):** đã xoá 4 entity demo `Category`/`StockLevel`/`Tag`/`Article` (cả 4 layer: entity/repository/service/api/dto), migration `V2__example_entities.sql` liên quan (5 engine), và sửa `application.yaml` (`spring.application.name` → `quiz-service`, `db-name` → `quiz_db`). Package `example` đã xoá sạch, package `quiz` rỗng sẵn sàng cho entity thật. Còn lại chưa làm: `git remote` vẫn trỏ `api-service.git`, `V1__init.sql` (bảng translate/demo) chưa dọn — để xử lý sau, không thuộc phạm vi dọn dẹp lần này.

**Danh sách entity đầy đủ:** xem `../01-thiet-ke-tong-the.md` mục 2 — 10 entity: `Parent`, `Student`, `Subject`, `Lesson`, `Question`, `Choice`, `Test`, `TestQuestion`, `Attempt`, `AttemptAnswer`.

**Base path API:** `/api/parent/**` (role Parent) và `/api/student/**` (role Student) — mọi `*Api.java` mới đặt `@RequestMapping` theo đúng 1 trong 2 prefix này, không lẫn lộn.

**Migration Flyway:** viết ở `database/<engine>/Vn__*.sql` (5 engine song song, đúng quy ước đã có) — bắt đầu từ `V1__` mới cho `quiz-service` (không kế thừa `V1__init.sql`/`V2__example_entities.sql` của `api-service`, vì đó là bảng `translate`/demo không liên quan).

**Mã lỗi riêng:** tạo `exception/QuizErrorCode.java` implements `ErrorCode` (theo đúng mẫu `CommonErrorCode` bên `base`) cho các lỗi đặc thù quiz-service, ví dụ: `STUDENT_USERNAME_TAKEN`, `QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE`, `TEST_ALREADY_HAS_ATTEMPT`, `FORBIDDEN_NOT_OWNER`.

## Thứ tự làm task (mỗi task 1 file trong thư mục này)

1. `01-xac-thuc-phan-quyen.md` — **làm trước tiên**, mọi API khác đều cần "current user" từ đây.
2. `02-quan-ly-ho-so-con.md`
3. `03-mon-hoc-bai-hoc.md`
4. `04-ngan-hang-cau-hoi.md`
5. `05-tao-giao-bai-kiem-tra.md`
6. `06-hoc-sinh-lam-bai.md`
7. `07-ket-qua-bao-cao.md`
