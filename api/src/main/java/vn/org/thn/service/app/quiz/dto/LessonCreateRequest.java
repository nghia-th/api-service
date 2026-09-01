package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request body for {@code POST /api/parent/lessons}. {@code subjectId} must belong to the
 * current Parent (checked in {@code LessonService}), not just exist.
 * <p>
 * {@code summary}/{@code content}/{@code textbookPage} are all optional - a Parent can create a
 * bare lesson (just {@code name}) and fill these in later via update. The lesson image is not
 * part of this request; it is uploaded separately through {@code POST
 * /api/parent/lessons/{id}/image} once the lesson already exists.
 */
@Data
public class LessonCreateRequest {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "1", description = "Id of the subject this lesson belongs to - must belong to the current parent")
    private Long subjectId;

    @NotBlank
    @Schema(type = "string", example = "Unit 1 - Present Simple", description = "Lesson name")
    private String name;

    @Schema(type = "string", example = "On lai thi hien tai don, cach dung va dau hieu nhan biet.", description = "Short summary shown to the student before they review the full content", nullable = true)
    private String summary;

    @Schema(type = "string", description = "Detailed lesson content the student can read in full", nullable = true)
    private String content;

    @Positive
    @Schema(type = "integer", format = "int32", example = "12", description = "Page number in the textbook, if any", nullable = true)
    private Integer textbookPage;
}
