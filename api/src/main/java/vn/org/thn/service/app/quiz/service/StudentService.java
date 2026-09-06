package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import vn.org.thn.service.app.quiz.dto.StudentCreateRequest;
import vn.org.thn.service.app.quiz.dto.StudentResponse;
import vn.org.thn.service.app.quiz.dto.StudentUpdateRequest;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Student CRUD for the currently logged-in Parent (task 2). Every method reads {@link
 * CurrentUser#get()} itself rather than taking a {@code parentId} parameter, so the ownership
 * check can never accidentally be skipped by a caller forgetting to pass one - see {@code
 * docs/dev/02-quan-ly-ho-so-con.md} acceptance criteria: Parent A must never be able to
 * read/update/delete a Student belonging to Parent B, even when it knows that student's id.
 * <p>
 * {@code DELETE} does a hard delete, with SOME cascade cleanup added 2026-09-06 (see {@link
 * CascadeDeleteService#deleteStudentTimetableDataCascade}): {@code TimetableEntry} and {@code
 * LessonPreparation} rows for this Student are removed first, otherwise the delete below would
 * throw a foreign-key-constraint-violation the moment a Student with a timetable had a real
 * {@code student_id} foreign key added to {@code timetable_entry} (V7 migration). {@code Test}/
 * {@code Attempt}/{@code LessonReport} rows are NOT cleaned up here - this codebase's original
 * v1 note below (deferred because those entities didn't exist yet) is now stale (they do exist,
 * and DO have their own {@code REFERENCES student(id)} foreign keys), so deleting a Student that
 * already has any Test/Attempt/LessonReport row will still throw today - a pre-existing bug,
 * flagged to the user but deliberately NOT fixed as part of this change (unlike the Timetable
 * tables above, it was not made worse by this change, and fixing it means permanently losing a
 * Student's grading history, a materially bigger decision that deserves its own confirmation).
 */
@Service
public class StudentService extends IBase {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private CascadeDeleteService cascadeDeleteService;

    public StudentResponse create(StudentCreateRequest request) {
        Long parentId = CurrentUser.get().userId();
        ensureUsernameAvailable(request.getUsername(), null);
        classroomService.getOwnedOrThrow(request.getClassroomId(), parentId);

        LocalDateTime now = LocalDateTime.now();
        Student student = new Student();
        student.setParentId(parentId);
        student.setFullName(request.getFullName());
        student.setClassroomId(request.getClassroomId());
        student.setUsername(request.getUsername());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setCreatedAt(now);
        student.setUpdatedAt(now);
        student.setCreatedBy("parent:" + parentId);
        student.setUpdatedBy("parent:" + parentId);
        student = studentRepository.save(student);

        logInfo("Student created: id={}, parentId={}, username={}", student.getId(), parentId, student.getUsername());
        return StudentResponse.from(student);
    }

    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Long parentId = CurrentUser.get().userId();
        Student student = getOwnedOrThrow(id, parentId);

        // Every field is optional (see StudentUpdateRequest) - null, and for fullName/grade/
        // username also blank, means "leave unchanged". Password is the exception: a blank
        // password is rejected by @Size before this method ever runs, so here it is only ever
        // null (unchanged) or a valid new password.
        if (StringUtils.hasText(request.getFullName())) {
            student.setFullName(request.getFullName());
        }
        if (request.getClassroomId() != null) {
            classroomService.getOwnedOrThrow(request.getClassroomId(), parentId);
            student.setClassroomId(request.getClassroomId());
        }
        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(student.getUsername())) {
            ensureUsernameAvailable(request.getUsername(), student.getId());
            student.setUsername(request.getUsername());
        }
        if (request.getPassword() != null) {
            student.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        student.setUpdatedAt(LocalDateTime.now());
        student.setUpdatedBy("parent:" + parentId);
        student = studentRepository.save(student);

        logInfo("Student updated: id={}, parentId={}", student.getId(), parentId);
        return StudentResponse.from(student);
    }

    public StudentResponse get(Long id) {
        Student student = getOwnedOrThrow(id, CurrentUser.get().userId());
        return StudentResponse.from(student);
    }

    /** Every Student belonging to the current Parent. No paging in v1 - a family's number of children is always small (see task 2 doc). */
    public List<StudentResponse> list() {
        Long parentId = CurrentUser.get().userId();
        return studentRepository.query().eq(Student::getParentId, parentId).list()
                .stream().map(StudentResponse::from).toList();
    }

    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Student student = getOwnedOrThrow(id, parentId);
        cascadeDeleteService.deleteStudentTimetableDataCascade(student.getId());
        studentRepository.deleteById(student.getId());
        logInfo("Student deleted: id={}, parentId={}", student.getId(), parentId);
    }

    /** Loads the Student with id {@code id}, throwing if it doesn't exist or doesn't belong to {@code parentId}. Package-private (not private) so {@code TestService} (task 5) can reuse it, same pattern as {@code SubjectService#getOwnedOrThrow}. */
    Student getOwnedOrThrow(Long id, Long parentId) {
        Student student = studentRepository.findById(id);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }
        if (!student.getParentId().equals(parentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This student does not belong to the current parent");
        }
        return student;
    }

    /**
     * Loads the Student with id {@code id}, throwing if it doesn't exist - deliberately NO
     * ownership check against a Parent (unlike {@link #getOwnedOrThrow}). Only safe to call from a
     * Student-facing self-service flow where {@code id} is ALWAYS {@code CurrentUser.get().userId()}
     * itself, never a caller-supplied id (2026-09-06, "hoc sinh tao thoi khoa bieu" - Student
     * self-service ADD on their own {@code TimetableEntry}, see {@code
     * TimetableService#addOwnEntry}) - a Student has no "owned by" relationship to check the way a
     * Parent does, they simply access their own single row.
     */
    Student getSelfOrThrow(Long id) {
        Student student = studentRepository.findById(id);
        if (student == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Student not found");
        }
        return student;
    }

    /** Throws {@link QuizErrorCode#USERNAME_TAKEN} if {@code username} is already used by a student other than {@code excludeStudentId}. Pass null for {@code excludeStudentId} on create. */
    private void ensureUsernameAvailable(String username, Long excludeStudentId) {
        // .ne() is a no-op when the value is null (see BaseConditionBuilder), so this single
        // query works unchanged for both create (excludeStudentId == null) and update.
        boolean taken = studentRepository.query()
                .eq(Student::getUsername, username)
                .ne(Student::getId, excludeStudentId)
                .exists();
        if (taken) {
            throw new BusinessException(QuizErrorCode.USERNAME_TAKEN);
        }
    }
}
