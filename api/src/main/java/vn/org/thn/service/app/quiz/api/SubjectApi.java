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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.SubjectRequest;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.SubjectService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Subject CRUD for the currently logged-in Parent (task 3). Same conventions as {@link
 * vn.org.thn.service.app.quiz.api.StudentApi}: behind {@link JwtAuthFilter}, ownership enforced
 * in the service layer, Swagger {@code @io.swagger.v3.oas.annotations.responses.ApiResponse}
 * fully-qualified to avoid colliding with this module's own {@link ApiResponse}.
 */
@Tag(name = "Subject", description = "CRUD for the current Parent's own Subjects")
@RestController
@RequestMapping("/api/parent/subjects")
public class SubjectApi extends BaseCtl {

    @Autowired
    private SubjectService subjectService;

    @Operation(
            summary = "Create a subject",
            description = "Creates a new Subject owned by the current Parent. parentId is taken from the token, never from the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        return ok(subjectService.create(request));
    }

    @Operation(
            summary = "Update a subject",
            description = "Only the owning Parent can update their own Subject."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated Subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @Parameter(description = "Subject id") @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return ok(subjectService.update(id, request));
    }

    @Operation(
            summary = "List my subjects",
            description = "Every Subject belonging to the current Parent. Not paginated in v1."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current parent's subjects - never another parent's")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> list() {
        return ok(subjectService.list());
    }

    @Operation(
            summary = "Get one subject",
            description = "Only the owning Parent can view their own Subject."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Subject"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> get(@Parameter(description = "Subject id") @PathVariable Long id) {
        return ok(subjectService.get(id));
    }

    @Operation(
            summary = "Delete a subject",
            description = "Blocked while the Subject still has Lesson children - delete or move its lessons first. Only the owning Parent can delete their own Subject."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This subject does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Subject still has lessons - QUIZ_005 SUBJECT_HAS_LESSONS")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Subject id") @PathVariable Long id) {
        subjectService.delete(id);
        return ok();
    }
}
