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


## Trạng thái: ĐÃ CODE (task 3)

Code tại `api/src/main/java/vn/org/thn/service/app/quiz/{entity,repository,dto,service,api}` — `Subject`/`Lesson` entity, `SubjectRepository`/`LessonRepository`, `SubjectRequest` (dùng chung create/update) + `SubjectResponse`, `LessonCreateRequest`/`LessonUpdateRequest` (2 DTO riêng vì create cần `subjectId` còn update chỉ đổi `name`) + `LessonResponse`, `SubjectService`/`LessonService`, `SubjectApi`/`LessonApi`. Migration `database/<engine>/V3__subject_lesson.sql` (5 engine), nối tiếp `V2`.

Các quyết định/giả định khi code:

- **`Lesson` không có `parentId` riêng** — đúng theo data model đã chốt (`01-thiet-ke-tong-the.md`), ownership của Lesson luôn resolve gián tiếp qua `Subject.parentId`. `LessonService` gọi lại `SubjectService.getOwnedOrThrow(subjectId, parentId)` (package-private) thay vì tự viết lại logic not-found/forbidden lần 2.
- **DELETE Subject:** chặn đúng theo spec — nếu còn `Lesson` con thì trả lỗi `QUIZ_005 SUBJECT_HAS_LESSONS` (409), không cascade-delete ngầm. Rule này làm được đầy đủ ngay vì `Lesson` đã tồn tại (tạo trong cùng task này).
- **DELETE Lesson:** spec yêu cầu chặn nếu còn `Question` con, nhưng `Question` chưa tồn tại (task 4 chưa code) nên **chưa có gì để check** — áp dụng đúng policy đã dùng ở task 2 (`StudentService.delete`): xoá cứng ngay ở v1, có comment rõ trong code, bổ sung check khi task 4 code xong `Question`. Đã thêm sẵn `QuizErrorCode.LESSON_HAS_QUESTIONS` (QUIZ_006) để task 4 dùng lại, theo đúng tiền lệ thêm `USERNAME_TAKEN` trước ở task 1 cho task 2 dùng.
- **`GET /api/parent/lessons?subjectId=`**: `subjectId` bắt buộc (không có giá trị mặc định) — đã kiểm tra `base`'s `GlobalExceptionHandler` có sẵn handler cho `MissingServletRequestParameterException` (trả 400 `COMMON_002` rõ ràng), không cần tự validate tay.
- Validate + OpenAPI: cùng chuẩn Task 1/2 — `@Valid`/`@NotBlank`/`@NotNull` trên DTO, `@Tag`/`@Operation`/`@ApiResponses` tiếng Anh trên 2 controller.

Lưu ý: session code này không có mạng ở máy — **chưa build/compile thử được**; đã kiểm tra brace/paren balance từng file OK, grep xác nhận không còn ký tự tiếng Việt trong code, `git status` chỉ có đúng các file mới + 1 file sửa (`QuizErrorCode.java`, thêm 2 mã lỗi). Anh cần tự chạy `./gradlew :api:compileJava` để xác nhận trước khi merge.
