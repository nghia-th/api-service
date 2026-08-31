# Task 3 — Môn học & Bài học (Subject / Lesson)

Phụ thuộc: Task 1 (auth).

## Entity liên quan

`Subject` (`parentId`, `name`), `Lesson` (`subjectId`, `name`) — xem `../01-thiet-ke-tong-the.md` mục 2.

## API — Subject (role Parent, base path `/api/parent/subjects`)

- `POST /api/parent/subjects` — `{ name }`, `parentId` lấy từ `CurrentUser`.
- `PUT /api/parent/subjects/{id}` — `{ name }`, check ownership (`subject.parentId == currentUser.userId()`).
- `GET /api/parent/subjects` — danh sách Subject của Parent hiện tại.
- `GET /api/parent/subjects/{id}`
- `DELETE /api/parent/subjects/{id}` — **rule quan trọng:** chặn xoá nếu Subject còn Lesson con (`BusinessException` báo rõ, không cascade-delete ngầm — xoá cascade dễ mất dữ liệu câu hỏi/kết quả liên quan mà phụ huynh không lường trước).

## API — Lesson (role Parent, base path `/api/parent/lessons`)

- `POST /api/parent/lessons` — `{ subjectId, name }`. Rule: `subjectId` phải thuộc đúng Parent hiện tại (check qua `subject.parentId`), không chỉ check tồn tại.
- `PUT /api/parent/lessons/{id}` — `{ name }`, check ownership qua Subject cha.
- `GET /api/parent/lessons?subjectId=` — danh sách Lesson theo Subject (bắt buộc filter theo `subjectId`, không trả toàn bộ Lesson của Parent trong 1 API để tránh danh sách quá dài khi UI cần chọn theo cây Môn → Bài).
- `GET /api/parent/lessons/{id}`
- `DELETE /api/parent/lessons/{id}` — cùng rule chặn xoá nếu còn Question con.

## Acceptance criteria

- Tạo Lesson với `subjectId` thuộc Parent khác → lỗi, không tạo được (không chỉ check `subjectId` tồn tại mà phải check đúng chủ sở hữu).
- Xoá Subject còn Lesson con → lỗi rõ ràng, không xoá cascade ngầm.
- `GET /api/parent/lessons?subjectId=X` không trả Lesson của Subject khác dù cùng thuộc Parent hiện tại.
