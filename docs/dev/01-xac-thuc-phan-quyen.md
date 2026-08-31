# Task 1 — Xác thực & phân quyền (Auth)

**ĐÃ CODE (2026-08-31)** — xem code ở `api/src/main/java/vn/org/thn/service/app/quiz/{entity,dto,repository,security,exception,service,api,config}`, migration `database/<engine>/V2__parent_student.sql`. Chưa build/test thực tế (máy dev không có mạng để tải Gradle lúc code xong — cần build lại thủ công để xác nhận compile OK). Lưu ý khác so với mô tả gốc bên dưới:
- JWT dùng thư viện `jjwt` bản `0.13.0`, JSON backend là `jjwt-gson` (không phải `jjwt-jackson`) — vì `base` đã dùng Jackson 3 (`tools.jackson.*`), còn `jjwt-jackson` vẫn cần Jackson 2 (`com.fasterxml.jackson.databind`), dùng chung sẽ tạo 2 stack JSON không liên quan trong cùng app. Gson tách biệt hoàn toàn, không xung đột — xem javadoc `security/JwtUtil.java`.
- Migration đặt tên `V2__parent_student.sql` (không phải "V1 mới") — vì `V1__init.sql` hiện tại chứa bảng `translate` là hạ tầng i18n thật của `base` (không phải demo như ghi chú cũ suy đoán), nên giữ nguyên, bảng Parent/Student nối tiếp thành V2.
- **Cập nhật 2026-08-31 (sau khi anh hỏi lại):** đổi từ validate tay (`requireNonBlank` trong `AuthService`) sang Bean Validation chuẩn của Spring - thêm `spring-boot-starter-validation`, `@NotBlank`/`@Email`/`@Size(min=6)` (giữ nguyên min 6, anh xác nhận) trên request DTO, `@Valid` trên `@RequestBody` trong `AuthApi`. Đồng thời thêm `@Schema` (OpenAPI, package `io.swagger.v3.oas.annotations.media.Schema`) cho từng field của 3 request DTO - theo đúng convention đã có sẵn ở `base` (`LanguageRequest.java`), không tự chế convention mới. Response DTO (`ParentResponse`/`StudentResponse`/...) chưa thêm `@Schema` - anh chỉ yêu cầu request body.
- **Cập nhật 2026-08-31 (lần 2, anh yêu cầu ghi API chuẩn OpenAPI):** thêm `@Tag` (class-level), `@Operation` (summary + description) và `@ApiResponses` (200 + các mã lỗi cụ thể per-endpoint, ví dụ 409 QUIZ_002 EMAIL_TAKEN cho register, 401 QUIZ_004 INVALID_CREDENTIALS cho 2 endpoint login) vào `AuthApi.java`. Vì `io.swagger.v3.oas.annotations.responses.ApiResponse` trùng tên class với `vn.org.thn.service.base.response.ApiResponse` (response envelope của chính dự án) nên annotation Swagger phải viết full-qualified (`@io.swagger.v3.oas.annotations.responses.ApiResponse(...)`) thay vì import bình thường - tránh xung đột tên.
- **Lưu ý build.gradle quan trọng:** `springdoc-openapi-starter-webmvc-ui` phải khai báo lại trong `api/build.gradle` (dùng `compileOnly`, không phải `implementation`) dù `base` đã có sẵn dependency này - vì Gradle `implementation` không lộ ra classpath biên dịch của module phụ thuộc (`api` phụ thuộc `base` qua `implementation project(':base')`), chỉ lộ ra ở runtime. Đây là lý do `mybatis-spring-boot-starter` cũng đã được khai báo lại tương tự từ trước. Nếu thiếu dòng này, code dùng `@Schema`/`@Operation`/`@Tag` sẽ không biên dịch được dù `base` "đã có" thư viện đó.
- **Cập nhật 2026-08-31 (lần 3, anh nhắc):** toàn bộ nội dung `@Schema`/`@Operation`/`@Tag`/`@ApiResponses` lúc đầu viết bằng tiếng Việt không dấu (vd "Ho ten phu huynh") - sai quy tắc "mọi comment/code bằng tiếng Anh" của dự án. Đã dịch lại toàn bộ sang tiếng Anh trong 3 request DTO + `AuthApi.java`. Chỉ tài liệu `docs/*.md` (file markdown, không phải code) mới dùng tiếng Việt.

**Làm trước tiên** — mọi task sau đều phụ thuộc vào "current user" lấy được từ đây. Xem thiết kế tổng thể ở `../01-thiet-ke-tong-the.md` mục 3.

## Entity liên quan

`Parent`, `Student` — xem field đầy đủ ở `../01-thiet-ke-tong-the.md` mục 2. Cả 2 đều có `password` lưu dạng hash (BCrypt), **không bao giờ** trả `password` trong bất kỳ response nào (dùng DTO response riêng, không trả thẳng entity cho API liên quan tới Parent/Student).

## Việc cần làm (hạ tầng mới, `base` hiện chưa có)

1. Thêm dependency mã hoá password: `org.springframework.security:spring-security-crypto` (chỉ lấy `BCryptPasswordEncoder`, không cần add toàn bộ Spring Security).
2. Thêm dependency JWT: ví dụ `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` (kiểm tra tương thích Jackson 3 trước khi chọn version — `base` đã chuyển sang Jackson 3 `tools.jackson.*`, xem `claude/base-module-status.md` mục "Lịch sử debug quan trọng" — jjwt bản cũ có thể còn phụ thuộc Jackson 2, cần verify).
3. `security/JwtUtil.java` — sinh token (payload: `userId`, `role` = `PARENT`/`STUDENT`, thời hạn hết hạn), verify + parse token.
4. `security/JwtAuthFilter.java` — theo đúng mẫu `RequestContextFilter` đã có trong `base` (đọc header `Authorization: Bearer <token>`, verify, lưu `userId`/`role` vào request attribute hoặc MDC để tầng Service đọc lại — không dùng Spring Security filter chain đầy đủ, giữ đơn giản đúng tinh thần `base`).
5. `security/CurrentUser.java` (hoặc method tĩnh tương tự `BaseCtl.getClientIp()`) — helper đọc `userId`/`role` hiện tại từ request attribute, dùng trong mọi `*Service` để lọc dữ liệu theo `parentId`/`studentId`.
6. `exception/QuizErrorCode.java` — thêm `UNAUTHORIZED` (token thiếu/hết hạn/sai), `EMAIL_TAKEN`, `USERNAME_TAKEN`, `INVALID_CREDENTIALS`.

## API

### `POST /api/auth/parent/register`
Request: `{ fullName, email, password, phone? }`
Response: thông tin Parent (không kèm password) + có thể trả luôn token (auto-login sau đăng ký) hoặc yêu cầu login riêng — chọn **auto-login** cho đơn giản.
Rule: `email` phải unique → nếu trùng, `BusinessException(QuizErrorCode.EMAIL_TAKEN)`. Hash password bằng BCrypt trước khi lưu.

### `POST /api/auth/parent/login`
Request: `{ email, password }`
Response: `{ token, parent: {...} }`
Rule: email không tồn tại hoặc password sai → `BusinessException(QuizErrorCode.INVALID_CREDENTIALS)` (dùng chung 1 message cho cả 2 trường hợp, không lộ "email không tồn tại" — tránh dò email).

### `POST /api/auth/student/login`
Request: `{ username, password }`
Response: `{ token, student: {...} }`
Rule: tương tự, dùng chung `INVALID_CREDENTIALS`. Không có API "student register" — Student chỉ được tạo qua `POST /api/parent/students` (task 2).

## Áp dụng cho mọi API sau này

Mọi `*Api.java` dưới `/api/parent/**`/`/api/student/**` (trừ 3 endpoint auth ở trên) đều phải đi qua `JwtAuthFilter`. Mọi `*Service.java` khi query/update dữ liệu phải lọc theo `CurrentUser` — ví dụ `StudentService.list()` chỉ trả Student có `parentId = currentUser.userId()`, không phải `findAll()` trần.

## Acceptance criteria

- Đăng ký Parent với email đã tồn tại → lỗi rõ ràng, không tạo trùng.
- Đăng nhập sai password → lỗi, không lộ thông tin email/username có tồn tại hay không.
- Gọi API `/api/parent/**` không kèm token → 401, không crash 500.
- Gọi API `/api/parent/**` với token của Student → 401/403 (role không khớp).
- Token hết hạn → 401 rõ ràng, không phải lỗi chung chung.
