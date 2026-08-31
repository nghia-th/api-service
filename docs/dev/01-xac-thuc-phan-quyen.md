# Task 1 — Xác thực & phân quyền (Auth)

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
