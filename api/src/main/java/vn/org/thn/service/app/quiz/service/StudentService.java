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
 * {@code DELETE} does a hard delete in v1 - {@code Test}/{@code Attempt} entities do not exist
 * yet (task 5/6), so there is nothing to check before removing a Student's row. Once task 6 adds
 * {@code Attempt}, this should be revisited to block deletion of a Student that already has
 * attempts (per the task 2 doc's own note); confirmed with the user to defer that check rather
 * than block task 2 on entities that don't exist yet.
 */
@Service
public class StudentService extends IBase {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private ClassroomService classroomService;

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
