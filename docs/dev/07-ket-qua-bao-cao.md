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


## Trạng thái: ĐÃ CODE (task 7, code qua đêm - anh review lại)

Code tại `api/src/main/java/vn/org/thn/service/app/quiz/{dto,service,api}` — không có entity mới đúng như spec. `AttemptAnswerDetail`/`KnowledgeTagBreakdown`/`AttemptReportResponse`/`StudentAttemptHistoryItem`, `ReportService`, `ReportApi` (base path `/api/parent`, 2 endpoint: `GET /attempts/{id}` và `GET /students/{studentId}/attempts`). Bổ sung `QuizErrorCode.QUIZ_013` (attempt chưa nộp bài).

**Quyết định/giả định khi code:**

- **Attempt chưa nộp → chặn hẳn** (chọn 1 trong 2 phương án spec đưa ra: "chặn hoặc trả rõ trạng thái") — trả lỗi `QUIZ_013 ATTEMPT_NOT_SUBMITTED` (409) thay vì trả 1 response "chưa nộp bài" dạng dữ liệu, nhất quán với cách toàn bộ code xử lý business rule khác (throw + `GlobalExceptionHandler`).
- **`byKnowledgeTag`**: câu không có tag gom vào nhãn `"Chưa phân loại"` — đây là nội dung hiển thị trực tiếp cho phụ huynh (không phải code prose), viết tiếng Việt có dấu theo đúng tinh thần đã áp dụng ở task 4 (xem LANGUAGE NOTE trong `QuestionImportService`), có ghi rõ trong Javadoc của `KnowledgeTagBreakdown` để giải thích tại sao literal này khác quy tắc code tiếng Anh chung.
- **`GET /students/{studentId}/attempts` chỉ trả Attempt đã nộp** (`submittedAt != null`) — **giả định tự thêm, spec không ghi rõ** nhưng hợp lý vì đây là "lịch sử kiểm tra", 1 Attempt đang làm dở không có `submittedAt`/điểm số để hiển thị hợp lý trong danh sách rút gọn. Đã ghi rõ giả định này trong Javadoc `StudentAttemptHistoryItem` để anh review.
- **Filter Test theo cả `studentId` VÀ `parentId`** (không chỉ `studentId`) khi tính lịch sử — phòng thủ thêm để không lộ dữ liệu nếu có bất nhất dữ liệu ở đâu đó, dù về lý thuyết `Test.parentId` luôn khớp `Student.parentId` do `TestService.create` đã check `studentId` ownership lúc tạo.

Lưu ý: không build/compile thử được — đã kiểm tra brace/paren balance, `git status` sạch. **Đây là task cuối cùng trong phạm vi MVP v1 đã lên kế hoạch (task 1-7) — toàn bộ luồng chính (đăng ký/đăng nhập → quản lý con → môn học/bài học → ngân hàng câu hỏi → tạo/giao bài → học sinh làm bài → xem kết quả theo tag kiến thức) đã có code, chưa build/test thật.**

## Review sáng 01/09 (trước khi làm UI)

Review lại: ownership `getAttemptReport` đúng (qua `TestService.getOwnedOrThrow`), chặn đúng khi Attempt chưa nộp (`QUIZ_013`), sắp xếp theo `orderIndex` đúng, nhãn "Chưa phân loại" đúng chuỗi/dấu, `getStudentAttemptHistory` lọc đúng cả `studentId` lẫn `parentId` và chỉ lấy Attempt đã nộp như đã ghi trong Javadoc. `ReportService` hoàn toàn read-only (không gọi `save()` ở đâu) nên không áp dụng bug audit-field-overwrite. Không tìm thấy bug nào khác — đây là task duy nhất trong 7 task không phát hiện gì cần sửa.
