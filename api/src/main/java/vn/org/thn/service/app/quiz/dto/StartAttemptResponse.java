package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/** Response body for {@code POST /api/student/tests/{testId}/start} - see {@link StudentQuestionResponse}/{@link StudentChoiceResponse} for why no correct-answer field appears anywhere in this tree. */
@Data
@AllArgsConstructor
public class StartAttemptResponse {
    private Long attemptId;
    private List<StudentQuestionResponse> questions;
}
