package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Body of {@code POST /api/parent/tests/from-lessons} (2026-09-05, item 3/11 of the 11-item batch
 * request: "khi tao de thi chon nhung bai con da hoc" - when creating a test, only allow picking
 * from Lessons the student has already studied for a subject, e.g. if 5 Lessons have been entered
 * for Toan, only those 5 are selectable). This is a PARALLEL creation mode alongside {@link
 * TestCreateRequest}'s hand-pick-each-question flow (AskUserQuestion 2026-09-05: "them lua chon
 * song song"), not a replacement - the Parent picks whole Lessons here instead of a single Lesson,
 * but (2026-09-06 revision, see below) still hand-picks the exact Questions to include, same as
 * {@link TestCreateRequest}.
 * <p>
 * <b>2026-09-06 revision</b> - the original version of this request had ONLY {@code lessonIds}
 * and the server auto-included EVERY Question under them. The user's own follow-up feedback after
 * testing ("chưa thấy danh sách câu hỏi của những bài đã chọn để phụ huynh chọn câu hỏi cho đề
 * kiểm tra" - "I don't see the question list from the selected lessons for the parent to pick
 * questions for the test") made clear {@code lessonIds} was meant to SCOPE which Questions are
 * offered, not to auto-select all of them. {@code questionIds} was added to carry the Parent's
 * actual pick; {@code lessonIds} is kept unchanged as the authorization/scope boundary - see
 * {@code TestService#createFromLessons} for how the two are cross-checked (every {@code
 * questionId} must belong to one of {@code lessonIds}, server-side, not trusted from the client).
 * The frontend still shuffles nothing extra client-side; {@code TestService#createFromLessons}
 * still shuffles the final display order server-side exactly as before this revision.
 * <p>
 * No {@code subjectId} field - the frontend restricts {@code lessonIds} to one Subject's own
 * Lessons at a time (a Subject-then-Lesson picker, mirroring {@code PracticeGenerateRequest}'s
 * Subject-level pool selection), but the backend does not itself require every lesson to share
 * one Subject - it only requires each Lesson's owning Subject to be in the SAME Classroom as
 * {@code studentId} (see {@code TestService#createFromLessons}), same rigor as {@code
 * PracticeGenerateRequest}'s subject/classroom check.
 */
@Data
public class TestCreateFromLessonsRequest {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "1", description = "Id of the student this test is assigned to - must belong to the current parent")
    private Long studentId;

    @NotBlank
    @Schema(type = "string", example = "Kiem tra 5 bai dau", description = "Test name")
    private String name;

    @NotEmpty
    @Schema(description = "Ids of the Lessons that questionIds are allowed to be picked from - must all belong to the current parent and be in the student's own classroom, at least 1 required")
    private List<@NotNull Long> lessonIds;

    @NotEmpty
    @Schema(description = "Ids of the specific Questions to include, hand-picked by the Parent from the combined question pool of lessonIds (2026-09-06 revision) - each one must belong to one of lessonIds, at least 1 required")
    private List<@NotNull Long> questionIds;
}
