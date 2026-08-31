# api-service

Tài liệu này dành cho dev tham gia repo `api-service` (thư mục/repo trước đây tên `example-service`) — mô tả cấu trúc thư mục thật, quy tắc viết code, và các cạm bẫy đã gặp trong quá trình dựng project. Đọc file này trước khi bắt đầu thêm nghiệp vụ mới, để làm đúng ngay từ đầu thay vì phải sửa lại nhiều vòng.

Tài liệu kỹ thuật chi tiết về thư viện dùng chung `base` (API đầy đủ của ORM/query DSL, error handling, đa hệ CSDL, i18n...) nằm ở `../base/README.md` (tiếng Anh) — file này **không lặp lại** nội dung đó, chỉ tập trung vào cách tổ chức + quy tắc riêng của workspace này.

## 1. Sơ đồ workspace

Project gồm 2 repo Git riêng biệt, nằm cạnh nhau trên máy:

```
java-project/
├── api-service/     <- repo này (Gradle root project, có submodule "api" bên trong)
└── base/            <- repo riêng, thư viện dùng chung (xem base/README.md)
```

`api-service/settings.gradle` include `base` bằng đường dẫn tương đối `../base`:

```groovy
rootProject.name = 'api-service'
include("api", ":base")
project(":base").projectDir = file("../base")
```

Nghĩa là `base` **phải nằm ngay cạnh** (sibling) `api-service` trên máy — không tự ý đổi vị trí nếu không sửa `settings.gradle` theo.

> **Về việc đổi tên (đã cập nhật):** module Gradle của service nghiệp vụ đã đổi tên từ `:example` sang `:api` (thư mục `example/` → `api/`), và `rootProject.name` cũng đã sửa khớp `api-service`. **Lưu ý dễ nhầm:** package Java bên trong — `vn.org.thn.service.app.example.*` — vẫn giữ nguyên tên `example` (đây là tên gói nghiệp vụ mẫu, không phải tên module Gradle) — 2 khái niệm khác nhau, đừng nhầm khi đọc code hay import.

## 2. Cấu trúc thư mục `api-service`

```
api-service/
├── build.gradle, settings.gradle      # root Gradle project, include :api và :base
├── database/                          # migration SQL (Flyway), 1 thư mục con / engine
│   ├── sqlite/       Vx__*.sql
│   ├── postgresql/   Vx__*.sql
│   ├── mysql/        Vx__*.sql
│   ├── sqlserver/    Vx__*.sql
│   └── oracle/       Vx__*.sql
├── mapper/                            # MyBatis XML mapper dùng chung (DynamicSQL.xml)
├── lang/                               # file JSON đa ngôn ngữ (vi.json/en.json) — tự sinh lúc app khởi động
├── data/                                # file DB SQLite (app.db) khi type=SQLITE
├── logs/                                 # log runtime (info_*.log / error_*.log)
└── api/                                    # submodule Gradle ":api" — service nghiệp vụ thật
    ├── build.gradle
    └── src/main/java/vn/org/thn/service/app/
        ├── AppApplication.java            # entrypoint Spring Boot — KHÔNG đổi package này
        └── example/                        # package con — tên gói nghiệp vụ (giữ nguyên "example", không đổi theo tên module)
            ├── api/         *Api.java       # REST controller, extends BaseCtl
            ├── service/     *Service.java   # business logic, extends IBase
            ├── repository/  *Repository.java # extends BaseRepositoryImpl<Entity, IdType>
            ├── entity/      *.java           # @Entity @Table, tự khai @Id riêng
            └── dto/         *Request.java     # request body
```

**Vì sao có 1 package con `example` bên trong `app`:** `example` ở đây là tên nghiệp vụ/service (đặt từ đầu, chưa đổi theo tên module Gradle), tách khỏi package gốc `app` (chứa `AppApplication`). Nếu sau này service này có thêm nghiệp vụ khác, tạo thêm 1 package con mới cùng cấp `example` (ví dụ `app.order`, `app.inventory`...), không nhét trực tiếp vào `app`. Nếu muốn đổi luôn tên package `example` cho khớp tên module `api`, cần sửa lại `package`/import ở toàn bộ ~20 file trong `api/src/main/java/.../app/example/` — chưa làm, báo em nếu anh muốn đổi.

**cwd khi chạy app rất quan trọng:** `DatabasePath` (trong `base`) lấy đường dẫn tương đối theo *thư mục làm việc lúc app chạy* (cwd), không phải thư mục source code. Chạy `:api:bootRun` (qua IntelliJ hay terminal) thì cwd là `api-service/` (thư mục gốc) — đó là lý do `database/`, `mapper/`, `lang/`, `data/` đều nằm ở gốc `api-service/`, không phải bên trong `api/`.

## 3. Quy tắc thêm nghiệp vụ mới

Chia theo **layer** (không phải feature-based) — entity/service/repository/api mới đều đi vào đúng 5 thư mục layer có sẵn trong `api/.../app/example/`, **không** tạo thêm thư mục con theo tên nghiệp vụ riêng.

Quy ước đặt tên (theo đúng 4 entity mẫu `Category`/`StockLevel`/`Tag`/`Article` đã có sẵn để tham khảo):

| Layer | Tên class | Kế thừa | Ghi chú |
|---|---|---|---|
| `entity` | `Xxx` | tuỳ chọn `extends BaseEntity` | tự khai `@Id` (+ `@GeneratedValue` nếu tự tăng); nếu `extends BaseEntity` phải thêm `@EqualsAndHashCode(callSuper = true)` + `@ToString(callSuper = true)` cạnh `@Data` (xem mục 4) |
| `repository` | `XxxRepository` | `extends BaseRepositoryImpl<Xxx, IdType>` | `@Repository`, không cần viết method gì thêm cho CRUD cơ bản |
| `service` | `XxxService` | `extends IBase` | `@Service`, nghiệp vụ thật, ném `BusinessException` khi lỗi (xem mục 4) |
| `api` | `XxxApi` | `extends BaseCtl` | `@RestController`, dùng `ok(...)`/`fail(...)` để trả `ApiResponse` — **không** đặt tên `XxxController` |
| `dto` | `XxxRequest` | — | request body; response trả thẳng entity hoặc `PageResponse<Xxx>`, không cần DTO response riêng trừ khi cần ẩn field |

Ví dụ mẫu tối thiểu (rút gọn từ `Category` đã có sẵn trong `api/src/main/java/vn/org/thn/service/app/example/{entity,repository,service,api}/Category*.java`):

```java
// entity
@Data
@Entity
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    private Boolean active;
}

// repository
@Repository
public class CategoryRepository extends BaseRepositoryImpl<Category, Long> {}

// service
@Service
public class CategoryService extends IBase {
    @Autowired
    private CategoryRepository categoryRepository;

    public Category get(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    }
}

// api
@RestController
@RequestMapping("/example/category")
public class CategoryApi extends BaseCtl {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/{id}")
    public ApiResponse<Category> get(@PathVariable Long id) {
        return ok(categoryService.get(id));
    }
}
```

Chi tiết cách dùng `QueryBuilder`/`UpdateBuilder`/`DeleteBuilder`, composite key, native query escape hatch... xem `base/README.md` mục "Repositories" / "The query DSL".

## 4. Quy ước code

- **Lombok `@Data`** cho mọi entity/DTO mới (đã áp dụng cho toàn bộ entity/DTO hiện có, không viết getter/setter tay). Ngoại lệ: class chỉ dựng qua factory method tĩnh, không có setter, cần giữ bất biến — như `ApiResponse`/`PageResponse` — dùng `@Getter` thay vì `@Data` để không tự sinh setter phá vỡ tính bất biến.
- Entity `extends BaseEntity` **bắt buộc** thêm `@EqualsAndHashCode(callSuper = true)` + `@ToString(callSuper = true)` cạnh `@Data` (xem `Article.java` làm mẫu) — thiếu 2 annotation này thì Lombok sẽ **âm thầm bỏ qua 5 field kế thừa** (`createdAt`/`updatedAt`/`createdBy`/`updatedBy`/`deleted`) khi sinh `equals`/`hashCode`/`toString`.
- **`BaseEntity` không có field `id`** — mỗi entity tự khai khoá chính theo đúng kiểu nó cần (tự tăng/String tự đặt/UUID/khoá kép). Đây là quyết định thiết kế có chủ đích, không phải thiếu sót — không thêm `id` vào `BaseEntity`.
- 4 field audit của `BaseEntity` (`createdAt`/`updatedAt`/`createdBy`/`updatedBy`) **không tự động điền** — không có interceptor nào set hộ. Nếu entity kế thừa `BaseEntity`, service phải tự gán các field này (thường từ `LocalDateTime.now()` + user hiện tại) trước khi gọi `save()` — xem `ArticleService` làm ví dụ. `deleted` cũng chỉ là cột dữ liệu thường, không tự lọc — muốn soft-delete phải tự thêm điều kiện `.eq(Entity::isDeleted, false)` vào query và tự set flag thay vì gọi `deleteById()`.
- **Lỗi nghiệp vụ**: luôn ném `BusinessException(errorCode[, message])`, không ném exception JDK trần (`NoSuchElementException`, `IllegalStateException`...) — `GlobalExceptionHandler` chỉ trả đúng HTTP status (404, 400...) qua `ApiResponse` chuẩn khi bắt được `BusinessException`/`BaseException`; exception JDK trần sẽ rơi vào handler catch-all 500 chung chung. Định nghĩa mã lỗi riêng cho từng service bằng 1 enum implements `ErrorCode`, tách khỏi `CommonErrorCode` (chỉ dùng cho lỗi dùng chung mọi service, xem `base/exception/CommonErrorCode.java`).
- **Controller** đặt tên `*Api` (không phải `*Controller`), extends `BaseCtl`, trả về qua `ok(...)`/`fail(...)`.
- **Query CSDL**: ưu tiên `QueryBuilder`/`UpdateBuilder`/`DeleteBuilder` (fluent DSL) — ưu tiên overload dùng method reference (`Category::getName`) thay vì chuỗi tên field để an toàn lúc compile. Chỉ dùng native SQL (`nativeQuery`/`mapper(...)`) khi DSL không biểu diễn được.
- `UpdateBuilder.execute()`/`DeleteBuilder.execute()` bắt buộc phải có ít nhất 1 điều kiện WHERE — DSL tự chặn UPDATE/DELETE toàn bảng do quên điều kiện, không cần tự kiểm tra thêm.

## 5. Cấu hình đa hệ quản trị CSDL (tóm tắt)

Chi tiết đầy đủ ở `base/README.md` mục "Multi-database support". Tóm tắt phần hay chỉnh nhất, đọc trực tiếp trong `api/src/main/resources/application.yaml`:

```yaml
base:
  database:
    type: POSTGRESQL              # SQLITE / POSTGRESQL / MYSQL / SQLSERVER / ORACLE (mặc định SQLITE)
    db-name: example_db           # tên database thật (mặc định "app")
    dbPrefix: dev                 # ghép trước db-name -> "dev_example_db" (mặc định rỗng)
    postgresql:
      host: localhost
      port: 5432
    # mysql / sqlserver / oracle / sqlite: xem base/README.md — chỉ có tác dụng khi `type` chọn đúng engine đó
```

**Lưu ý quan trọng đang áp dụng thật (dev tự kiểm tra khi debug DB):** `dbPrefix: dev` đang được set trong file → tên database thật app đang dùng là **`dev_example_db`**, không phải `example_db`. Nếu vào psql/DBeaver mà connect thẳng `example_db` sẽ không thấy bảng nào.

Đổi engine: set `base.database.type` (hoặc biến môi trường `BASE_DATABASE_TYPE`) sang `MYSQL`/`SQLSERVER`/`ORACLE`/`SQLITE`. DB đích sẽ được tự tạo nếu chưa tồn tại (Postgres/MySQL/SQL Server) — riêng Oracle không tự tạo PDB, phải có sẵn (mặc định `XEPDB1` của Oracle XE là đủ dùng cho dev).

## 6. Migration SQL (Flyway)

- Vị trí: `api-service/database/<engine>/Vn__<mo_ta>.sql` — **5 engine luôn phải viết song song** khi thêm bảng mới (không chỉ viết cho Postgres rồi bỏ quên MySQL/SQL Server/Oracle/SQLite), vì `type` trong `application.yaml` có thể đổi sang engine khác bất cứ lúc nào.
- Không cần `IF NOT EXISTS` — Flyway tự đảm bảo 1 script chỉ chạy đúng 1 lần qua bảng `flyway_schema_history`, thêm guard này chỉ làm SQL Server/Oracle phức tạp hoá không cần thiết.
- Cột kiểu text nên để **nullable** (không `NOT NULL`) trừ khi thực sự bắt buộc — Oracle coi chuỗi rỗng `''` là `NULL`, đặt `NOT NULL` dễ vỡ khi service ghi giá trị rỗng hợp lệ.
- Cột tự tăng khai khác nhau theo engine — xem `V1__init.sql`/`V2__example_entities.sql` có sẵn trong từng thư mục làm mẫu: SQLite `AUTOINCREMENT`, Postgres `GENERATED ALWAYS AS IDENTITY`, MySQL `AUTO_INCREMENT`, SQL Server `IDENTITY(1,1)`, Oracle `GENERATED BY DEFAULT AS IDENTITY` (chú ý **`BY DEFAULT`, không phải `ALWAYS`** — Oracle insert xong mới `SELECT MAX(id)` lấy id, không insert-trả-id trong 1 câu lệnh như 4 engine kia).

## 7. Build & chạy

```bash
cd api-service
./gradlew :api:bootRun          # chạy app (cwd = api-service/, xem mục 2)
./gradlew test                   # chạy test (useJUnitPlatform() đã bật ở base + api)
./gradlew :base:compileJava      # build riêng base khi chỉ sửa base
```

Swagger UI: `/api.html` (đường dẫn cấu hình ở `springdoc.swagger-ui.path`).

## 8. Cạm bẫy đã gặp — đọc trước khi debug

- **`base/application.yaml` gần như không dùng.** `base` và `api` đều có file `application.yaml` trùng tên trên classpath — Spring **không gộp** (merge) 2 file, chỉ 1 file thắng, và thực tế `api/src/main/resources/application.yaml` luôn thắng. Cần đổi cấu hình chung thì sửa ở `api/application.yaml`, không phải `base/`.
- **`base/src/main/resources/logback-spring.xml` có sẵn** → khi có file `logback-spring.xml`/`logback.xml` riêng, Spring Boot **bỏ qua hoàn toàn** `logging.pattern.console/file` khai trong `application.yaml`. Muốn đổi pattern log, sửa `logback-spring.xml`, không sửa yaml.
- **Property cấu hình đa hệ CSDL phải lồng đúng cấp theo engine** — ví dụ `base.database.postgresql.host`, không phải `base.database.host`. Spring không báo lỗi gì nếu đặt sai cấp, chỉ âm thầm không bind được, provider dùng giá trị mặc định trong code. Khi nghi ngờ 1 khối yaml sai vị trí, đối chiếu lại đúng field `@Value`/`@ConfigurationProperties` trong provider Java tương ứng ở `base/db/`.
- **Spring Boot 4 đổi package của một số autoconfig quen thuộc** — nếu gặp lỗi "package does not exist" ở 1 annotation từng quen (`@AutoConfigureMockMvc`, `WebMvcAutoConfiguration`, `DataSourceProperties`...), khả năng cao package đã đổi ở Boot 4 (`org.springframework.boot.webmvc.*`, `org.springframework.boot.jdbc.autoconfigure.*`...) — tra lại docs.spring.io theo đúng version thay vì đoán theo kiến thức cũ.
- **Jackson 3**: package đổi từ `com.fasterxml.jackson.*` sang `tools.jackson.*` (trừ `jackson-annotations`); `JacksonException` giờ là unchecked exception, không còn kế thừa `IOException`.
- **`useJUnitPlatform()` không tự động bật** chỉ vì có `junit-platform-launcher` trên classpath — phải khai rõ `tasks.named('test') { useJUnitPlatform() }` trong `build.gradle` (đã bật ở `base` và `api`).
- **Oracle**: `InsertExecutor` không lấy được id vừa insert trong 1 lượt gọi như 4 engine kia — phải `INSERT` rồi `SELECT MAX(id)` riêng, không an toàn tuyệt đối nếu nhiều request insert đồng thời vào cùng 1 bảng (bảng ít ghi đồng thời thì ổn — cấu hình, danh mục...).
- **`.idea/gradle.xml`**: lỗi Gradle "No cached version ... available for offline mode" khi thêm dependency mới không hẳn do khai sai — kiểm tra `offlineMode` trong `.idea/gradle.xml` trước (cài đặt riêng của IntelliJ, không phải setting Gradle chuẩn).
- **Module Gradle `:api` vs package Java `example`**: từ đợt đổi tên này, tên module (`api`) và tên package nghiệp vụ bên trong (`app.example`) không còn khớp nhau nữa — khi đọc/tìm code, nhớ package vẫn là `vn.org.thn.service.app.example.*`, không phải `app.api.*`.

## 9. Tài liệu liên quan

- `../base/README.md` — API đầy đủ của thư viện `base` (ORM/query DSL, entity/repository, error handling, đa hệ CSDL, i18n, Javadoc coverage).
