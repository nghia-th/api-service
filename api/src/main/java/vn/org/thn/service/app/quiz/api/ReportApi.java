package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.AttemptReportResponse;
import vn.org.thn.service.app.quiz.dto.StudentAttemptHistoryItem;
import vn.org.thn.service.app.quiz.service.ReportService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

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
            @Parameter(description = "Student id") @PathVariable Long studentId) {
        return ok(reportService.getStudentAttemptHistory(studentId));
    }
}
