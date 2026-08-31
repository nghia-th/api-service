# Task 4 — Ngân hàng câu hỏi (Question / Choice)

Phụ thuộc: Task 3 (Lesson đã có).

## Entity liên quan

`Question` (`lessonId`, `content`, `knowledgeTag` optional), `Choice` (`questionId`, `content`, `correct`) — xem `../01-thiet-ke-tong-the.md` mục 2. 1 Question có N Choice (tạo/sửa/xoá cùng lúc với Question, không có API Choice độc lập).

## API (role Parent, base path `/api/parent/questions`)

### `POST /api/parent/questions`
Request:
```
{
  lessonId,
  content,
  knowledgeTag?,           // optional, tự do
  choices: [
    { content, correct },  // tối thiểu 2 phần tử
    ...
  ]
}
```
Rule:
- `lessonId` phải thuộc đúng Parent hiện tại (check qua `lesson.subject.parentId`).
- `choices` tối thiểu 2 phần tử, và **đúng 1** phần tử có `correct = true` — không phải 0, không phải nhiều hơn 1. Vi phạm → `BusinessException(QuizErrorCode.QUESTION_MUST_HAVE_ONE_CORRECT_CHOICE)`.
- Tạo Question và toàn bộ Choice con trong 1 transaction (Question trước để lấy `id`, rồi insert từng Choice với `questionId` đó).

### `PUT /api/parent/questions/{id}`
Request: giống `POST` (thay toàn bộ nội dung câu hỏi + choices — cách đơn giản nhất cho v1 là xoá hết Choice cũ rồi insert lại theo request mới, thay vì diff từng Choice).
Rule: check ownership qua `lesson.subject.parentId`, cùng rule "đúng 1 choice correct" như trên.

### `GET /api/parent/questions?lessonId=`
Response: danh sách Question thuộc 1 Lesson, mỗi Question kèm theo danh sách Choice đầy đủ (kể cả cờ `correct` — vì đây là API cho Parent, khác hẳn API cho Student ở task 6 sẽ **không** lộ đáp án đúng).

### `GET /api/parent/questions/{id}`
Cùng rule ownership.

### `DELETE /api/parent/questions/{id}`
Rule: chặn xoá nếu Question đã từng xuất hiện trong 1 `TestQuestion` (đã được dùng trong bài kiểm tra nào đó, kể cả bài đã hoàn thành) — tránh phá vỡ lịch sử kết quả cũ đang tham chiếu tới Question này. Báo lỗi rõ ràng thay vì cascade xoá.

## Acceptance criteria

- Tạo câu hỏi với 0 hoặc 2 choice `correct=true` → lỗi rõ, không lưu.
- Tạo câu hỏi chỉ có 1 choice → lỗi (tối thiểu 2).
- `knowledgeTag` để trống vẫn tạo được bình thường (optional thật, không bắt buộc).
- Không thể tạo/sửa câu hỏi trên `lessonId` không thuộc Parent hiện tại.
- Xoá câu hỏi đã dùng trong 1 bài kiểm tra đã giao → bị chặn, có lý do rõ ràng.
