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


## Trạng thái: ĐÃ CODE (task 6, code qua đêm - anh review lại)

Code tại `api/src/main/java/vn/org/thn/service/app/quiz/{entity,repository,dto,service,api}` — `Attempt`/`AttemptAnswer` entity, `AttemptRepository`/`AttemptAnswerRepository`, `StudentTestSummaryResponse`/`StudentChoiceResponse` (**không có field `correct`** - điểm quan trọng nhất của task này)/`StudentQuestionResponse`/`StartAttemptResponse`/`AnswerItem`/`AnswerRequest`/`SubmitAttemptResponse`, `StudentAttemptService`, `StudentAttemptApi` (base path `/api/student`, tự động được `JwtAuthFilter` yêu cầu token role STUDENT nhờ prefix có sẵn từ task 1, không cần cấu hình gì thêm). Migration `database/<engine>/V6__attempt_attempt_answer.sql` (5 engine). Bổ sung `QuizErrorCode.QUIZ_010` (attempt đã nộp rồi).

**Quyết định/giả định khi code:**

- **`start` idempotent đúng theo spec**: query `Attempt` theo `testId` trước, có rồi thì trả lại, không tạo mới — đã kiểm tra kỹ response của `start` không map thẳng entity `Choice` (dùng `StudentChoiceResponse` riêng, chỉ có `choiceId`+`content`) — đúng cảnh báo "lỗi dễ mắc nhất" trong acceptance criteria.
- **Câu chưa trả lời khi nộp bài**: vẫn tạo 1 dòng `AttemptAnswer` với `choiceId = null`, `correct = false` — để task 7 group theo tag không bị thiếu câu hỏi nào.
- **Bổ sung ngoài spec (tự quyết, đề nghị anh xem lại)**: khi lưu answer (`POST .../answers`), thêm 2 check không có trong spec gốc: (1) `questionId` phải thuộc đúng `testId` của attempt, (2) `choiceId` phải thuộc đúng `questionId` đó — tránh học sinh gửi `choiceId` "mượn" từ câu khác (biết trước đáp án đúng của câu khác rồi gán bừa vào câu đang làm) để gian lận điểm. Trả lỗi `COMMON_002 INVALID_PARAMETER` nếu vi phạm. Đây là bổ sung an toàn dữ liệu hợp lý nhưng **không được spec yêu cầu tường minh, anh xem có cần không**.
- **`submit` chỉ chạy 1 lần**: check `submittedAt != null` chặn ngay từ đầu (áp dụng chung cho cả `submit` và `POST answers`, đúng cả 2 acceptance criteria "nộp 2 lần" và "sửa đáp án sau khi nộp").
- **`Test.status = COMPLETED`** set trong cùng transaction với `submit`, load entity `Test` có sẵn rồi sửa tại chỗ (không tạo object mới) — tránh đúng bug audit-field đã bắt được ở task 4.

Lưu ý: không build/compile thử được — đã kiểm tra brace/paren balance, `git status` sạch.

## Review sáng 01/09 (trước khi làm UI)

Review lại kỹ nhất ở đây vì đây là phần rủi ro cao nhất (dữ liệu học sinh thấy trực tiếp): xác nhận `StudentChoiceResponse` không có field `correct` và không có đường nào từ `/api/student/**` trả về `ChoiceResponse` (bản dành cho Parent, có `correct`) — học sinh không thể thấy đáp án đúng trước khi nộp bài. `start()` idempotent đúng (query trước, không tạo Attempt trùng). 2 check chống gian lận ở `saveAnswers()` (questionId thuộc đúng test, choiceId thuộc đúng questionId) đều có và đúng phạm vi. `submit()` chấm đúng cả câu chưa trả lời, chặn nộp 2 lần, set `Test.status=COMPLETED` bằng cách load-sửa-tại-chỗ (không tái phạm bug audit-field-overwrite). Không tìm thấy lỗ hổng ownership nào (mọi chỗ đều lấy id từ `CurrentUser`/JWT, không tin id truyền từ client). Không tìm thấy bug nào khác.
