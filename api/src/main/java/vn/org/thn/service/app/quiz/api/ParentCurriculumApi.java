package vn.org.thn.service.app.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.org.thn.service.app.quiz.dto.CurriculumResponse;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.CurriculumService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Read-only curriculum list for the currently logged-in Parent (2026-09-05) - reuses {@link
 * CurriculumService#list()} with no ownership filtering (same shared method the Admin CRUD API
 * uses), so the Parent's "browse the library" filter dropdown (see {@code
 * SubjectLibraryDialog.tsx}) can offer the current, Admin-managed curriculum list instead of a
 * hardcoded one. No create/update/delete here - only an Admin manages the list itself (see {@link
 * AdminCurriculumApi}). Behind {@link JwtAuthFilter} under {@code /api/parent/*}.
 */
@Tag(name = "Parent - Curriculum", description = "Read-only curriculum list for the current Parent")
@RestController
@RequestMapping("/api/parent/curricula")
public class ParentCurriculumApi extends BaseCtl {

    @Autowired
    private CurriculumService curriculumService;

    @Operation(summary = "List all curricula", description = "Same list every Admin manages - used to populate the library browse filter dropdown.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Every curriculum")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CurriculumResponse>>> list() {
        return ok(curriculumService.list());
    }
}
