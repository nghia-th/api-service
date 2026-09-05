package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentResponse;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.SubjectLibraryLinkResponse;
import vn.org.thn.service.app.quiz.entity.Curriculum;
import vn.org.thn.service.app.quiz.entity.LibraryDocument;
import vn.org.thn.service.app.quiz.entity.SubjectLibraryLink;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.CurriculumRepository;
import vn.org.thn.service.app.quiz.repository.LibraryDocumentRepository;
import vn.org.thn.service.app.quiz.repository.SubjectLibraryLinkRepository;
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
 * Admin-managed textbook PDF library (2026-09-05, "thu vien sach giao khoa" feature, per the
 * user's explicit request): an Admin uploads PDF textbooks organized by {@code grade} (1-12),
 * {@code subjectName} (free text, e.g. "Toan") and {@code curriculum} (a name from the
 * Admin-managed {@link vn.org.thn.service.app.quiz.entity.Curriculum} list, see {@code
 * CurriculumService}'s javadoc - previously a hardcoded 3-value list, changed 2026-09-05 per
 * the user's explicit request), plus an optional {@code volume} - see {@link LibraryDocument}'s javadoc for the full
 * shape and the user's own example ("Lop 4 -&gt; Toan tap 1 -&gt; Ket noi tri thuc").
 * <p>
 * ACCESS MODEL (3 roles, mirroring {@code LessonService}/{@code StudentLessonService}'s existing
 * "one shared file-serving method, per-role access check" shape):
 * <ul>
 *     <li><b>Admin</b> ({@code AdminLibraryApi}, this class) - full CRUD over the whole library,
 *     no root restriction (unlike {@code AdminManageApi}'s Admin-manages-Admin feature - every
 *     Admin can manage textbooks).</li>
 *     <li><b>Parent</b> ({@code ParentLibraryApi}/{@code ParentLibraryService}) - browses the
 *     WHOLE library read-only (to decide what to link, {@link #list}), then links/unlinks their
 *     OWN {@link vn.org.thn.service.app.quiz.entity.Subject} rows to documents (many-to-many, one
 *     Subject can link multiple documents, per the user's explicit decision) via {@link
 *     vn.org.thn.service.app.quiz.entity.SubjectLibraryLink} - the actual PDF file is only
 *     reachable for a Subject the Parent owns AND has linked.</li>
 *     <li><b>Student</b> ({@code StudentLibraryApi}/{@code StudentLibraryService}) - read-only,
 *     can view/download a document linked to any Subject in their OWN classroom (per the user's
 *     explicit decision that both Parent AND Student can view/download) - a more direct
 *     Subject-&gt;Classroom-&gt;Student check than {@code StudentLessonService}'s Test-assignment-
 *     based one, since a textbook is reference material for the whole Subject/Classroom, not tied
 *     to any specific assigned Test.</li>
 * </ul>
 * {@link #getById}/{@link #loadFile}/{@link #listLinksForSubject}/{@link #isLinked} are
 * package-private with NO ownership check of their own, reused by {@code ParentLibraryService}/
 * {@code StudentLibraryService} once each has independently proven (its own, different) access
 * rule - same "shared low-level method, per-role check happens one layer up" shape as {@code
 * LessonService#getById}/{@code #loadImage}.
 * <p>
 * FILE STORAGE: same convention as {@code LessonService}'s {@code IMAGE_DIR} - only the
 * server-generated filename lives in {@code LibraryDocument.filePath}, the actual PDF bytes live
 * under {@link #LIBRARY_DIR} (a service-local folder, per {@code DatabasePath}'s own javadoc
 * guidance not to extend that class for app-specific upload folders).
 */
@Service
public class LibraryService extends IBase {

    private static final long MAX_PDF_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_PDF_TYPES = Map.of("application/pdf", "pdf");
    private static final Path LIBRARY_DIR = DatabasePath.HOME.resolve("uploads").resolve("library");

    private static final int MIN_GRADE = 1;
    private static final int MAX_GRADE = 12;

    @Autowired
    private LibraryDocumentRepository libraryDocumentRepository;

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private SubjectLibraryLinkRepository subjectLibraryLinkRepository;

    /** Every library document, optionally narrowed by grade/subjectName(partial match)/curriculum - used by BOTH Admin's management list and Parent's browse-to-link list (see this class's javadoc), so no ownership filtering applies here at all - the whole catalog is visible to any authenticated Admin or Parent. */
    public List<LibraryDocumentResponse> list(Integer grade, String subjectName, String curriculum) {
        return libraryDocumentRepository.query()
                .eq(LibraryDocument::getGrade, grade)
                .like(LibraryDocument::getSubjectName, subjectName)
                .eq(LibraryDocument::getCurriculum, curriculum)
                .list()
                .stream().map(LibraryDocumentResponse::from).toList();
    }

    /** Admin-only (enforced by {@code JwtAuthFilter}'s {@code /api/admin/*} prefix, no further root check - see this class's javadoc). Validates grade against the 1-12 range and curriculum against the Admin-managed Curriculum list, and the file against {@link #ALLOWED_PDF_TYPES}/{@link #MAX_PDF_SIZE_BYTES} before anything is written to disk. */
    public LibraryDocumentResponse upload(int grade, String subjectName, String curriculum, String volume, String title, MultipartFile file) {
        if (grade < MIN_GRADE || grade > MAX_GRADE
                || !curriculumRepository.query().eq(Curriculum::getName, curriculum).exists()) {
            throw new BusinessException(QuizErrorCode.LIBRARY_INVALID_TAXONOMY);
        }
        String extension = ALLOWED_PDF_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(QuizErrorCode.LIBRARY_PDF_INVALID_TYPE);
        }
        if (file.getSize() > MAX_PDF_SIZE_BYTES) {
            throw new BusinessException(QuizErrorCode.LIBRARY_PDF_TOO_LARGE);
        }

        try {
            Files.createDirectories(LIBRARY_DIR);
        } catch (IOException e) {
            logError("Could not create library directory " + LIBRARY_DIR, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        String filename = "library-" + UUID.randomUUID() + "." + extension;
        Path target = LIBRARY_DIR.resolve(filename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            logError("Could not save library document to " + target, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }

        Long adminId = CurrentUser.get().userId();
        LocalDateTime now = LocalDateTime.now();
        String actor = "admin:" + adminId;
        String resolvedTitle = (title == null || title.isBlank())
                ? defaultTitle(grade, subjectName, volume, curriculum)
                : title.trim();

        LibraryDocument doc = new LibraryDocument();
        doc.setGrade(grade);
        doc.setSubjectName(subjectName);
        doc.setCurriculum(curriculum);
        doc.setVolume(volume == null || volume.isBlank() ? null : volume.trim());
        doc.setTitle(resolvedTitle);
        doc.setFilePath(filename);
        doc.setFileSize(file.getSize());
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc.setCreatedBy(actor);
        doc.setUpdatedBy(actor);
        doc = libraryDocumentRepository.save(doc);

        logInfo("Library document uploaded: id={}, grade={}, subjectName={}, curriculum={}, adminId={}",
                doc.getId(), grade, subjectName, curriculum, adminId);
        return LibraryDocumentResponse.from(doc);
    }

    /** e.g. "Toán 4 - Tập 1 - Kết nối tri thức" (volume omitted if blank) - only used when the Admin leaves the title field blank on upload. */
    private String defaultTitle(int grade, String subjectName, String volume, String curriculum) {
        StringBuilder sb = new StringBuilder(subjectName).append(' ').append(grade);
        if (volume != null && !volume.isBlank()) {
            sb.append(" - ").append(volume.trim());
        }
        sb.append(" - ").append(curriculum);
        return sb.toString();
    }

    /** Admin-only. Permanently deletes the document row, its PDF file, and every {@link SubjectLibraryLink} referencing it (cascade, no blocking rule - same "delete means delete" shape as {@code AdminParentService#deleteCascade}, since a removed textbook naturally removes any Parent's link to it too). */
    public void delete(Long id) {
        LibraryDocument doc = getById(id);

        subjectLibraryLinkRepository.delete().eq(SubjectLibraryLink::getLibraryDocumentId, id).execute();
        libraryDocumentRepository.deleteById(id);
        try {
            Files.deleteIfExists(LIBRARY_DIR.resolve(doc.getFilePath()));
        } catch (IOException e) {
            log().warn("Could not delete library document file {}: {}", doc.getFilePath(), e.getMessage());
        }

        logInfo("Library document deleted: id={}, adminId={}", id, CurrentUser.get().userId());
    }

    /** Admin-only download/view (no ownership concept for Admin - full access, see this class's javadoc). Public, unlike {@link #getById}/{@link #loadFile} below (package-private, reused by Parent/Student after THEIR OWN access checks) - Admin has no separate check to perform first. */
    public LibraryFile downloadForAdmin(Long id) {
        return loadFile(getById(id));
    }

    /** Loads the LibraryDocument with id {@code id} with NO ownership check at all - package-private so {@code ParentLibraryService}/{@code StudentLibraryService} can resolve it after doing their own (different) access checks, same shape as {@code LessonService#getById}. */
    LibraryDocument getById(Long id) {
        LibraryDocument doc = libraryDocumentRepository.findById(id);
        if (doc == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Library document not found");
        }
        return doc;
    }

    /** Reads the document's PDF bytes off disk. Package-private + takes the already-resolved {@link LibraryDocument} (no access check of its own), same reasoning as {@code LessonService#loadImage}. */
    LibraryFile loadFile(LibraryDocument doc) {
        Path path = LIBRARY_DIR.resolve(doc.getFilePath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            logError("Library document file missing on disk: " + path, e);
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Library document file not found");
        }
        return new LibraryFile(bytes, "application/pdf", doc.getFilePath());
    }

    /** Every document linked to {@code subjectId}, newest link first is NOT guaranteed (client sorts, same convention as every other "list everything" endpoint) - package-private, NO ownership check of its own (see this class's javadoc); {@code ParentLibraryService}/{@code StudentLibraryService} call this only after independently proving the caller may see this Subject. */
    List<SubjectLibraryLinkResponse> listLinksForSubject(Long subjectId) {
        List<SubjectLibraryLink> links = subjectLibraryLinkRepository.query().eq(SubjectLibraryLink::getSubjectId, subjectId).list();
        return links.stream()
                .map(link -> SubjectLibraryLinkResponse.from(link, getById(link.getLibraryDocumentId())))
                .toList();
    }

    /** Whether {@code subjectId} is currently linked to {@code documentId} - package-private, used both by {@code ParentLibraryService#link}'s duplicate-check and by the Parent/Student file-download access checks. */
    boolean isLinked(Long subjectId, Long documentId) {
        return subjectLibraryLinkRepository.query()
                .eq(SubjectLibraryLink::getSubjectId, subjectId)
                .eq(SubjectLibraryLink::getLibraryDocumentId, documentId)
                .exists();
    }

    /** Creates the link row - package-private, called only by {@code ParentLibraryService#link} after it has already checked Subject ownership, document existence, and non-duplication. */
    SubjectLibraryLinkResponse createLink(Long subjectId, Long documentId, String linkedBy) {
        LocalDateTime now = LocalDateTime.now();
        SubjectLibraryLink link = new SubjectLibraryLink();
        link.setSubjectId(subjectId);
        link.setLibraryDocumentId(documentId);
        link.setLinkedAt(now);
        link.setLinkedBy(linkedBy);
        link = subjectLibraryLinkRepository.save(link);
        return SubjectLibraryLinkResponse.from(link, getById(documentId));
    }

    /** Removes the link row, throwing {@code COMMON_005 NOT_FOUND} if it doesn't exist - package-private, called only by {@code ParentLibraryService#unlink} after it has already checked Subject ownership. */
    void removeLink(Long subjectId, Long documentId) {
        SubjectLibraryLink link = subjectLibraryLinkRepository.query()
                .eq(SubjectLibraryLink::getSubjectId, subjectId)
                .eq(SubjectLibraryLink::getLibraryDocumentId, documentId)
                .one();
        if (link == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Link not found");
        }
        subjectLibraryLinkRepository.deleteById(link.getId());
    }
}
