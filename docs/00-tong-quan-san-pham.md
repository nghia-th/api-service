# Tổng quan sản phẩm — "Hiểu Bài" (quiz-service)

**Trạng thái:** Đã chốt phạm vi MVP, chưa code. Đây là tài liệu tổng quan cho MVP, viết lại từ tài liệu concept gốc (`Tong_quan_ung_dung_kiem_tra_hieu_bai_hoc_sinh.md`) sau khi đã thu gọn phạm vi qua các vòng trao đổi.

## 1. Bối cảnh & mục đích

Phụ huynh muốn biết con có thực sự hiểu bài vừa học trong sách giáo khoa hay không, nhưng không có thời gian soạn bài kiểm tra, không biết nên hỏi câu gì, và chỉ nhìn điểm số thì không biết con hiểu đến đâu.

**Mục đích cốt lõi:** giúp phụ huynh tạo nhanh 1 bài kiểm tra trắc nghiệm gắn với bài học con vừa học, giao cho con làm, và xem kết quả để biết con nắm bài đến đâu — không chỉ điểm số mà còn biết **sai ở mảng kiến thức nào**.

**3 câu hỏi trung tâm sản phẩm phải trả lời được:**
1. Con đã hiểu bài chưa?
2. Nếu chưa, con đang sai ở phần nào?
3. Phụ huynh nên xem lại/dạy lại phần nào cho con?

## 2. Đối tượng sử dụng

**Phụ huynh (Parent)** — tài khoản chính, tự đăng ký. Có thể: quản lý hồ sơ con (tạo tài khoản, set Lớp), tạo Môn học/Bài học, tạo ngân hàng câu hỏi trắc nghiệm, tạo và giao bài kiểm tra cho con, xem kết quả và lịch sử kiểm tra.

**Học sinh (Student)** — tài khoản do phụ huynh tạo và quản lý (không tự đăng ký), đăng nhập bằng username/password do phụ huynh đặt. Có thể: xem bài được giao, làm bài, nộp bài, xem kết quả (điểm, câu đúng/sai) sau khi nộp.

## 3. Phạm vi MVP v1

**Trong phạm vi:**
- 1 loại câu hỏi duy nhất: trắc nghiệm (nhiều lựa chọn, 1 đáp án đúng).
- Phụ huynh tự nhập tay câu hỏi, gắn với Môn học + Bài học tự đặt tên (không cần danh mục chuẩn hoá sẵn).
- Mỗi câu hỏi có thể gắn 1 tag nhóm kiến thức tự do (ví dụ "Do/Does", "Câu phủ định") — dùng để phân tích kết quả theo mảng kiến thức, không phải hệ phân loại chuẩn hoá.
- Hệ thống tự chấm điểm, phụ huynh xem kết quả tổng điểm + breakdown theo tag nhóm kiến thức.
- Phụ huynh quản lý nhiều hồ sơ con, mỗi con có tài khoản riêng (username/password do phụ huynh đặt) và field Lớp.

**Ngoài phạm vi (để giai đoạn sau):**
- Import câu hỏi qua OCR/chụp ảnh, import file (Word/PDF/Excel).
- Dạng câu hỏi Đúng/Sai và Điền đáp án ngắn.
- Ngân hàng câu hỏi có sẵn theo Lớp → Môn → Chủ đề → Bài học (soạn sẵn trong hệ thống).
- Taxonomy/chuẩn hoá nhóm kiến thức (v1 chỉ là tag tự do).
- Học sinh làm lại 1 bài kiểm tra nhiều lần.

## 4. Luồng chức năng chính

```text
PHỤ HUYNH đăng ký/đăng nhập
        ↓
Tạo hồ sơ con (username/password + Lớp)
        ↓
Tạo Môn học → Bài học
        ↓
Tạo câu hỏi trắc nghiệm (gắn Bài học, tuỳ chọn gắn tag nhóm kiến thức)
        ↓
Tạo Bài kiểm tra (chọn câu hỏi, giao cho 1 con)
        ↓
HỌC SINH đăng nhập → xem bài được giao → làm bài → nộp bài
        ↓
Hệ thống tự chấm điểm
        ↓
PHỤ HUYNH xem kết quả: tổng điểm + câu sai + breakdown theo tag nhóm kiến thức
        ↓
PHỤ HUYNH biết nên dạy lại phần nào cho con
```

## 5. Hướng phát triển sau MVP

Giữ nguyên định hướng của tài liệu concept gốc: Giai đoạn 2 (ngân hàng câu hỏi có sẵn theo lớp/môn/bài), Giai đoạn 3 (chuẩn hoá nhóm kiến thức, gợi ý ôn tập, báo cáo tuần), Giai đoạn 4 (AI hỗ trợ tạo câu hỏi, OCR nhận diện câu hỏi từ ảnh, AI phân tích lỗi), Giai đoạn 5 (bài tập tự động, lộ trình học cá nhân hoá).

## 6. Tài liệu liên quan

- `01-thiet-ke-tong-the.md` — kiến trúc, data model, quy tắc API.
- `dev/` — tài liệu chi tiết theo từng chức năng/task, dùng để code trực tiếp.
