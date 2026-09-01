package vn.org.thn.service.app.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** Request body for {@code POST /api/student/attempts/{attemptId}/answers} - accepts either one answer at a time or the whole test at once, since saving is an upsert per questionId either way (see task 6 spec). */
@Data
public class AnswerRequest {

    @NotEmpty
    @Valid
    private List<AnswerItem> answers;
}
