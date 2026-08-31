# quiz-service — "Hiểu Bài"

Backend cho ứng dụng giúp phụ huynh tạo bài kiểm tra trắc nghiệm theo bài học, giao cho con làm, và xem kết quả theo mảng kiến thức để biết con hiểu bài đến đâu. Repo này là bản clone từ `api-service` (dùng chung thư viện `base`) — kế thừa toàn bộ hạ tầng ORM/query DSL/error handling/đa hệ CSDL, chỉ khác phần nghiệp vụ.

**Trạng thái hiện tại: repo mới clone, chưa bắt đầu code nghiệp vụ thật.** Trước khi đọc tiếp file này, đọc `docs/00-tong-quan-san-pham.md` và `docs/01-thiet-ke-tong-the.md` để biết đang xây cái gì — file README này chỉ nói *cách* tổ chức code, không nói *xây gì*. Việc cần code cụ thể nằm ở `docs/dev/*.md`, đọc theo đúng thứ tự `01` → `07`.

## 1. Tài liệu chức năng (đọc trước khi code bất kỳ task nào)

```
docs/
├── 00-tong-quan-san-pham.md   — bối cảnh, phạm vi MVP, luồng chức năng chính
├── 01-thiet-ke-tong-the.md    — kiến trúc, data model đầy đủ (10 entity), auth & phân quyền, quy tắc API
└── dev/                        — 1 file / chức năng, đủ chi tiết để code trực tiếp
    ├── 00-quy-uoc-chung.md
    ├── 01-xac-thuc-phan-quyen.md   ← làm trước tiên, mọi API khác phụ thuộc vào đây
    ├── 02-quan-ly-ho-so-con.md
    ├── 03-mon-hoc-bai-hoc.md
    ├── 04-ngan-hang-cau-hoi.md
    ├── 05-tao-giao-bai-kiem-tra.md
    ├── 06-hoc-sinh-lam-bai.md
    └── 07-ket-qua-bao-cao.md
```

## 2. Sơ đồ workspace

```
java-project/
├── quiz-service/    <- repo này (Gradle root project, submodule "api" bên trong)
├── api-service/      <- repo gốc đã clone từ đây, không liên quan tới quiz-service nữa
└── base/               <- repo riêng, thư viện dùng chung (xem base/README.md)
```

`settings.gradle` include `base` bằng đường dẫn tương đối `../base`:

```groovy
rootProject.name = 'quiz-service'
include("api", ":base")
project(":base").projectDir = file("../base")
```

`base` **phải nằm ngay cạnh** (sibling) `quiz-service` trên máy. `git remote` hiện **vẫn trỏ về `api-service.git`** (giữ nguyên từ lúc clone) — cần đổi sang repo GitHub riêng cho `quiz-service` trước khi push, không thì đẩy nhầm vào repo cũ.

## 3. Việc dọn dẹp cần làm đầu tiên (trước khi viết entity thật)

**Đã làm (2026-08-31):**

- ✅ Xoá 4 entity demo `Category`/`StockLevel`/`Tag`/`Article` (cả 4 layer: entity/repository/service/api/dto), toàn bộ `api/src/main/java/vn/org/thn/service/app/example/` đã xoá sạch. Package `vn.org.thn.service.app.quiz.*` đã có sẵn (rỗng), đây là nơi viết entity thật (Task 1 trở đi) — không có gì để "đổi tên" nữa vì entity demo bị xoá thẳng thay vì rename.
- ✅ Xoá migration `database/<engine>/V2__example_entities.sql` (5 engine).
- ✅ `application.yaml` (`api/src/main/resources/`): `spring.application.name` → `quiz-service`, `db-name` → `quiz_db` (giữ nguyên `dbPrefix: dev` — đây là prefix môi trường bình thường, không phải giá trị demo → DB thật sẽ là `dev_quiz_db`).

**Còn lại (chưa làm, để nguyên theo yêu cầu):**

- `git remote` vẫn trỏ về `api-service.git` — cần đổi sang repo riêng trước khi push.
- `database/*/V1__init.sql` hiện tại (bảng `translate`/demo kế thừa từ `api-service`) chưa đụng tới — sẽ xử lý khi bắt đầu viết migration cho entity thật (Task 1 trở đi), không thuộc phạm vi dọn dẹp lần này.

Xem chi tiết ở `docs/dev/00-quy-uoc-chung.md`.

## 4. Cấu trúc thư mục

```
quiz-service/
├── build.gradle, settings.gradle
├── database/<engine>/Vn__*.sql        # migration Flyway, 5 engine song song
├── mapper/                             # MyBatis XML mapper dùng chung
├── lang/, data/, logs/                  # runtime — xem base/README.md
├── docs/                                 # tài liệu chức năng, xem mục 1
└── api/                                   # submodule Gradle ":api" — service nghiệp vụ
    └── src/main/java/vn/org/thn/service/app/
        ├── AppApplication.java
        └── quiz/                          # package nghiệp vụ (đích đến sau khi đổi từ "example")
            ├── api/         *Api.java       # extends BaseCtl, base path /api/parent/** hoặc /api/student/**
            ├── service/     *Service.java   # extends IBase
            ├── repository/  *Repository.java # extends BaseRepositoryImpl<Entity, IdType>
            ├── entity/      *.java
            ├── dto/         *Request.java
            ├── security/     JwtAuthFilter, JwtUtil, CurrentUser  # mới — xem docs/dev/01
            └── exception/    QuizErrorCode.java implements ErrorCode
```

cwd khi chạy app là `quiz-service/` (thư mục gốc, không phải `api/`) — `DatabasePath` trong `base` dựa vào cwd lúc chạy, không phải thư mục source. Đó là lý do `database/`, `mapper/`, `lang/`, `data/` nằm ở gốc.

## 5. Quy ước code (kế thừa từ `api-service`, không đổi)

- **Layer, không phải feature-based**: entity/service/repository/api mới đi vào đúng thư mục layer trong `api/.../app/quiz/`.
- Naming: `entity/Xxx`, `repository/XxxRepository extends BaseRepositoryImpl<Xxx, IdType>`, `service/XxxService extends IBase`, `api/XxxApi extends BaseCtl` (**không** `XxxController`), `dto/XxxRequest`.
- **Lombok `@Data`** cho mọi entity/DTO. Entity `extends BaseEntity` bắt buộc thêm `@EqualsAndHashCode(callSuper = true)` + `@ToString(callSuper = true)` — thiếu sẽ khiến Lombok âm thầm bỏ qua 5 field kế thừa khi sinh `equals`/`hashCode`/`toString`.
- **`BaseEntity` không có field `id`** — mỗi entity tự khai `@Id` theo đúng kiểu khoá nó cần.
- 4 field audit của `BaseEntity` **không tự động điền** — service phải tự gán trước khi `save()`.
- **Lỗi nghiệp vụ**: luôn `BusinessException(errorCode[, message])`, không exception JDK trần. Định nghĩa lỗi riêng của `quiz-service` ở `exception/QuizErrorCode.java` implements `ErrorCode`, tách khỏi `CommonErrorCode`.
- **Query CSDL**: ưu tiên `QueryBuilder`/`UpdateBuilder`/`DeleteBuilder` (method reference thay vì chuỗi tên field). `UpdateBuilder.execute()`/`DeleteBuilder.execute()` bắt buộc có ít nhất 1 điều kiện WHERE.
- **Phân quyền dữ liệu (riêng của `quiz-service`, không có trong `base`):** mọi `*Service` khi query/update phải lọc theo `parentId`/`studentId` lấy từ `CurrentUser` (xem `docs/dev/01`) — không dùng `findAll()`/`findById()` trần cho dữ liệu thuộc về 1 Parent/Student cụ thể, phải luôn kèm điều kiện sở hữu. Đây là lỗi dễ mắc nhất nếu copy nguyên khuôn từ `api-service` (vốn không cần phân quyền theo user).

Chi tiết API/entity/business rule của từng chức năng: xem `docs/dev/*.md`, không lặp lại ở đây. Chi tiết cách dùng `QueryBuilder`/repository/composite key: xem `../base/README.md`.

## 6. Cấu hình đa hệ quản trị CSDL (tóm tắt, chi tiết ở `base/README.md`)

```yaml
base:
  database:
    type: POSTGRESQL              # SQLITE / POSTGRESQL / MYSQL / SQLSERVER / ORACLE
    db-name: example_db           # ⚠ vẫn là giá trị demo — cần đổi tên database thật cho quiz-service
    dbPrefix: dev
    postgresql:
      host: localhost
      port: 5432
```

Đổi engine: `base.database.type` (hoặc env `BASE_DATABASE_TYPE`). DB đích tự tạo nếu chưa tồn tại (Postgres/MySQL/SQL Server) — Oracle không tự tạo PDB, cần có sẵn.

## 7. Migration SQL (Flyway)

Vị trí: `database/<engine>/Vn__<mo_ta>.sql`, **5 engine song song**. Không cần `IF NOT EXISTS`. Cột text nên nullable (Oracle coi `''` là `NULL`). Cột tự tăng khai khác nhau theo engine — xem `../base/README.md` hoặc migration mẫu còn sót lại trong `database/*/V1__init.sql` (sẽ bị thay bằng migration thật của `quiz-service`, xem mục 3).

## 8. Build & chạy

```bash
cd quiz-service
./gradlew :api:bootRun          # chạy app (cwd = quiz-service/)
./gradlew test                   # useJUnitPlatform() đã bật sẵn ở base + api
./gradlew :base:compileJava      # build riêng base khi chỉ sửa base
```

Swagger UI: `/api.html`.

## 9. Cạm bẫy đã gặp khi làm `api-service`/`base` (áp dụng nguyên cho `quiz-service`, cùng stack)

- `base/application.yaml` gần như không dùng — 2 file `application.yaml` trùng tên trên classpath, Spring không gộp, file của module `api` luôn thắng.
- `base/logback-spring.xml` có sẵn → `logging.pattern.*` trong `application.yaml` bị bỏ qua hoàn toàn, sửa log pattern phải sửa file XML.
- Property đa hệ CSDL phải lồng đúng cấp theo engine (`base.database.postgresql.host`, không phải `base.database.host`) — sai cấp không báo lỗi, chỉ âm thầm dùng default.
- Spring Boot 4 đổi package 1 số autoconfig quen thuộc (`@AutoConfigureMockMvc`, `WebMvcAutoConfiguration`, `DataSourceProperties` → namespace mới) — gặp lỗi "package does not exist" thì tra lại docs.spring.io theo đúng version.
- Jackson 3: `com.fasterxml.jackson.*` → `tools.jackson.*` (trừ `jackson-annotations`).
- `useJUnitPlatform()` phải khai rõ trong `build.gradle`, không tự bật.
- Oracle: `InsertExecutor` phải `INSERT` rồi `SELECT MAX(id)` riêng để lấy id — không an toàn tuyệt đối khi insert đồng thời cao.
- `.idea/gradle.xml` `offlineMode` có thể gây lỗi "No cached version" khi thêm dependency mới dù khai đúng cú pháp.

## 10. Tài liệu liên quan

- `docs/` — chức năng/task cần code, đọc trước README này.
- `../base/README.md` — API đầy đủ của `base`.
- `../api-service/README.md` — README gốc trước khi clone, tham khảo nếu cần đối chiếu quy ước chung (không phải nguồn quyết định cho `quiz-service`).
