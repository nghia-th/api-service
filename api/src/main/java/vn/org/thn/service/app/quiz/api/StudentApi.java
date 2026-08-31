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
import vn.org.thn.service.app.quiz.dto.StudentCreateRequest;
import vn.org.thn.service.app.quiz.dto.StudentResponse;
import vn.org.thn.service.app.quiz.dto.StudentUpdateRequest;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.StudentService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Student CRUD for the currently logged-in Parent (task 2). Every endpoint here is behind {@link
 * JwtAuthFilter} (see {@code config/SecurityConfig}'s {@code /api/parent/*} pattern) - a Parent
 * token is required, and ownership (the {@code id} in the path belongs to the caller) is
 * enforced in {@link StudentService}, not here.
 * <p>
 * As with {@link AuthApi}, {@code @Valid} triggers Bean Validation on the request DTOs, and the
 * Swagger {@code @io.swagger.v3.oas.annotations.responses.ApiResponse} is used fully-qualified
 * to avoid colliding with this module's own {@link ApiResponse} response envelope.
 */
@Tag(name = "Student", description = "CRUD for the current Parent's own Student accounts")
@RestController
@RequestMapping("/api/parent/students")
public class StudentApi extends BaseCtl {

    @Autowired
    private StudentService studentService;

    @Operation(
            summary = "Create a student",
            description = "Creates a new Student owned by the current Parent. parentId is taken from the token, never from the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Student (password excluded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field (fullName/grade/username/password) is missing or malformed - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username is already taken (system-wide, not just within this parent) - QUIZ_003 USERNAME_TAKEN")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<StudentResponse>> create(@Valid @RequestBody StudentCreateRequest request) {
        return ok(studentService.create(request));
    }

    @Operation(
            summary = "Update a student",
            description = "Every field is optional - omit or send null for a field to leave it unchanged. Only the owning Parent can update their own Student."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated Student"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A supplied field is malformed (e.g. password shorter than 6 characters) - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This student does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "New username is already taken by another student - QUIZ_003 USERNAME_TAKEN")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> update(
            @Parameter(description = "Student id") @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request) {
        return ok(studentService.update(id, request));
    }

    @Operation(
            summary = "List my students",
            description = "Every Student belonging to the current Parent. Not paginated in v1 - the number of children in one family is always small."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current parent's students - never another parent's")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<StudentResponse>>> list() {
        return ok(studentService.list());
    }

    @Operation(
            summary = "Get one student",
            description = "Only the owning Parent can view their own Student."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Student"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This student does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StudentResponse>> get(@Parameter(description = "Student id") @PathVariable Long id) {
        return ok(studentService.get(id));
    }

    @Operation(
            summary = "Delete a student",
            description = "Hard delete in v1 - Test/Attempt entities do not exist yet, so there is no test history to worry about losing. Only the owning Parent can delete their own Student."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This student does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No student with this id - COMMON_005 NOT_FOUND")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Student id") @PathVariable Long id) {
        studentService.delete(id);
        return ok();
    }
}
