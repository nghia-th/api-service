# Task 5 — Tạo & giao bài kiểm tra (Test)

Phụ thuộc: Task 2 (Student), Task 4 (Question).

## Entity liên quan

`Test` (`parentId`, `studentId`, `name`, `status`), `TestQuestion` (`testId`, `questionId`, `orderIndex`) — xem `../01-thiet-ke-tong-the.md` mục 2. Tạo Test = giao bài luôn (không có bước "giao" tách riêng ở v1 — Test luôn có `studentId` ngay từ lúc tạo, `status = ASSIGNED`).

## API (role Parent, base path `/api/parent/tests`)

### `POST /api/parent/tests`
Request: `{ studentId, name, questionIds: [] }` (thứ tự trong mảng `questionIds` chính là `orderIndex`)
Rule:
- `studentId` phải thuộc đúng Parent hiện tại (check qua `student.parentId`).
- Mọi `questionIds` phải thuộc đúng Parent hiện tại (check qua `question.lesson.subject.parentId`) — không cho ghép câu hỏi của Parent khác vào bài kiểm tra dù có cách nào đó biết được `id`.
- `questionIds` tối thiểu 1 phần tử.
- Set `status = ASSIGNED` ngay khi tạo.
- Tạo `Test` trước để lấy `id`, rồi insert từng `TestQuestion` theo đúng thứ tự trong mảng.

### `GET /api/parent/tests?studentId=`
Response: danh sách Test đã giao cho 1 Student (tuỳ chọn filter theo `studentId`, không bắt buộc — nếu không truyền thì trả tất cả Test của Parent hiện tại).

### `GET /api/parent/tests/{id}`
Response: chi tiết Test kèm danh sách Question đã chọn (đúng thứ tự `orderIndex`), kèm `status`.
Rule: check ownership qua `test.parentId`.

### `DELETE /api/parent/tests/{id}`
Rule: **chỉ cho xoá khi chưa có Attempt nào** (`status = ASSIGNED` và chưa có bản ghi `Attempt` gắn `testId` này) — Test đã có Attempt (dù đang làm dở hay đã nộp) không được xoá, tránh mất lịch sử kết quả. Vi phạm → `BusinessException` báo rõ lý do.

## Acceptance criteria

- Tạo Test với `questionIds` trộn cả câu hỏi của Parent khác → lỗi, không tạo được.
- Tạo Test giao cho Student không thuộc Parent hiện tại → lỗi.
- Test rỗng (`questionIds` rỗng) → lỗi, không tạo được bài kiểm tra không có câu hỏi nào.
- Xoá Test đã có Attempt (kể cả đang làm dở, chưa nộp) → bị chặn.
