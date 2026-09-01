package vn.org.thn.service.app.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One row of {@link AttemptReportResponse#getAnswers()} - Parent-facing, so unlike task 6's student view, this DOES show the correct answer next to what the student actually chose. */
@Data
@AllArgsConstructor
public class AttemptAnswerDetail {
    private Long questionId;
    private String questionContent;
    /** Null if the student left this question blank. */
    private String chosenChoiceContent;
    private String correctChoiceContent;
    private boolean correct;
    private String knowledgeTag;
}
