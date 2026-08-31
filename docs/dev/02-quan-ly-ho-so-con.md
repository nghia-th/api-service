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
