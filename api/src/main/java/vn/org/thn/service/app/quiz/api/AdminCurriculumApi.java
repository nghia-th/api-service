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
import vn.org.thn.service.app.quiz.dto.CurriculumRequest;
import vn.org.thn.service.app.quiz.dto.CurriculumResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.CurriculumService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Admin CRUD for the "bo sach" (curriculum) lookup list (2026-09-05) - full CRUD, no root
 * restriction (every Admin can manage it, same as {@link AdminLibraryApi}). See {@link
 * CurriculumService}'s javadoc for the full feature background. Behind {@link JwtAuthFilter}
 * under {@code /api/admin/*}.
 */
@Tag(name = "Admin - Curriculum", description = "Admin CRUD for the curriculum (bo sach) lookup list")
@RestController
@RequestMapping("/api/admin/curricula")
public class AdminCurriculumApi extends BaseCtl {

    @Autowired
    private CurriculumService curriculumService;

    @Operation(summary = "List all curricula")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every curriculum")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CurriculumResponse>>> list() {
        return ok(curriculumService.list());
    }

    @Operation(
            summary = "Create a curriculum",
            description = "name must be unique - otherwise QUIZ_036."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new curriculum"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "A curriculum with this name already exists - QUIZ_036")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CurriculumResponse>> create(@Valid @RequestBody CurriculumRequest request) {
        return ok(curriculumService.create(request));
    }

    @Operation(
            summary = "Update a curriculum",
            description = "name must stay unique across the other curricula - otherwise QUIZ_036."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated curriculum"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "name is missing or blank - COMMON_001"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No curriculum with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "A curriculum with this name already exists - QUIZ_036")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CurriculumResponse>> update(
            @Parameter(description = "Curriculum id") @PathVariable Long id,
            @Valid @RequestBody CurriculumRequest request) {
        return ok(curriculumService.update(id, request));
    }

    @Operation(
            summary = "Delete a curriculum",
            description = "Blocked while any library document still uses this curriculum's name - reassign or delete them first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No curriculum with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Curriculum is still used by a library document - QUIZ_037")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Curriculum id") @PathVariable Long id) {
        curriculumService.delete(id);
        return ok();
    }
}
