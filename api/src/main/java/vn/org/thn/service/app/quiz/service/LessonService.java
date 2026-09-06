package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.BulkDeleteResponse;
import vn.org.thn.service.app.quiz.dto.LessonCreateRequest;
import vn.org.thn.service.app.quiz.dto.LessonImage;
import vn.org.thn.service.app.quiz.dto.LessonResponse;
import vn.org.thn.service.app.quiz.dto.LessonUpdateRequest;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.db.DatabasePath;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lesson CRUD for the currently logged-in Parent (task 3), plus lesson-content fields and the
 * illustrative image (task "Backend: them field noi dung cho Lesson", 2026-09-01). {@code
 * Lesson} has no {@code parentId} of its own (see the entity's javadoc), so every ownership check
 * here goes through {@link SubjectService#getOwnedOrThrow} on the lesson's {@code subjectId} -
 * reused (package-private) rather than duplicating the "not found vs. forbidden" logic a second
 * time. This class's own {@code getOwnedOrThrow} is package-private for the same reason - task
 * 4's {@code QuestionService} reuses it to resolve a Question's indirect owner through its
 * Lesson, and now {@code StudentLessonService} also reuses {@link #getById} + {@link #loadImage}
 * once it has independently proven a Student may see this Lesson.
 * <p>
 * <b>Revision 2026-09-06:</b> {@code delete} used to BLOCK outright on any {@code Question}
 * child via {@code QuizErrorCode#LESSON_HAS_QUESTIONS} (QUIZ_006, task 3's original rule). Per
 * the user's explicit request ("phu huynh can duoc xoa cac du lieu nhu mon hoc, bai hoc, de on,
 * neu cac du lieu do duoc lien ket voi hoc sinh thi xoa luon nhung du lieu lien quan"), it now
 * cascades instead - see {@link CascadeDeleteService}'s javadoc for the full cascade design
 * (every Question/Test that depends on this Lesson is deleted with it, including any Test's
 * Attempt/score history, and every {@code LessonReport} "bao bai" row for it).
 * <p>
 * FILE IMPORT (added 2026-09-01, "phan bai hoc cho phep import bang file"): {@link
 * #createFromImportRow} is the persistence entry point {@link LessonImportService} calls back
 * into for each valid row of an uploaded Excel/CSV file, same "one shared persistence path for
 * both hand-entry and import" shape {@link QuestionService#createFromImportRow} already
 * established for Question. The lesson image is never part of the import row (same "text fields
 * via the file, the image uploaded afterwards" split as hand-entry lessons) - a Parent still has
 * to attach it per-lesson afterwards through {@link #uploadImage}.
 * <p>
 * IMAGE STORAGE: the illustrative image's bytes are never put in the database - only its
 * server-side filename lives in {@code Lesson.imagePath}. Files live under {@link #IMAGE_DIR}
 * (a service-local folder, per {@link DatabasePath}'s own javadoc guidance not to extend that
 * class for app-specific folders like uploads). The stored filename is always generated
 * server-side ({@code lesson-<id>-<random>.<ext>}, extension derived from the validated
 * content-type, never from the client-supplied original filename) - this sidesteps path
 * traversal (a filename like "../../etc/passwd") and any client-controlled extension entirely.
 */
@Service
public class LessonService extends IBase {

    /** 5 MB app-level cap - deliberately smaller than Spring's own {@code max-file-size: 8MB} (application.yaml) so this friendly QUIZ_017 error fires first; see that file's comment. */
    private static final long MAX_IMAGE_SIZE_BYTES = 5L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private static final Path IMAGE_DIR = DatabasePath.HOME.resolve("uploads").resolve("lessons");

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private CascadeDeleteService cascadeDeleteService;

    public LessonResponse create(LessonCreateRequest request) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(request.getSubjectId(), parentId);

        Lesson lesson = lessonRepository.save(newLesson(parentId, request.getSubjectId(), request.getName(),
                request.getSummary(), request.getContent(), request.getTextbookPage()));

        logInfo("Lesson created: id={}, subjectId={}, parentId={}", lesson.getId(), lesson.getSubjectId(), parentId);
        return LessonResponse.from(lesson);
    }

    /**
     * Creates one Lesson for {@link LessonImportService} - same persistence logic as {@link
     * #create}, minus re-deriving {@code parentId}/re-checking {@code subjectId} ownership a
     * second time per row (the import already resolved and verified both once for the whole
     * file). {@code @Transactional} on this method only (not the caller's import loop), same
     * "one row's insert is atomic with itself but independent of every other row" reasoning as
     * {@code QuestionService#createFromImportRow} - a later row failing must not roll back an
     * earlier row's already-committed success (best-effort per-row import, see task 4's
     * precedent).
     */
    @org.springframework.transaction.annotation.Transactional
    Lesson createFromImportRow(Long subjectId, Long parentId, String name, String summary, String content, Integer textbookPage) {
        return lessonRepository.save(newLesson(parentId, subjectId, name, summary, content, textbookPage));
    }

    /** A brand-new (unsaved) Lesson with both audit timestamps set - only ever for a genuine INSERT, same reasoning as {@code QuestionService#newQuestion}. imagePath is never set here - a brand-new Lesson never has an image yet, only attachable afterwards via {@link #uploadImage} once the Lesson has an id. */
    private Lesson newLesson(Long parentId, Long subjectId, String name, String summary, String content, Integer textbookPage) {
        LocalDateTime now = LocalDateTime.now();
        Lesson lesson = new Lesson();
        lesson.setSubjectId(subjectId);
        lesson.setName(name);
        lesson.setSummary(summary);
        lesson.setContent(content);
        lesson.setTextbookPage(textbookPage);
        lesson.setCreatedAt(now);
        lesson.setUpdatedAt(now);
        lesson.setCreatedBy("parent:" + parentId);
        lesson.setUpdatedBy("parent:" + parentId);
        return lesson;
    }

    public LessonResponse update(Long id, LessonUpdateRequest request) {
        Long parentId = CurrentUser.get().userId();
        Lesson lesson = getOwnedOrThrow(id, parentId);

        lesson.setName(request.getName());
        lesson.setSummary(request.getSummary());
        lesson.setContent(request.getContent());
        lesson.setTextbookPage(request.getTextbookPage());
        lesson.setUpdatedAt(LocalDateTime.now());
        lesson.setUpdatedBy("parent:" + parentId);
        lesson = lessonRepository.save(lesson);

        logInfo("Lesson updated: id={}, parentId={}", lesson.getId(), parentId);
        return LessonResponse.from(lesson);
    }

    public LessonResponse get(Long id) {
        return LessonResponse.from(getOwnedOrThrow(id, CurrentUser.get().userId()));
    }

    /** Every Lesson under {@code subjectId}. {@code subjectId} is a required query param (not "every lesson of the current parent") to keep the list short for a Subject -> Lesson picker UI - see task 3 spec. */
    public List<LessonResponse> list(Long subjectId) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);
        return lessonRepository.query().eq(Lesson::getSubjectId, subjectId).list()
                .stream().map(LessonResponse::from).toList();
    }

    /**
     * Deletes this Lesson and, per the 2026-09-06 revision, everything that depends on it - see
     * {@link CascadeDeleteService#deleteLessonsCascade} (also handles the lesson's own image
     * file, so this method no longer calls {@link #deleteImageFileQuietly} itself). This method
     * only does the ownership check first, same "auth here, cascade there" split as {@link
     * SubjectService#delete}/{@link QuestionService#delete}/{@link TestService#delete}.
     */
    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Lesson lesson = getOwnedOrThrow(id, parentId);
        cascadeDeleteService.deleteLessonsCascade(List.of(lesson.getId()));
        logInfo("Lesson deleted (cascade): id={}, parentId={}", lesson.getId(), parentId);
    }

    /**
     * Bulk delete (2026-09-06, "xoa bai cua muon hoc") - deletes each of {@code ids} via this
     * class's own {@link #delete}, so every id gets the same ownership check + cascade as a
     * single delete. Best-effort: one id failing (wrong owner, already gone, ...) does not stop
     * the rest - see {@link BulkDeleteSupport}.
     */
    public BulkDeleteResponse deleteMany(List<Long> ids) {
        return BulkDeleteSupport.deleteEach(ids, this::delete);
    }

    /**
     * Validates and stores a new illustrative image for the lesson, replacing any previous one.
     * Only the owning Parent can call this. Content-type is checked against {@link
     * #ALLOWED_IMAGE_TYPES} (never trusts the client-supplied filename/extension) and size against
     * {@link #MAX_IMAGE_SIZE_BYTES} before anything is written to disk.
     */
    public LessonResponse uploadImage(Long id, MultipartFile file) {
        Long parentId = CurrentUser.get().userId();
        Lesson lesson = getOwnedOrThrow(id, parentId);

        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(QuizErrorCode.LESSON_IMAGE_INVALID_TYPE);
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(QuizErrorCode.LESSON_IMAGE_TOO_LARGE);
        }

        try {
            Files.createDirectories(IMAGE_DIR);
        } catch (IOException e) {
            logError("Could not create lesson image directory " + IMAGE_DIR, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        String oldImagePath = lesson.getImagePath();
        String filename = "lesson-" + id + "-" + UUID.randomUUID() + "." + extension;
        Path target = IMAGE_DIR.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            logError("Could not save lesson image to " + target, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        lesson.setImagePath(filename);
        lesson.setUpdatedAt(LocalDateTime.now());
        lesson.setUpdatedBy("parent:" + parentId);
        lesson = lessonRepository.save(lesson);

        // Only removed AFTER the new file is safely written+saved, so a mid-upload failure never
        // leaves the lesson pointing at an image that no longer exists on disk.
        deleteImageFileQuietly(oldImagePath);

        logInfo("Lesson image uploaded: id={}, parentId={}, filename={}", id, parentId, filename);
        return LessonResponse.from(lesson);
    }

    /** Only the owning Parent can view it. Throws {@code COMMON_005 NOT_FOUND} if the lesson has no image yet. */
    public LessonImage getImageOwned(Long id, Long parentId) {
        Lesson lesson = getOwnedOrThrow(id, parentId);
        return loadImage(lesson);
    }

    public LessonResponse deleteImage(Long id) {
        Long parentId = CurrentUser.get().userId();
        Lesson lesson = getOwnedOrThrow(id, parentId);

        deleteImageFileQuietly(lesson.getImagePath());
        lesson.setImagePath(null);
        lesson.setUpdatedAt(LocalDateTime.now());
        lesson.setUpdatedBy("parent:" + parentId);
        lesson = lessonRepository.save(lesson);

        logInfo("Lesson image deleted: id={}, parentId={}", id, parentId);
        return LessonResponse.from(lesson);
    }

    /**
     * Reads the lesson's image bytes off disk. Package-private + takes the already-resolved
     * {@link Lesson} (no ownership check of its own) so {@code StudentLessonService} can reuse it
     * once it has independently proven, via its own multi-hop check, that the current Student may
     * see this Lesson.
     */
    LessonImage loadImage(Lesson lesson) {
        if (lesson.getImagePath() == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Lesson has no image");
        }
        Path path = IMAGE_DIR.resolve(lesson.getImagePath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            logError("Lesson image file missing on disk: " + path, e);
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Lesson image file not found");
        }
        return new LessonImage(bytes, contentTypeForFilename(lesson.getImagePath()), lesson.getImagePath());
    }

    private String contentTypeForFilename(String filename) {
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return ALLOWED_IMAGE_TYPES.entrySet().stream()
                .filter(entry -> entry.getValue().equals(ext))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }

    /** Best-effort delete - a missing/already-gone file is not an error worth failing the caller's request over. */
    private void deleteImageFileQuietly(String imagePath) {
        if (imagePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(IMAGE_DIR.resolve(imagePath));
        } catch (IOException e) {
            log().warn("Could not delete old lesson image file {}: {}", imagePath, e.getMessage());
        }
    }

    /** Loads the Lesson with id {@code id}, throwing if it doesn't exist or its Subject doesn't belong to {@code parentId}. Package-private (not private) so {@code QuestionService} can reuse it - same reasoning as {@code SubjectService#getOwnedOrThrow}. */
    Lesson getOwnedOrThrow(Long id, Long parentId) {
        Lesson lesson = getById(id);
        // Resolves ownership through the parent Subject - throws NOT_FOUND/FORBIDDEN the same way
        // a direct Lesson.parentId check would, if that field existed.
        subjectService.getOwnedOrThrow(lesson.getSubjectId(), parentId);
        return lesson;
    }

    /** Loads the Lesson with id {@code id} with NO ownership check at all. Package-private so {@code StudentLessonService} can resolve it after doing its own (Parent-unrelated) Student ownership check. */
    Lesson getById(Long id) {
        Lesson lesson = lessonRepository.findById(id);
        if (lesson == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Lesson not found");
        }
        return lesson;
    }
}
