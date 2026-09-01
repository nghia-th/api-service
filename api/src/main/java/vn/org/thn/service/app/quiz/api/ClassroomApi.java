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
import vn.org.thn.service.app.quiz.dto.ClassroomRequest;
import vn.org.thn.service.app.quiz.dto.ClassroomResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.ClassroomService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Classroom CRUD for the currently logged-in Parent - top of the Classroom -> Subject -> Lesson
 * -> Question hierarchy, and the target of each Student's {@code classroomId}. Same conventions
 * as {@link SubjectApi}: behind {@link JwtAuthFilter}, ownership enforced in the service layer.
 */
@Tag(name = "Classroom", description = "CRUD for the current Parent's own Classrooms")
@RestController
@RequestMapping("/api/parent/classrooms")
public class ClassroomApi extends BaseCtl {

    @Autowired
    private ClassroomService classroomService;

    @Operation(
            summary = "Create a classroom",
            description = "Creates a new Classroom owned by the current Parent. parentId is taken from the token, never from the request body."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Classroom"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ClassroomResponse>> create(@Valid @RequestBody ClassroomRequest request) {
        return ok(classroomService.create(request));
    }

    @Operation(
            summary = "Update a classroom",
            description = "Only the owning Parent can update their own Classroom."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated Classroom"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This classroom does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No classroom with this id - COMMON_005 NOT_FOUND")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassroomResponse>> update(
            @Parameter(description = "Classroom id") @PathVariable Long id,
            @Valid @RequestBody ClassroomRequest request) {
        return ok(classroomService.update(id, request));
    }

    @Operation(
            summary = "List my classrooms",
            description = "Every Classroom belonging to the current Parent. Not paginated in v1."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the current parent's classrooms - never another parent's")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClassroomResponse>>> list() {
        return ok(classroomService.list());
    }

    @Operation(
            summary = "Get one classroom",
            description = "Only the owning Parent can view their own Classroom."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Classroom"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This classroom does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No classroom with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClassroomResponse>> get(@Parameter(description = "Classroom id") @PathVariable Long id) {
        return ok(classroomService.get(id));
    }

    @Operation(
            summary = "Delete a classroom",
            description = "Blocked while the Classroom still has Student or Subject children - move/delete them first. Only the owning Parent can delete their own Classroom."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This classroom does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No classroom with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Classroom still has students (QUIZ_014) or subjects (QUIZ_015)")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Classroom id") @PathVariable Long id) {
        classroomService.delete(id);
        return ok();
    }
}
