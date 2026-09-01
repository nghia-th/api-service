package vn.org.thn.service.app.quiz.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Body of {@code POST /api/parent/tests/practice} - generates a random "On tap kien thuc" Test
 * for one Student from one whole Subject's question pool (every Lesson under it), see {@code
 * TestService#generatePractice}.
 * <p>
 * Unlike {@link TestCreateRequest} there is no {@code questionIds} list - the question set is
 * picked randomly on the server, and can be regenerated any number of times by calling this
 * again; each call creates a brand-new Test (see {@link vn.org.thn.service.app.quiz.entity.TestType}'s
 * javadoc for why retakes never reuse an existing Test row).
 */
@Data
public class PracticeGenerateRequest {

    @NotNull
    private Long studentId;

    @NotNull
    private Long subjectId;

    /** Optional - blank/null defaults to "Ôn tập &lt;tên môn&gt;" (see {@code TestService}). */
    private String name;

    /** Optional - number of random questions to pick; null/non-positive defaults to 10 (or fewer if the Subject has fewer than 10 Questions total), see {@code TestService}. */
    private Integer questionCount;
}
