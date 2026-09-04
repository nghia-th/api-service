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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.QuestionAudio;
import vn.org.thn.service.app.quiz.dto.QuestionImportResponse;
import vn.org.thn.service.app.quiz.dto.QuestionVideo;
import vn.org.thn.service.app.quiz.dto.QuestionRequest;
import vn.org.thn.service.app.quiz.dto.QuestionResponse;
import vn.org.thn.service.app.quiz.dto.TemplateFile;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.app.quiz.security.JwtAuthFilter;
import vn.org.thn.service.app.quiz.service.QuestionImportService;
import vn.org.thn.service.app.quiz.service.QuestionService;
import vn.org.thn.service.base.controller.BaseCtl;
import vn.org.thn.service.base.response.ApiResponse;

import java.util.List;

/**
 * Question/Choice CRUD + file import for the currently logged-in Parent (task 4). Same
 * conventions as {@link SubjectApi}/{@link LessonApi} for the hand-entry endpoints. The 2 import
 * endpoints at the bottom return a raw file / a plain import-result JSON respectively - {@code
 * importTemplate} does NOT go through {@link BaseCtl#ok}'s {@link ApiResponse} envelope, since its
 * response body is a binary file download, not a JSON API result.
 */
@Tag(name = "Question", description = "CRUD for Questions/Choices under the current Parent's own Lessons, plus Excel/CSV bulk import")
@RestController
@RequestMapping("/api/parent/questions")
public class QuestionApi extends BaseCtl {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionImportService questionImportService;

    @Operation(
            summary = "Create a question",
            description = "Creates a Question with its Choices in one call. lessonId must belong to the current parent. Choices need at least 2 entries with exactly one marked correct."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Created successfully - returns the new Question with its Choices"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field is missing/malformed, fewer than 2 choices, or not exactly one correct choice - COMMON_001 or QUIZ_007"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "lessonId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this lessonId - COMMON_005 NOT_FOUND")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<QuestionResponse>> create(@Valid @RequestBody QuestionRequest request) {
        return ok(questionService.create(request));
    }

    @Operation(
            summary = "Update a question",
            description = "Full replace - same request shape as create. All of the question's existing choices are replaced by the ones in this request (deleted and re-inserted, not diffed)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Updated successfully - returns the updated Question with its Choices"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "A required field is missing/malformed, fewer than 2 choices, or not exactly one correct choice - COMMON_001 or QUIZ_007"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question (or the new lessonId) does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id, or no lesson with the given lessonId - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This question has already been answered in a test attempt - it can no longer be edited - QUIZ_019 QUESTION_HAS_ATTEMPTS")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            @Parameter(description = "Question id") @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request) {
        return ok(questionService.update(id, request));
    }

    @Operation(
            summary = "List questions of a lesson",
            description = "Every Question under lessonId, each with its full Choice list including which one is correct - this is the Parent-facing view (see task 6 for the Student-facing view that hides the answer)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the lesson's questions - never another lesson's"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "lessonId query param is missing - COMMON_002"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "lessonId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this lessonId - COMMON_005 NOT_FOUND")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> list(
            @Parameter(description = "Lesson id - lists questions under this lesson only") @RequestParam Long lessonId) {
        return ok(questionService.list(lessonId));
    }

    @Operation(
            summary = "Get one question",
            description = "Only the owning Parent (via the Question's Lesson/Subject) can view it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the requested Question with its Choices"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<QuestionResponse>> get(@Parameter(description = "Question id") @PathVariable Long id) {
        return ok(questionService.get(id));
    }

    @Operation(
            summary = "Delete a question",
            description = "Blocked if the question is already used in any test (assigned or completed), to avoid breaking past results. Only the owning Parent can delete it."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted successfully - no response body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Question is used in a test - QUIZ_008 QUESTION_USED_IN_TEST")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@Parameter(description = "Question id") @PathVariable Long id) {
        questionService.delete(id);
        return ok();
    }

    @Operation(
            summary = "Upload/replace the question's audio clip",
            description = "MP3/M4A/WAV/OGG only, 10MB max. Replaces any previous audio for this question. Only the owning Parent can upload it. Blocked once the question has already been answered in an attempt, same rule as update()."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Uploaded successfully - returns the updated Question (hasAudio=true)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Wrong file type - QUIZ_020, or file too large - QUIZ_021"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This question has already been answered in a test attempt - QUIZ_019 QUESTION_HAS_ATTEMPTS")
    })
    @PostMapping(value = "/{id}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionResponse>> uploadAudio(
            @Parameter(description = "Question id") @PathVariable Long id,
            @Parameter(description = "The audio file - audio/mpeg, audio/mp4, audio/wav or audio/ogg") @RequestPart MultipartFile file) {
        return ok(questionService.uploadAudio(id, file));
    }

    @Operation(
            summary = "Download the question's audio clip",
            description = "Only the owning Parent can view it. See StudentAttemptApi for the student-facing equivalent (access gated by whether the question is on a test assigned to that student, not by lesson ownership)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the audio file (Content-Type set from its stored type)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id, or it has no audio - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}/audio")
    public ResponseEntity<byte[]> audio(@Parameter(description = "Question id") @PathVariable Long id) {
        QuestionAudio audio = questionService.getAudioOwned(id, CurrentUser.get().userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(audio.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audio.filename() + "\"")
                .body(audio.content());
    }

    @Operation(
            summary = "Delete the question's audio clip",
            description = "No-op (still 200) if the question had no audio. Only the owning Parent can delete it. Blocked once the question has already been answered in an attempt, same rule as uploadAudio()."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted (or already had no audio) - returns the updated Question (hasAudio=false)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This question has already been answered in a test attempt - QUIZ_019 QUESTION_HAS_ATTEMPTS")
    })
    @DeleteMapping("/{id}/audio")
    public ResponseEntity<ApiResponse<QuestionResponse>> deleteAudio(@Parameter(description = "Question id") @PathVariable Long id) {
        return ok(questionService.deleteAudio(id));
    }

    @Operation(
            summary = "Upload/replace the question's video clip",
            description = "Video-question feature (2026-09-04, part 3/4). MP4/WebM/MOV/OGG only, 50MB max. Replaces any previous video for this question. Only the owning Parent can upload it. Blocked once the question has already been answered in an attempt, same rule as uploadAudio()."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Uploaded successfully - returns the updated Question (hasVideo=true)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Wrong file type - QUIZ_028, or file too large - QUIZ_029"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This question has already been answered in a test attempt - QUIZ_019 QUESTION_HAS_ATTEMPTS")
    })
    @PostMapping(value = "/{id}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionResponse>> uploadVideo(
            @Parameter(description = "Question id") @PathVariable Long id,
            @Parameter(description = "The video file - video/mp4, video/webm, video/quicktime or video/ogg") @RequestPart MultipartFile file) {
        return ok(questionService.uploadVideo(id, file));
    }

    @Operation(
            summary = "Download the question's video clip",
            description = "Only the owning Parent can view it. See StudentAttemptApi for the student-facing equivalent (access gated by whether the question is on a test assigned to that student, not by lesson ownership)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the video file (Content-Type set from its stored type)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id, or it has no video - COMMON_005 NOT_FOUND")
    })
    @GetMapping("/{id}/video")
    public ResponseEntity<byte[]> video(@Parameter(description = "Question id") @PathVariable Long id) {
        QuestionVideo video = questionService.getVideoOwned(id, CurrentUser.get().userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(video.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + video.filename() + "\"")
                .body(video.content());
    }

    @Operation(
            summary = "Delete the question's video clip",
            description = "No-op (still 200) if the question had no video. Only the owning Parent can delete it. Blocked once the question has already been answered in an attempt, same rule as uploadVideo()."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Deleted (or already had no video) - returns the updated Question (hasVideo=false)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "This question does not belong to the current parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No question with this id - COMMON_005 NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This question has already been answered in a test attempt - QUIZ_019 QUESTION_HAS_ATTEMPTS")
    })
    @DeleteMapping("/{id}/video")
    public ResponseEntity<ApiResponse<QuestionResponse>> deleteVideo(@Parameter(description = "Question id") @PathVariable Long id) {
        return ok(questionService.deleteVideo(id));
    }

    @Operation(
            summary = "Download the question import template",
            description = "Returns a ready-to-fill Excel (default) or CSV file with the fixed 7-column layout plus one illustrative example row, which the import endpoint recognizes and skips automatically whether or not it is deleted before uploading."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Returns the template file (Content-Type set per the requested format)")
    })
    @GetMapping("/import-template")
    public ResponseEntity<byte[]> importTemplate(
            @Parameter(description = "\"xlsx\" (default) or \"csv\"") @RequestParam(required = false, defaultValue = "xlsx") String format) {
        TemplateFile template = questionImportService.generateTemplate(format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(template.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + template.filename() + "\"")
                .body(template.content());
    }

    @Operation(
            summary = "Import questions from an Excel/CSV file",
            description = "Best-effort per row - one bad row does not stop the others in the same file. lessonId must belong to the current parent, checked before the file is even read."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File was read - check the response body for per-row errors, if any (this is 200 even when some/all rows failed, since the request itself succeeded)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File could not be read at all (wrong format/corrupt/empty), or has more rows than the per-import limit - QUIZ_012 or QUIZ_011"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "lessonId belongs to another parent - COMMON_004 FORBIDDEN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "No lesson with this lessonId - COMMON_005 NOT_FOUND")
    })
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<QuestionImportResponse>> importFile(
            @Parameter(description = "Lesson id every imported question is attached to") @RequestParam Long lessonId,
            @Parameter(description = "The .xlsx or .csv file, filled in from the downloaded template") @RequestPart MultipartFile file) {
        return ok(questionImportService.importFile(lessonId, file));
    }
}
