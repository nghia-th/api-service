# Task 4 — Ngân hàng câu hỏi (Question / Choice)

Phụ thuộc: Task 3 (Lesson đã có). Có 2 cách tạo câu hỏi: **nhập tay** (API `POST`/`PUT` bên dưới) và **import file Excel/CSV theo mẫu** (mục riêng cuối file — bổ sung 2026-08-31) — cả 2 cùng tạo ra `Question`/`Choice`, cùng chịu chung mọi rule bên dưới.

## Entity liên quan

`Question` (`lessonId`, `content`, `knowledgeTag` optional), `Choice` (`questionId`, `content`, `correct`) — xem `../01-thiet-ke-tong-the.md` mục 2. 1 Question có N Choice (tạo/sửa/xoá cùng lúc với Question, không có API Choice độc lập).

## API nhập tay (role Parent, base path `/api/parent/questions`)

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

## Import câu hỏi từ file Excel/CSV theo mẫu (bổ sung 2026-08-31)

**Quyết định phạm vi (đã hỏi lại trước khi thêm vào task này):** import bằng **file Excel/CSV có cấu trúc cột cố định** (phụ huynh tải mẫu về, điền, upload lại) — **KHÔNG** phải OCR/nhận diện từ ảnh hay trích xuất từ Word/PDF viết tự do (phần đó vẫn đẩy sang giai đoạn sau, xem `../00-tong-quan-san-pham.md`). Đọc file có cấu trúc cố định là bài toán rủi ro kỹ thuật thấp — dùng thư viện đọc Excel/CSV chuẩn (ví dụ Apache POI cho `.xlsx`, hoặc Apache Commons CSV cho `.csv`), không cần AI/OCR.

### Cấu trúc file mẫu (cột cố định)

| Cột | Tên cột | Bắt buộc | Ghi chú |
|---|---|---|---|
| A | Câu hỏi | Có | nội dung câu hỏi |
| B | Lựa chọn 1 | Có | |
| C | Lựa chọn 2 | Có | |
| D | Lựa chọn 3 | Không | để trống nếu câu hỏi chỉ có 2-3 lựa chọn |
| E | Lựa chọn 4 | Không | |
| F | Đáp án đúng | Có | ghi số thứ tự lựa chọn đúng: `1`/`2`/`3`/`4` (ứng với cột B-E) — **không** ghi lại nội dung lựa chọn, để tránh lỗi gõ sai chính tả không khớp |
| G | Tag nhóm kiến thức | Không | tự do, để trống nếu không phân loại |

Hàng 1 là header (tên cột), dữ liệu từ hàng 2 trở đi. Toàn bộ câu hỏi trong 1 lần import đều gắn vào **cùng 1 Lesson** (chọn trước khi import, không hỗ trợ nhiều Lesson trong 1 file ở v1).

### API

#### `GET /api/parent/questions/import-template?format=xlsx`
Response: file Excel (hoặc CSV nếu `format=csv`) mẫu để tải về — header đúng 7 cột ở trên, kèm 1 hàng ví dụ minh hoạ (đánh dấu rõ là dòng mẫu, phụ huynh xoá đi trước khi điền thật hoặc hệ thống tự bỏ qua dòng ví dụ khi import — chọn 1 trong 2 cách, ghi rõ trong code).

#### `POST /api/parent/questions/import`
Request: `multipart/form-data` — field `file` (Excel/CSV theo mẫu) + field `lessonId`.
Rule:
- `lessonId` phải thuộc đúng Parent hiện tại (cùng rule như `POST /api/parent/questions`).
- Đọc **best-effort theo từng dòng** — 1 dòng lỗi không chặn các dòng hợp lệ khác trong cùng file (khác hẳn transaction "tất cả hoặc không gì" của `POST` thường).
- 1 dòng bị coi là lỗi khi: thiếu "Câu hỏi" hoặc thiếu cả 2 lựa chọn bắt buộc (cột B, C), hoặc "Đáp án đúng" không phải số 1-4, hoặc "Đáp án đúng" trỏ tới 1 cột lựa chọn đang để trống (ví dụ ghi `4` nhưng cột E trống).
- Giới hạn số dòng tối đa 1 lần import — đề xuất **200 dòng**, cần dev xác nhận con số cụ thể lúc code (không phải yêu cầu cứng từ phân tích, chỉ là giả định hợp lý để tránh request quá lớn).
- File rỗng hoặc không đọc được (sai định dạng hoàn toàn, ví dụ upload nhầm file `.docx`) → lỗi ngay, không tạo Question nào.

Response:
```
{
  totalRows,
  successCount,
  errors: [
    { rowNumber, reason }   // rowNumber tính theo dòng trong file (kể cả header), reason là câu mô tả lỗi rõ ràng bằng tiếng Việt
  ]
}
```

## Acceptance criteria

- Tạo câu hỏi với 0 hoặc 2 choice `correct=true` → lỗi rõ, không lưu.
- Tạo câu hỏi chỉ có 1 choice → lỗi (tối thiểu 2).
- `knowledgeTag` để trống vẫn tạo được bình thường (optional thật, không bắt buộc).
- Không thể tạo/sửa câu hỏi trên `lessonId` không thuộc Parent hiện tại.
- Xoá câu hỏi đã dùng trong 1 bài kiểm tra đã giao → bị chặn, có lý do rõ ràng.
- Import file có 10 dòng, trong đó 2 dòng lỗi (ví dụ thiếu lựa chọn) → tạo đúng 8 Question, trả về `errors` ghi rõ dòng nào lỗi và vì sao, không fail toàn bộ file.
- Import với `lessonId` không thuộc Parent hiện tại → lỗi ngay từ đầu, không đọc file.
- Import file sai định dạng hoàn toàn (không parse được) → lỗi rõ ràng, không crash 500.
