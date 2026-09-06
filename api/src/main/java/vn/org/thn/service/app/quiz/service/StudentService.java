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
 * <b>Revision 2026-09-06 (e):</b> {@code DELETE} is a hard delete that now cascades away
 * EVERYTHING belonging to this Student first - see {@link
 * CascadeDeleteService#deleteStudentDataCascade} for the full list ({@code TimetableEntry},
 * {@code LessonPreparation}, {@code LessonReport}, and every {@code Test}/{@code Attempt}/{@code
 * AttemptAnswer} assigned to them). Earlier revisions only cleaned up Timetable/LessonPreparation
 * and left Test/Attempt/LessonReport as a known, flagged gap (deleting a Student with any of
 * those would throw a live foreign-key-constraint-violation) - per the user's explicit choice
 * now ("xoá học sinh ... anh muốn có popup xác nhận và cảnh báo nếu xoá thì sẽ xoá hết dữ liệu
 * của học sinh này và nếu đồng ý sẽ xoá hết" - a confirmation popup warning this permanently
 * deletes ALL of the Student's data, and go ahead and delete everything once confirmed), this
 * gap is now closed: every Student delete is unconditionally a full, permanent wipe of that
 * Student's data. The confirmation popup itself is frontend-only (see {@code
 * BlocParentStudents.ts}/{@code Students.tsx}) - this endpoint does not ask for confirmation
 * again and does not accept a "keep history" option; by the time this is called, the Parent has
 * already confirmed.
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
        cascadeDeleteService.deleteStudentDataCascade(student.getId());
        studentRepository.deleteById(student.getId());
        logInfo("Student deleted (cascade - all data): id={}, parentId={}", student.getId(), parentId);
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
