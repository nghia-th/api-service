package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.AnswerRequest;
import vn.org.thn.service.app.quiz.dto.QuestionAudio;
import vn.org.thn.service.app.quiz.dto.StartAttemptResponse;
import vn.org.thn.service.app.quiz.dto.StudentPracticeGenerateRequest;
import vn.org.thn.service.app.quiz.dto.StudentTestSummaryResponse;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
import vn.org.thn.service.app.quiz.dto.SubmitAttemptResponse;
import vn.org.thn.service.app.quiz.dto.TestResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.StudentAttemptService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Student-facing test-taking flow (task 6) - list assigned tests, start/resume an attempt, save
 * answers, submit for grading. Everything here is under {@code /api/student/**}, gated by {@link
 * JwtAuthFilter} to a STUDENT-role token (a PARENT token gets 403, see {@code JwtAuthFilter}),
 * mirroring how every {@code /api/parent/**} controller is gated to PARENT.
 */
@Tag(name = "Student Attempt", description = "Student-facing: list assigned tests, take a test, submit for grading")
@RestController
@RequestMapping("/api/student")
public class StudentAttemptApi extends BaseCtl {

    @Autowired
    private StudentAttemptService studentAttemptService;

    @Operation(
            summary = "List my assigned tests",
            description = "Every Test assigned to the current Student, with status (ASSIGNED/COMPLETED) so the student knows what is left to do."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current student's tests - never another student's")
    })
    @GetMapping("/tests")
    public ResponseEntity<ApiResponse<List<StudentTestSummaryResponse>>> listTests() {
        return ok(studentAttemptService.listTests());
    }

    @Operation(
            summary = "List subjects available to me",
            description = "Every Subject in the current Student's own Classroom - used to populate the \"chọn Môn\" dropdown before generating a practice test. The Student never picks a Classroom (they only ever belong to one)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current student's own classroom's subjects")
    })
    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> listSubjects() {
        return ok(studentAttemptService.listSubjects());
    }

    @Operation(
            summary = "Generate my own practice test (Ôn tập)",
            description = "Self-service version of the Parent's generate-practice endpoint - the current Student picks one Subject and gets a brand-new randomized practice Test back, immediately startable via the normal start/answer/submit flow. Can be called again any number of times for unlimited retakes, each with a freshly-randomized question set."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new practice Test"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "The subject has no questions to practice from - QUIZ_018 SUBJECT_NO_QUESTIONS"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject is not in the current student's classroom - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @PostMapping("/tests/practice")
    public ResponseEntity<ApiResponse<TestResponse>> generatePractice(@Valid @RequestBody StudentPracticeGenerateRequest request) {
        return ok(studentAttemptService.generatePractice(request));
    }

    @Operation(
            summary = "Download a question's audio clip",
            description = "Listening-question feature (task \"Cau hoi dang am thanh\", 2026-09-01). Only reachable if some test assigned to the current student has this question on it (works both while taking the test and after submitting, same as the lesson content/image endpoints)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the audio file (Content-Type set from its stored type)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question is not reachable from any test assigned to the current student - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id, or it has no audio - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/questions/{id}/audio")
    public ResponseEntity<byte[]> questionAudio(@Parameter(description = "Question id") @PathVariable Long id) {
        QuestionAudio audio = studentAttemptService.getQuestionAudio(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audio.filename() + "\"")
                .body(audio.content());
    }

    @Operation(
            summary = "Start or resume a test",
            description = "Idempotent - a second call for the same test returns the same attemptId instead of creating a new attempt (v1 allows only 1 attempt per test). The response never includes which choice is correct."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the attempt id and the test's questions (answers hidden)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This test is not assigned to the current student - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No test with this id - COMMON_005 NOT_FOUND")
    })
    @PostMapping("/tests/{testId}/start")
    public ResponseEntity<ApiResponse<StartAttemptResponse>> start(@Parameter(description = "Test id") @PathVariable Long testId) {
        return ok(studentAttemptService.start(testId));
    }

    @Operation(
            summary = "Save answers",
            description = "Upserts by questionId - accepts one answer or the whole test at once, callable repeatedly before submit. Rejected once the attempt has already been submitted."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Answers saved - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "answers is empty, or a questionId/choiceId does not belong to this attempt's test - COMMON_001 or COMMON_002"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This attempt does not belong to the current student - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No attempt with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attempt was already submitted - QUIZ_010 ATTEMPT_ALREADY_SUBMITTED")
    })
    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(
            @Parameter(description = "Attempt id") @PathVariable Long attemptId,
            @Valid @RequestBody AnswerRequest request) {
        studentAttemptService.saveAnswers(attemptId, request);
        return ok();
    }

    @Operation(
            summary = "Submit the attempt for grading",
            description = "Grades every question (a never-answered question counts as wrong), sets the test to COMPLETED, and can only be called once per attempt."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the basic score - correct count, total questions, percent"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This attempt does not belong to the current student - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No attempt with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attempt was already submitted - QUIZ_010 ATTEMPT_ALREADY_SUBMITTED")
    })
    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<ApiResponse<SubmitAttemptResponse>> submit(@Parameter(description = "Attempt id") @PathVariable Long attemptId) {
        return ok(studentAttemptService.submit(attemptId));
    }
}
