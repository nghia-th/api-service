package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.LibraryFile;
import vn.org.thn.service.app.quiz.dto.SubjectLibraryLinkResponse;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.util.List;

/**
 * Student-facing read-only access to entries linked to a Subject (see {@link LibraryService}'s
 * javadoc for the full 3-role model) - a Student may see the entries linked to ANY Subject in
 * their OWN classroom ({@link #assertAccessible}), a direct {@code Subject.classroomId ==
 * Student.classroomId} check - deliberately simpler than {@code StudentLessonService}'s
 * Test-assignment-based check, since library material is reference material for the whole
 * Subject/Classroom, not tied to any specific assigned Test.
 */
@Service
public class StudentLibraryService extends IBase {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private StudentRepository studentRepository;

    /** Entries linked to {@code subjectId} - throws {@code COMMON_004 FORBIDDEN} unless {@code subjectId}'s classroom matches the caller's own classroom. */
    public List<SubjectLibraryLinkResponse> listLinks(Long subjectId) {
        Long studentId = CurrentUser.get().userId();
        assertAccessible(subjectId, studentId);
        return libraryService.listLinksForSubject(subjectId);
    }

    /** Downloads one file ({@code fileId}, must belong to {@code documentId}), only if {@code documentId} is linked to {@code subjectId} AND {@code subjectId} is in the caller's own classroom. Revision 2026-09-06: takes {@code fileId} now that a document can carry multiple files. */
    public LibraryFile downloadFile(Long subjectId, Long documentId, Long fileId) {
        Long studentId = CurrentUser.get().userId();
        assertAccessible(subjectId, studentId);
        if (!libraryService.isLinked(subjectId, documentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Subject is not linked to this document");
        }
        return libraryService.loadFile(libraryService.getFileOrThrow(documentId, fileId));
    }

    /** Throws {@code COMMON_004 FORBIDDEN} unless {@code subjectId}'s classroom equals {@code studentId}'s own classroom (no ownership relationship between Student and Subject otherwise). */
    private void assertAccessible(Long subjectId, Long studentId) {
        Subject subject = subjectService.getById(subjectId);
        Student student = studentRepository.findById(studentId);
        // A SHARED Subject (classroomId == null) is accessible from every Classroom of its own
        // Parent, this Student's classroom included - see Subject's javadoc. A shared Subject
        // still carries its owning parentId, so that check still guards against a Student
        // reaching another Parent's shared Subject.
        boolean accessible = student != null && subject.getParentId().equals(student.getParentId())
                && (subject.getClassroomId() == null || subject.getClassroomId().equals(student.getClassroomId()));
        if (!accessible) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Subject is not accessible");
        }
    }
}
