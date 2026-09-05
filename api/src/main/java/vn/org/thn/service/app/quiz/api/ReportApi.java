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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.AttemptReportResponse;
import vn.org.thn.service.app.quiz.dto.SpeakingAnswerAudio;
import vn.org.thn.service.app.quiz.dto.SpeakingGradeRequest;
import vn.org.thn.service.app.quiz.dto.StudentAttemptHistoryItem;
import vn.org.thn.service.app.quiz.service.ReportService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only results/reporting for the currently logged-in Parent (task 7) - the knowledge-tag
 * breakdown this whole product exists for, not just a raw score (see {@code
 * claude/hieu-bai-app-phan-tich.md}).
 */
@Tag(name = "Report", description = "Read-only: a child's test results, including the per-knowledge-tag breakdown")
@RestController
@RequestMapping("/api/parent")
public class ReportApi extends BaseCtl {

    @Autowired
    private ReportService reportService;

    @Operation(
            summary = "Get one attempt's full report",
            description = "Per-question detail (chosen vs. correct answer) plus a breakdown by knowledge tag - questions with no tag are grouped under \"Chua phan loai\" (Uncategorized), never dropped. Blocked while the attempt has not been submitted yet."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the full report for this attempt"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This attempt's test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No attempt with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attempt has not been submitted yet - QUIZ_013 ATTEMPT_NOT_SUBMITTED")
    })
    @GetMapping("/attempts/{id}")
    public ResponseEntity<ApiResponse<AttemptReportResponse>> getAttemptReport(@Parameter(description = "Attempt id") @PathVariable Long id) {
        return ok(reportService.getAttemptReport(id));
    }

    @Operation(
            summary = "Get a student's test history",
            description = "Rolled-up list of the student's submitted attempts, newest first - tap into one via GET /api/parent/attempts/{id} for the full per-question report."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the student's submitted attempts - never another student's"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This student does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/students/{studentId}/attempts")
    public ResponseEntity<ApiResponse<List<StudentAttemptHistoryItem>>> getStudentAttemptHistory(
            @Parameter(description = "Student id") @PathVariable Long studentId,
            @Parameter(description = "Inclusive start date (yyyy-MM-dd) - added 2026-09-05 for the \"weekly history\" view, item 8 of the 11-item batch. Omit both from/to to get the full history exactly as before.") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Inclusive end date (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ok(reportService.getStudentAttemptHistory(studentId, from, to));
    }

    @Operation(
            summary = "Play back a student's recorded speaking answer",
            description = "Speaking-question feature (task \"Cau hoi dang tu luan/thu am\", 2026-09-01). Only reachable once the attempt has been submitted, same gate as the full report."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the audio file (Content-Type set from its stored type)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This attempt's test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No attempt with this id, or nothing recorded for this question - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attempt has not been submitted yet - QUIZ_013 ATTEMPT_NOT_SUBMITTED")
    })
    @GetMapping("/attempts/{attemptId}/questions/{questionId}/speaking-answer")
    public ResponseEntity<byte[]> speakingAnswer(
            @Parameter(description = "Attempt id") @PathVariable Long attemptId,
            @Parameter(description = "Question id") @PathVariable Long questionId) {
        SpeakingAnswerAudio audio = reportService.getSpeakingAnswerAudio(attemptId, questionId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audio.filename() + "\"")
                .body(audio.content());
    }

    @Operation(
            summary = "Grade a student's recorded speaking answer (reference only)",
            description = "Purely a note for the parent's own report reading - never affects the attempt's score. correct=null clears it back to \"not reviewed\". Only reachable once the attempt has been submitted."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Graded successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This attempt's test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No attempt/question with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Attempt has not been submitted yet - QUIZ_013, or the question is not a SPEAKING question - QUIZ_025")
    })
    @PutMapping("/attempts/{attemptId}/questions/{questionId}/grade")
    public ResponseEntity<ApiResponse<Void>> gradeSpeakingAnswer(
            @Parameter(description = "Attempt id") @PathVariable Long attemptId,
            @Parameter(description = "Question id") @PathVariable Long questionId,
            @Valid @RequestBody SpeakingGradeRequest request) {
        reportService.gradeSpeakingAnswer(attemptId, questionId, request.getCorrect());
        return ok();
    }
}
