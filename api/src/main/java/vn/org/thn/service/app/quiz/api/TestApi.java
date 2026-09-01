package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.TestCreateRequest;
import vn.org.thn.service.app.quiz.dto.TestDetailResponse;
import vn.org.thn.service.app.quiz.dto.TestResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.TestService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Test creation/listing/deletion for the currently logged-in Parent (task 5). Creating a test
 * assigns it immediately - there is no separate "assign" endpoint. Same conventions as {@link
 * QuestionApi} for auth/ownership/OpenAPI documentation.
 */
@Tag(name = "Test", description = "Create/list/delete Tests assigned by the current Parent to their own Students")
@RestController
@RequestMapping("/api/parent/tests")
public class TestApi extends BaseCtl {

    @Autowired
    private TestService testService;

    @Operation(
            summary = "Create and assign a test",
            description = "Creates a Test and assigns it to studentId in one call - status is ASSIGNED immediately, there is no separate assign step. questionIds' order becomes each question's display order."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Test"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field is missing, or questionIds is empty - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "studentId or one of questionIds belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student or question with the given id - COMMON_005 NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TestResponse>> create(@Valid @RequestBody TestCreateRequest request) {
        return ok(testService.create(request));
    }

    @Operation(
            summary = "List my tests",
            description = "Every Test belonging to the current Parent, optionally filtered to one student's tests. Not paginated in v1."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current parent's tests - never another parent's"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "studentId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this studentId - COMMON_005 NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<TestResponse>>> list(
            @Parameter(description = "Optional - narrows the list to this student's tests only") @RequestParam(required = false) Long studentId) {
        return ok(testService.list(studentId));
    }

    @Operation(
            summary = "Get one test's detail",
            description = "Returns the Test plus its full question list (with choices, including which one is correct) in display order."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Test with its questions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No test with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestDetailResponse>> get(@Parameter(description = "Test id") @PathVariable Long id) {
        return ok(testService.get(id));
    }

    @Operation(
            summary = "Delete a test",
            description = "Blocked once the test has any attempt (in progress or submitted), to avoid losing result history. Only the owning Parent can delete their own Test."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This test does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No test with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Test already has an attempt - QUIZ_009 TEST_HAS_ATTEMPTS")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Test id") @PathVariable Long id) {
        testService.delete(id);
        return ok();
    }
}
