package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentFileResponse;
import vn.org.thn.service.app.quiz.dto.LibraryDocumentResponse;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.SubjectLibraryLinkResponse;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.List;

/**
 * Parent-facing access to the Admin-managed library (see {@link LibraryService}'s javadoc for the
 * full 3-role model) - a Parent browses the WHOLE catalog read-only ({@link #browse}), then
 * links/unlinks their OWN {@code Subject} rows to entries ({@link #link}/{@link #unlink},
 * many-to-many), and can only download a FILE for a Subject they own AND have linked to that
 * entry ({@link #downloadFile}).
 * <p>
 * Every method here resolves Subject ownership itself via {@code SubjectService#getOwnedOrThrow}
 * before touching {@link LibraryService}'s package-private methods - same "caller proves access,
 * then calls the shared low-level service" shape {@code StudentLessonService} already uses for
 * Lesson.
 */
@Service
public class ParentLibraryService extends IBase {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private SubjectService subjectService;

    /** Whole catalog, read-only, no ownership filtering - a Parent needs to see every entry to decide what to link (see {@link LibraryService#list}'s javadoc). */
    public List<LibraryDocumentResponse> browse(Integer grade, String subjectName, String curriculum) {
        return libraryService.list(grade, subjectName, curriculum);
    }

    /** Entries currently linked to {@code subjectId} - throws if the Subject isn't owned by the caller. */
    public List<SubjectLibraryLinkResponse> listLinks(Long subjectId) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);
        return libraryService.listLinksForSubject(subjectId);
    }

    /** Links {@code subjectId} (must be owned by the caller) to {@code documentId} (must exist) - throws {@code QUIZ_035 LIBRARY_ALREADY_LINKED} if the pair is already linked. */
    public SubjectLibraryLinkResponse link(Long subjectId, Long documentId) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);
        libraryService.getById(documentId);
        if (libraryService.isLinked(subjectId, documentId)) {
            throw new BusinessException(QuizErrorCode.LIBRARY_ALREADY_LINKED);
        }
        SubjectLibraryLinkResponse response = libraryService.createLink(subjectId, documentId, "parent:" + parentId);
        logInfo("Library document linked: subjectId={}, documentId={}, parentId={}", subjectId, documentId, parentId);
        return response;
    }

    /** Removes the link between {@code subjectId} (must be owned by the caller) and {@code documentId}. */
    public void unlink(Long subjectId, Long documentId) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);
        libraryService.removeLink(subjectId, documentId);
        logInfo("Library document unlinked: subjectId={}, documentId={}, parentId={}", subjectId, documentId, parentId);
    }

    /**
     * Downloads one file ({@code fileId}, must belong to {@code documentId}) - only allowed if
     * {@code subjectId} is owned by the caller AND already linked to {@code documentId}. Revision
     * 2026-09-06: takes {@code fileId} now that a document can carry multiple files (see {@link
     * LibraryService}'s javadoc) - {@link #listLinks} already returns each linked document's
     * {@link LibraryDocumentFileResponse} list so the caller knows which ids to pass here.
     */
    public LibraryFile downloadFile(Long subjectId, Long documentId, Long fileId) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);
        if (!libraryService.isLinked(subjectId, documentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Subject is not linked to this document");
        }
        return libraryService.loadFile(libraryService.getFileOrThrow(documentId, fileId));
    }
}
