# Task 6 — Học sinh làm bài (Attempt)

Phụ thuộc: Task 5 (Test đã được giao).

## Entity liên quan

`Attempt` (`testId`, `studentId`, `startedAt`, `submittedAt`, `correctCount`, `totalQuestions`), `AttemptAnswer` (`attemptId`, `questionId`, `choiceId`, `correct`) — xem `../01-thiet-ke-tong-the.md` mục 2.

**Giả định MVP v1 (ghi rõ để không tự hiểu khác khi code):** 1 Test chỉ cho phép **đúng 1 Attempt** — không hỗ trợ học sinh làm lại. Nếu `testId` đã có Attempt (kể cả đang làm dở), gọi `start` lần 2 phải trả về đúng Attempt cũ (idempotent), không tạo Attempt mới.

## API (role Student, base path `/api/student`)

### `GET /api/student/tests`
Response: danh sách Test có `studentId = currentUser.userId()`, kèm `status` (`ASSIGNED`/`COMPLETED`) để Student biết bài nào đã làm/chưa làm.

### `POST /api/student/tests/{testId}/start`
Rule: `testId` phải thuộc đúng Student hiện tại. Nếu chưa có Attempt cho Test này → tạo mới (`startedAt = now`, `totalQuestions` = số Question trong Test). Nếu đã có → trả lại Attempt cũ (không tạo trùng — xem giả định ở trên).
Response: `{ attemptId, questions: [ { questionId, content, choices: [{choiceId, content}] } ] }` — **quan trọng: KHÔNG trả field `correct` của Choice trong response này** (khác hẳn API `GET /api/parent/questions` ở task 4) — học sinh không được thấy đáp án đúng trước khi nộp bài.

### `POST /api/student/attempts/{attemptId}/answers`
Request: `{ answers: [ { questionId, choiceId }, ... ] }` — cho phép gửi từng câu một lần hoặc gửi hết 1 lần (client tự quyết, API chấp nhận cả 2 kiểu vì cùng logic upsert theo `questionId`).
Rule: `attemptId` phải thuộc đúng Student hiện tại, và `submittedAt` phải còn null (đã nộp rồi thì không cho sửa đáp án nữa). Lưu/ghi đè `AttemptAnswer` theo `questionId` (chưa tính `correct` ở bước này — chỉ tính khi `submit`).

### `POST /api/student/attempts/{attemptId}/submit`
Rule: `attemptId` thuộc đúng Student hiện tại, `submittedAt` phải còn null (chặn nộp 2 lần). Với mỗi `AttemptAnswer` đã lưu (và mỗi Question chưa có answer thì coi như sai/bỏ trống), so `choiceId` với Choice có `correct=true` của Question đó để set `correct` cho từng `AttemptAnswer`. Set `Attempt.submittedAt = now`, `Attempt.correctCount` = tổng số answer đúng. Set `Test.status = COMPLETED`.
Response: kết quả cơ bản cho Student — điểm số + câu đúng/sai (không cần breakdown theo tag nhóm kiến thức ở đây, phần đó dành cho Parent xem — task 7).

## Acceptance criteria

- Gọi `start` 2 lần liên tiếp trên cùng `testId` → trả về cùng 1 `attemptId`, không tạo Attempt thứ 2.
- Response của `start` không chứa field nào tiết lộ đáp án đúng — kiểm tra kỹ vì đây là lỗi dễ mắc nhất (trả thẳng entity `Choice` có field `correct` mà quên map sang DTO riêng).
- Nộp bài 2 lần (`submit` gọi lại sau khi đã `submittedAt` khác null) → lỗi, không tính điểm lại.
- Gửi `answers` sau khi đã `submit` → lỗi, không cho sửa đáp án bài đã nộp.
- Student A không gọi được API trên `attemptId`/`testId` của Student B.
