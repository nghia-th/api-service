package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentFileResponse;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentResponse;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.SubjectLibraryLinkResponse;
import vn.org.thn.service.app.quiz.entity.Curriculum;
import vn.org.thn.service.app.quiz.entity.LibraryDocument;
import vn.org.thn.service.app.quiz.entity.LibraryDocumentFile;
import vn.org.thn.service.app.quiz.entity.SubjectLibraryLink;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.CurriculumRepository;
import vn.org.thn.service.app.quiz.repository.LibraryDocumentFileRepository;
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
 * Admin-managed library (2026-09-05, "thu vien sach giao khoa" feature; extended 2026-09-06 per
 * the user's explicit choice - see {@link LibraryDocument}'s javadoc): an Admin creates entries
 * organized by an optional {@code grade} (1-12) and {@code curriculum} (a name from the
 * Admin-managed {@link Curriculum} list) - a general "mon hoc" not tied to any grade/curriculum
 * (e.g. "Lap trinh Python") simply leaves both blank - plus a required {@code subjectName} and an
 * optional {@code volume}. Each entry can carry any number of files (PDF or PowerPoint), added
 * and removed one at a time after the entry itself is created.
 * <p>
 * ACCESS MODEL (3 roles, mirroring {@code LessonService}/{@code StudentLessonService}'s existing
 * "one shared file-serving method, per-role access check" shape):
 * <ul>
 *     <li><b>Admin</b> ({@code AdminLibraryApi}, this class) - full CRUD over the whole library,
 *     no root restriction (unlike {@code AdminManageApi}'s Admin-manages-Admin feature - every
 *     Admin can manage it).</li>
 *     <li><b>Parent</b> ({@code ParentLibraryApi}/{@code ParentLibraryService}) - browses the
 *     WHOLE library read-only (to decide what to link, {@link #list}), then links/unlinks their
 *     OWN {@link vn.org.thn.service.app.quiz.entity.Subject} rows to entries (many-to-many) via
 *     {@link vn.org.thn.service.app.quiz.entity.SubjectLibraryLink} - a file is only reachable for
 *     a Subject the Parent owns AND has linked.</li>
 *     <li><b>Student</b> ({@code StudentLibraryApi}/{@code StudentLibraryService}) - read-only,
 *     can view/download a file linked to any Subject in their OWN classroom.</li>
 * </ul>
 * {@link #getById}/{@link #getFileOrThrow}/{@link #loadFile}/{@link #listLinksForSubject}/{@link
 * #isLinked}/{@link #toResponse} are package-private with NO ownership check of their own, reused
 * by {@code ParentLibraryService}/{@code StudentLibraryService} once each has independently proven
 * (its own, different) access rule - same "shared low-level method, per-role check happens one
 * layer up" shape as {@code LessonService#getById}.
 * <p>
 * FILE STORAGE: same convention as {@code LessonService}'s {@code IMAGE_DIR} - only the
 * server-generated filename lives in {@code LibraryDocumentFile.filePath}, the actual bytes live
 * under {@link #LIBRARY_DIR} (a service-local folder, per {@code DatabasePath}'s own javadoc
 * guidance not to extend that class for app-specific upload folders).
 */
@Service
public class LibraryService extends IBase {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final Map<String, String> ALLOWED_FILE_TYPES = Map.of(
            "application/pdf", "pdf",
            "application/vnd.ms-powerpoint", "ppt",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"
    );
    private static final Path LIBRARY_DIR = DatabasePath.HOME.resolve("uploads").resolve("library");

    private static final int MIN_GRADE = 1;
    private static final int MAX_GRADE = 12;

    @Autowired
    private LibraryDocumentRepository libraryDocumentRepository;

    @Autowired
    private LibraryDocumentFileRepository libraryDocumentFileRepository;

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
                .stream().map(this::toResponse).toList();
    }

    /**
     * Admin-only. Creates a library document row with NO file yet - files are attached
     * afterward, one at a time, via {@link #addFile} (a document can carry any number of them,
     * see this class's javadoc). {@code grade}/{@code curriculum} are validated only when
     * provided (both null is a valid, general "mon hoc" entry - see {@link
     * LibraryDocument}'s javadoc, revision 2026-09-06).
     */
    public LibraryDocumentResponse create(Integer grade, String subjectName, String curriculum, String volume, String title) {
        validateTaxonomy(grade, curriculum);
        LibraryDocument doc = buildAndSaveRow(grade, subjectName, curriculum, volume, title);
        logInfo("Library document created: id={}, grade={}, subjectName={}, curriculum={}, adminId={}",
                doc.getId(), grade, subjectName, curriculum, CurrentUser.get().userId());
        return toResponse(doc);
    }

    /**
     * Whether a NON-DELETED row already has this exact grade+subjectName+curriculum+volume
     * combination - used only by {@code LibraryImportService} to reject a duplicate row during
     * import. {@code eq(field, null)} is a NO-OP in this query builder (means "don't filter on
     * this field" - see {@link #list}'s intentional use of that for optional search filters), so
     * a null grade/curriculum/volume must use {@code isNull()} explicitly here, same fix already
     * applied to volume before this revision - otherwise a row with no grade/curriculum would be
     * treated as a duplicate of ANY other row's grade/curriculum instead of specifically "none".
     */
    boolean existsExact(Integer grade, String subjectName, String curriculum, String volume) {
        String normalizedVolume = (volume == null || volume.isBlank()) ? null : volume.trim();
        var query = libraryDocumentRepository.query().eq(LibraryDocument::getSubjectName, subjectName);
        query = grade == null ? query.isNull(LibraryDocument::getGrade) : query.eq(LibraryDocument::getGrade, grade);
        query = curriculum == null ? query.isNull(LibraryDocument::getCurriculum) : query.eq(LibraryDocument::getCurriculum, curriculum);
        query = normalizedVolume == null ? query.isNull(LibraryDocument::getVolume) : query.eq(LibraryDocument::getVolume, normalizedVolume);
        return query.exists();
    }

    /**
     * Admin-only. Adds one more file to an existing document - unlike the pre-2026-09-06
     * {@code attachFile} this replaced, this never deletes a previous file (a document can carry
     * any number of them now); removing one is a separate explicit action, see {@link
     * #removeFile}.
     */
    public LibraryDocumentFileResponse addFile(Long documentId, MultipartFile file) {
        LibraryDocument doc = getById(documentId);
        String filename = saveFileOrThrow(file);
        Long adminId = CurrentUser.get().userId();
        LocalDateTime now = LocalDateTime.now();

        LibraryDocumentFile row = new LibraryDocumentFile();
        row.setLibraryDocumentId(doc.getId());
        row.setOriginalName(originalNameOrDefault(file));
        row.setFilePath(filename);
        row.setFileSize(file.getSize());
        row.setContentType(file.getContentType());
        row.setUploadedAt(now);
        row.setUploadedBy("admin:" + adminId);
        row = libraryDocumentFileRepository.save(row);

        logInfo("Library document file added: documentId={}, fileId={}, adminId={}", documentId, row.getId(), adminId);
        return LibraryDocumentFileResponse.from(row);
    }

    /** Admin-only. Deletes one file (and its bytes on disk) from a document - the document row itself and its other files are untouched. */
    public void removeFile(Long documentId, Long fileId) {
        LibraryDocumentFile file = getFileOrThrow(documentId, fileId);
        libraryDocumentFileRepository.deleteById(fileId);
        try {
            Files.deleteIfExists(LIBRARY_DIR.resolve(file.getFilePath()));
        } catch (IOException e) {
            log().warn("Could not delete library file {}: {}", file.getFilePath(), e.getMessage());
        }
        logInfo("Library document file removed: documentId={}, fileId={}, adminId={}", documentId, fileId, CurrentUser.get().userId());
    }

    /** Throws {@link QuizErrorCode#LIBRARY_INVALID_TAXONOMY} if a provided grade is outside 1-12 or a provided curriculum is not a known name from the Admin-managed Curriculum list. Both null is always valid (a general "mon hoc" entry, see {@link LibraryDocument}'s javadoc). */
    private void validateTaxonomy(Integer grade, String curriculum) {
        if (!isValidTaxonomy(grade, curriculum)) {
            throw new BusinessException(QuizErrorCode.LIBRARY_INVALID_TAXONOMY);
        }
    }

    /**
     * Same check as {@link #validateTaxonomy} but returns a boolean instead of throwing - used by
     * {@code LibraryImportService} so a single bad row can be reported as a per-row error and
     * skipped. Revision 2026-09-06: a null grade/curriculum is always valid on its own (skips that
     * half of the check) - only a NON-null value still has to be in range/known.
     */
    boolean isValidTaxonomy(Integer grade, String curriculum) {
        boolean gradeOk = grade == null || (grade >= MIN_GRADE && grade <= MAX_GRADE);
        boolean curriculumOk = curriculum == null || curriculumRepository.query().eq(Curriculum::getName, curriculum).exists();
        return gradeOk && curriculumOk;
    }

    /** Validates {@code file} against {@link #ALLOWED_FILE_TYPES}/{@link #MAX_FILE_SIZE_BYTES} and writes it to {@link #LIBRARY_DIR} under a fresh random filename, returning that filename (never the original one - same convention as every other upload in this codebase). */
    private String saveFileOrThrow(MultipartFile file) {
        String extension = ALLOWED_FILE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(QuizErrorCode.LIBRARY_PDF_INVALID_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
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
            logError("Could not save library document file to " + target, e);
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
        }
        return filename;
    }

    private static String originalNameOrDefault(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name == null || name.isBlank()) ? "file" : name;
    }

    /** Builds and saves one {@link LibraryDocument} row - no file fields anymore (revision 2026-09-06, see this class's javadoc). */
    private LibraryDocument buildAndSaveRow(Integer grade, String subjectName, String curriculum, String volume, String title) {
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
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        doc.setCreatedBy(actor);
        doc.setUpdatedBy(actor);
        return libraryDocumentRepository.save(doc);
    }

    /** e.g. "Toán 4 - Tập 1 - Kết nối tri thức", or just "Lập trình Python" when grade/volume/curriculum are all absent - only used when the Admin leaves the title field blank. */
    private String defaultTitle(Integer grade, String subjectName, String volume, String curriculum) {
        StringBuilder sb = new StringBuilder(subjectName);
        if (grade != null) {
            sb.append(' ').append(grade);
        }
        if (volume != null && !volume.isBlank()) {
            sb.append(" - ").append(volume.trim());
        }
        if (curriculum != null && !curriculum.isBlank()) {
            sb.append(" - ").append(curriculum);
        }
        return sb.toString();
    }

    /** Admin-only. Permanently deletes the document row, every one of its files (rows + bytes on disk), and every {@link SubjectLibraryLink} referencing it (cascade, no blocking rule - same "delete means delete" shape as {@code AdminParentService#deleteCascade}). */
    public void delete(Long id) {
        getById(id);
        List<LibraryDocumentFile> files = libraryDocumentFileRepository.query().eq(LibraryDocumentFile::getLibraryDocumentId, id).list();
        for (LibraryDocumentFile file : files) {
            try {
                Files.deleteIfExists(LIBRARY_DIR.resolve(file.getFilePath()));
            } catch (IOException e) {
                log().warn("Could not delete library document file {}: {}", file.getFilePath(), e.getMessage());
            }
        }
        libraryDocumentFileRepository.delete().eq(LibraryDocumentFile::getLibraryDocumentId, id).execute();
        subjectLibraryLinkRepository.delete().eq(SubjectLibraryLink::getLibraryDocumentId, id).execute();
        libraryDocumentRepository.deleteById(id);

        logInfo("Library document deleted: id={}, filesDeleted={}, adminId={}", id, files.size(), CurrentUser.get().userId());
    }

    /** Admin-only download/view (no ownership concept for Admin - full access, see this class's javadoc). Public, unlike {@link #getFileOrThrow}/{@link #loadFile} below (package-private, reused by Parent/Student after THEIR OWN access checks) - Admin has no separate check to perform first. */
    public LibraryFile downloadForAdmin(Long documentId, Long fileId) {
        return loadFile(getFileOrThrow(documentId, fileId));
    }

    /** Loads the LibraryDocument with id {@code id} with NO ownership check at all - package-private so {@code ParentLibraryService}/{@code StudentLibraryService} can resolve it after doing their own (different) access checks, same shape as {@code LessonService#getById}. */
    LibraryDocument getById(Long id) {
        LibraryDocument doc = libraryDocumentRepository.findById(id);
        if (doc == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Library document not found");
        }
        return doc;
    }

    /** Loads the file with id {@code fileId}, throwing NOT_FOUND unless it exists AND belongs to {@code documentId} - package-private, no ownership check of {@code documentId} itself (see this class's javadoc). */
    LibraryDocumentFile getFileOrThrow(Long documentId, Long fileId) {
        LibraryDocumentFile file = libraryDocumentFileRepository.findById(fileId);
        if (file == null || !file.getLibraryDocumentId().equals(documentId)) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Library document file not found");
        }
        return file;
    }

    /** Reads the file's bytes off disk. Package-private + takes the already-resolved {@link LibraryDocumentFile} (no access check of its own), same reasoning as {@code LessonService#loadImage}. */
    LibraryFile loadFile(LibraryDocumentFile file) {
        Path path = LIBRARY_DIR.resolve(file.getFilePath());
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            logError("Library document file missing on disk: " + path, e);
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Library document file not found");
        }
        return new LibraryFile(bytes, file.getContentType(), file.getOriginalName());
    }

    /** Every file belonging to {@code documentId}, in no guaranteed order - package-private, used by {@link #toResponse} and reused directly by {@code AdminLibraryApi} for the file list under one document. */
    List<LibraryDocumentFileResponse> listFiles(Long documentId) {
        return libraryDocumentFileRepository.query().eq(LibraryDocumentFile::getLibraryDocumentId, documentId).list()
                .stream().map(LibraryDocumentFileResponse::from).toList();
    }

    /** Builds the full response for one document, including its file list - package-private, the only place {@link LibraryDocumentResponse#from} is called from (every list/get/link path in this feature goes through here so the file list is never forgotten). */
    LibraryDocumentResponse toResponse(LibraryDocument doc) {
        return LibraryDocumentResponse.from(doc, listFiles(doc.getId()));
    }

    /** Every document linked to {@code subjectId}, newest link first is NOT guaranteed (client sorts, same convention as every other "list everything" endpoint) - package-private, NO ownership check of its own (see this class's javadoc); {@code ParentLibraryService}/{@code StudentLibraryService} call this only after independently proving the caller may see this Subject. */
    List<SubjectLibraryLinkResponse> listLinksForSubject(Long subjectId) {
        List<SubjectLibraryLink> links = subjectLibraryLinkRepository.query().eq(SubjectLibraryLink::getSubjectId, subjectId).list();
        return links.stream()
                .map(link -> SubjectLibraryLinkResponse.from(link, toResponse(getById(link.getLibraryDocumentId()))))
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
        return SubjectLibraryLinkResponse.from(link, toResponse(getById(documentId)));
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
