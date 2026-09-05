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
 * Student-facing read-only access to documents linked to a Subject (see {@link
 * LibraryService}'s javadoc for the full 3-role model) - per the user's explicit decision that
 * both Parent AND Student can view/download, a Student may see the documents linked to ANY
 * Subject in their OWN classroom ({@link #assertAccessible}), a direct {@code
 * Subject.classroomId == Student.classroomId} check - deliberately simpler than {@code
 * StudentLessonService}'s Test-assignment-based check, since a textbook is reference material for
 * the whole Subject/Classroom, not tied to any specific assigned Test.
 */
@Service
public class StudentLibraryService extends IBase {

    @Autowired
    private LibraryService libraryService;

    @Autowired
    private SubjectService subjectService;

    @Autowired
    private StudentRepository studentRepository;

    /** Documents linked to {@code subjectId} - throws {@code COMMON_004 FORBIDDEN} unless {@code subjectId}'s classroom matches the caller's own classroom. */
    public List<SubjectLibraryLinkResponse> listLinks(Long subjectId) {
        Long studentId = CurrentUser.get().userId();
        assertAccessible(subjectId, studentId);
        return libraryService.listLinksForSubject(subjectId);
    }

    /** Downloads the PDF for {@code documentId}, only if it is linked to {@code subjectId} AND {@code subjectId} is in the caller's own classroom. */
    public LibraryFile downloadFile(Long subjectId, Long documentId) {
        Long studentId = CurrentUser.get().userId();
        assertAccessible(subjectId, studentId);
        if (!libraryService.isLinked(subjectId, documentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Subject is not linked to this document");
        }
        return libraryService.loadFile(libraryService.getById(documentId));
    }

    /** Throws {@code COMMON_004 FORBIDDEN} unless {@code subjectId}'s classroom equals {@code studentId}'s own classroom (no ownership relationship between Student and Subject otherwise). */
    private void assertAccessible(Long subjectId, Long studentId) {
        Subject subject = subjectService.getById(subjectId);
        Student student = studentRepository.findById(studentId);
        if (student == null || !student.getClassroomId().equals(subject.getClassroomId())) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "Subject is not accessible");
        }
    }
}
