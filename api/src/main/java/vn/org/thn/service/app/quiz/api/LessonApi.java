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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.LessonCreateRequest;
import vn.org.thn.service.app.quiz.dto.LessonResponse;
import vn.org.thn.service.app.quiz.dto.LessonUpdateRequest;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.LessonService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Lesson CRUD for the currently logged-in Parent (task 3). Same conventions as {@link
 * SubjectApi}. Behind {@link JwtAuthFilter}; ownership (indirect, through the Lesson's Subject)
 * enforced in {@link LessonService}.
 */
@Tag(name = "Lesson", description = "CRUD for Lessons under the current Parent's own Subjects")
@RestController
@RequestMapping("/api/parent/lessons")
public class LessonApi extends BaseCtl {

    @Autowired
    private LessonService lessonService;

    @Operation(
            summary = "Create a lesson",
            description = "Creates a new Lesson under an existing Subject. subjectId must belong to the current parent, not just exist."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Lesson"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "subjectId or name is missing/malformed - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "subjectId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this subjectId - COMMON_005 NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<LessonResponse>> create(@Valid @RequestBody LessonCreateRequest request) {
        return ok(lessonService.create(request));
    }

    @Operation(
            summary = "Update a lesson",
            description = "Only the name can be changed - a Lesson cannot be moved to a different Subject. Only the owning Parent (via the Lesson's Subject) can update it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated Lesson"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This lesson does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this id - COMMON_005 NOT_FOUND")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> update(
            @Parameter(description = "Lesson id") @PathVariable Long id,
            @Valid @RequestBody LessonUpdateRequest request) {
        return ok(lessonService.update(id, request));
    }

    @Operation(
            summary = "List lessons of a subject",
            description = "Every Lesson under the given subjectId. subjectId is required - this never returns every lesson across every subject of the current parent in one call."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the subject's lessons - never another subject's"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "subjectId query param is missing - COMMON_002"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "subjectId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No subject with this subjectId - COMMON_005 NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<LessonResponse>>> list(
            @Parameter(description = "Subject id - lists lessons under this subject only") @RequestParam Long subjectId) {
        return ok(lessonService.list(subjectId));
    }

    @Operation(
            summary = "Get one lesson",
            description = "Only the owning Parent (via the Lesson's Subject) can view it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Lesson"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This lesson does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> get(@Parameter(description = "Lesson id") @PathVariable Long id) {
        return ok(lessonService.get(id));
    }

    @Operation(
            summary = "Delete a lesson",
            description = "Blocked if the lesson still has questions - delete those first (QUIZ_006). Only the owning Parent (via the Lesson's Subject) can delete it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This lesson does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Lesson still has questions - QUIZ_006 LESSON_HAS_QUESTIONS")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Lesson id") @PathVariable Long id) {
        lessonService.delete(id);
        return ok();
    }
}
