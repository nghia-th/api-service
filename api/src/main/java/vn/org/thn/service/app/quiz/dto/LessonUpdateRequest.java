package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Request body for {@code PUT /api/parent/lessons/{id}}. Unlike {@link SubjectRequest}'s reuse,
 * this is a separate DTO from {@link LessonCreateRequest} because update cannot move a Lesson to
 * a different Subject - only content fields are editable.
 * <p>
 * {@code summary}/{@code content}/{@code textbookPage} are optional and nullable here same as on
 * create - sending {@code null} clears the field back out (see {@code LessonService#update}). The
 * image is never touched by this endpoint; use {@code POST}/{@code DELETE
 * /api/parent/lessons/{id}/image} for that.
 */
@Data
public class LessonUpdateRequest {

    @NotBlank
    @Schema(type = "string", example = "Unit 1 - Present Simple (revised)", description = "New lesson name")
    private String name;

    @Schema(type = "string", example = "On lai thi hien tai don, cach dung va dau hieu nhan biet.", description = "Short summary shown to the student before they review the full content", nullable = true)
    private String summary;

    @Schema(type = "string", description = "Detailed lesson content the student can read in full", nullable = true)
    private String content;

    @Positive
    @Schema(type = "integer", format = "int32", example = "12", description = "Page number in the textbook, if any", nullable = true)
    private Integer textbookPage;
}
