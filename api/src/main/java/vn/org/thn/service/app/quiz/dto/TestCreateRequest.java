package vn.org.thn.service.app.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request body for {@code POST /api/parent/tests}. Creating a Test always assigns it - there is
 * no separate "assign" step in v1 (see task 5 spec), so {@code studentId} is required here, not
 * added later.
 * <p>
 * The order of {@code questionIds} becomes each question's {@code orderIndex} - see {@code
 * TestService#create}.
 */
@Data
public class TestCreateRequest {

    @NotNull
    @Schema(type = "integer", format = "int64", example = "1", description = "Id of the student this test is assigned to - must belong to the current parent")
    private Long studentId;

    @NotBlank
    @Schema(type = "string", example = "Unit 1 Quiz", description = "Test name")
    private String name;

    @NotEmpty
    @Schema(description = "Ids of the questions in this test, in display order - must all belong to the current parent, at least 1 required")
    private List<@NotNull Long> questionIds;
}
