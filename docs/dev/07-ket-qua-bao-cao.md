# Task 7 — Kết quả & báo cáo (Attempt, đọc)

Phụ thuộc: Task 6 (Attempt đã có dữ liệu nộp bài). Đây là task hiện thực hoá đúng "mâu thuẫn cốt lõi" đã phân tích ở `claude/hieu-bai-app-phan-tich.md` — kết quả không chỉ là điểm số mà phải group theo tag nhóm kiến thức.

## Entity liên quan (chỉ đọc, không entity mới)

`Attempt`, `AttemptAnswer`, `Question` (lấy `knowledgeTag`) — xem `../01-thiet-ke-tong-the.md` mục 2.

## API (role Parent, base path `/api/parent`)

### `GET /api/parent/attempts/{id}`
Rule: `attempt.test.parentId` phải là Parent hiện tại.
Response (mô tả cấu trúc, không phải code):
```
{
  attemptId,
  testName,
  studentName,
  correctCount, totalQuestions,
  scorePercent,                 // correctCount / totalQuestions * 100
  submittedAt,
  answers: [
    { questionId, questionContent, chosenChoiceContent, correctChoiceContent, correct, knowledgeTag }
  ],
  byKnowledgeTag: [
    { knowledgeTag, correctCount, totalCount }   // group AttemptAnswer theo Question.knowledgeTag
  ]
}
```
Câu hỏi có `knowledgeTag = null` gom chung vào 1 nhóm hiển thị dạng "Chưa phân loại" trong `byKnowledgeTag`, không loại bỏ khỏi báo cáo.

### `GET /api/parent/students/{studentId}/attempts`
Response: lịch sử kiểm tra của 1 con — danh sách rút gọn (`testName`, `submittedAt`, `correctCount`/`totalQuestions`, không cần chi tiết từng câu — bấm vào mới gọi API chi tiết ở trên). Sắp xếp mới nhất trước.
Rule: `studentId` phải thuộc đúng Parent hiện tại.

## Acceptance criteria

- `byKnowledgeTag` tính đúng khi 1 Test có nhiều câu hỏi cùng tag (ví dụ 3 câu tag "Do/Does", đúng 1 sai 2 → group trả `{ knowledgeTag: "Do/Does", correctCount: 1, totalCount: 3 }`).
- Câu hỏi không gắn tag vẫn xuất hiện trong báo cáo (nhóm "Chưa phân loại"), không bị bỏ sót.
- Chỉ xem được kết quả của con thuộc chính mình, không xem được kết quả của Student thuộc Parent khác dù biết `attemptId`.
- Xem kết quả 1 Attempt chưa nộp (`submittedAt = null`) → nên chặn hoặc trả rõ trạng thái "chưa nộp bài", không trả điểm rác (correctCount mặc định 0 dễ gây hiểu nhầm là "làm sai hết").
