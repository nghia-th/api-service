# Task 2 — Quản lý hồ sơ con (Student)

Phụ thuộc: Task 1 (auth) đã xong — mọi API ở đây yêu cầu token Parent hợp lệ.

## Entity liên quan

`Student` — xem field ở `../01-thiet-ke-tong-the.md` mục 2 (`parentId`, `fullName`, `grade`, `username`, `password`).

## API (role Parent, base path `/api/parent/students`)

### `POST /api/parent/students`
Request: `{ fullName, grade, username, password }`
Response: Student vừa tạo (không kèm password).
Rule: `parentId` lấy từ `CurrentUser`, không nhận từ request body. `username` phải unique **toàn hệ thống** (không chỉ trong phạm vi 1 Parent) — nếu trùng, `BusinessException(QuizErrorCode.USERNAME_TAKEN)`. Hash `password` bằng BCrypt trước khi lưu.

### `PUT /api/parent/students/{id}`
Request: `{ fullName?, grade?, username?, password? }` (password optional — chỉ đổi khi phụ huynh muốn reset)
Rule: `id` phải thuộc đúng Parent hiện tại (`student.parentId == currentUser.userId()`), nếu không → `BusinessException(FORBIDDEN)`. Nếu đổi `username`, vẫn phải check unique toàn hệ thống (trừ chính bản thân Student đang sửa).

### `GET /api/parent/students`
Response: danh sách Student thuộc Parent hiện tại (không phân trang ở v1, số con trong 1 gia đình luôn nhỏ).

### `GET /api/parent/students/{id}`
Rule: check ownership như trên.

### `DELETE /api/parent/students/{id}`
Rule: check ownership. Cân nhắc: nếu Student đã có Test/Attempt gắn vào, xoá cứng sẽ mất dữ liệu lịch sử kiểm tra — **khuyến nghị**: chặn xoá nếu đã có Attempt (`BusinessException` báo rõ lý do), hoặc cho xoá tự do ở v1 và chấp nhận mất lịch sử (ghi rõ giả định này khi code, không tự quyết ngầm).

## Acceptance criteria

- Parent A không thể sửa/xoá/xem Student thuộc Parent B (kể cả biết `id`) — test riêng case này, không chỉ test happy path.
- Tạo Student với `username` đã tồn tại (kể cả của Parent khác) → lỗi rõ ràng.
- `GET /api/parent/students` chỉ trả đúng con của Parent đang đăng nhập, không lộ danh sách toàn hệ thống.


## Trạng thái: ĐÃ CODE (task 2)

Code tại `api/src/main/java/vn/org/thn/service/app/quiz/{dto,service,api}` — `StudentCreateRequest`/`StudentUpdateRequest` (dto), `StudentService`, `StudentApi`. Tái dùng `StudentResponse` có sẵn từ task 1 cho mọi response (create/update/get/list đều trả `StudentResponse`, ẩn `password`).

Các quyết định/giả định khi code:

- **DELETE**: xác nhận qua AskUserQuestion với anh Nghĩa — chọn **xoá cứng ngay** (v1), bổ sung chặn xoá-khi-đã-có-Attempt sau khi task 6 (Attempt entity) được code. Có ghi comment rõ giả định này trong `StudentService.delete`.
- **Validate**: dùng Spring Bean Validation (`@Valid` + `@NotBlank`/`@Size`) trên 2 DTO, đúng chuẩn đã chốt từ task 1 — không tự check blank thủ công trong service nữa.
- **OpenAPI**: `StudentApi` có đủ `@Tag`/`@Operation`/`@ApiResponses` (tiếng Anh) như `AuthApi`; 2 DTO có `@Schema` trên từng field.
- **Update là partial update**: field nào null (và với `fullName`/`grade`/`username` — cả blank) thì giữ nguyên giá trị cũ; đổi `username` chỉ khi khác username hiện tại (tránh check-trùng-với-chính-mình không cần thiết); `password` là ngoại lệ — `@Size(min=6)` đã chặn blank ở tầng DTO nên trong service chỉ cần check `!= null`.
- **Check unique username**: 1 method `ensureUsernameAvailable(username, excludeStudentId)` dùng chung cho cả create (`excludeStudentId = null`) và update, tận dụng đặc tính `.ne()` no-op khi value null của `BaseConditionBuilder`.
- **Ownership check**: helper `getOwnedOrThrow(id, parentId)` — không tìm thấy → `CommonErrorCode.NOT_FOUND`; tìm thấy nhưng khác `parentId` → `CommonErrorCode.FORBIDDEN` (đúng theo acceptance criteria, không lộ thông tin khác giữa 2 trường hợp).
- `createdBy`/`updatedBy` lưu dạng `"parent:" + parentId"` (theo đúng convention `role:id` đã dùng ở task 1 cho `Parent.createdBy = email`, ở đây Student không có "tên đăng nhập ở dạng liên hệ" như email nên dùng parentId).

Lưu ý: session code này không có mạng ở máy — **chưa build/compile thử được**, anh Nghĩa cần tự chạy `./gradlew :api:compileJava` (hoặc build IDE) để xác nhận trước khi merge.
