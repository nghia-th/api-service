package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.ClassroomRequest;
import vn.org.thn.service.app.quiz.dto.ClassroomResponse;
import vn.org.thn.service.app.quiz.entity.Classroom;
import vn.org.thn.service.app.quiz.entity.Student;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.ClassroomRepository;
import vn.org.thn.service.app.quiz.repository.StudentRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Classroom CRUD for the currently logged-in Parent - top of the Classroom -> Subject -> Lesson
 * -> Question hierarchy and the target of {@code Student.classroomId} (1 classroom per student).
 * Same shape as {@link SubjectService}: every method reads {@link CurrentUser#get()} itself,
 * ownership enforced here rather than trusted from the caller.
 * <p>
 * {@code delete} blocks when the Classroom still has {@link Student} or {@link Subject} children -
 * same "protect against orphaning/losing data" reasoning as {@code SubjectService#delete}
 * blocking on {@link vn.org.thn.service.app.quiz.entity.Lesson} children.
 */
@Service
public class ClassroomService extends IBase {

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    public ClassroomResponse create(ClassroomRequest request) {
        Long parentId = CurrentUser.get().userId();

        LocalDateTime now = LocalDateTime.now();
        Classroom classroom = new Classroom();
        classroom.setParentId(parentId);
        classroom.setName(request.getName());
        classroom.setCreatedAt(now);
        classroom.setUpdatedAt(now);
        classroom.setCreatedBy("parent:" + parentId);
        classroom.setUpdatedBy("parent:" + parentId);
        classroom = classroomRepository.save(classroom);

        logInfo("Classroom created: id={}, parentId={}", classroom.getId(), parentId);
        return ClassroomResponse.from(classroom);
    }

    public ClassroomResponse update(Long id, ClassroomRequest request) {
        Long parentId = CurrentUser.get().userId();
        Classroom classroom = getOwnedOrThrow(id, parentId);

        classroom.setName(request.getName());
        classroom.setUpdatedAt(LocalDateTime.now());
        classroom.setUpdatedBy("parent:" + parentId);
        classroom = classroomRepository.save(classroom);

        logInfo("Classroom updated: id={}, parentId={}", classroom.getId(), parentId);
        return ClassroomResponse.from(classroom);
    }

    public ClassroomResponse get(Long id) {
        return ClassroomResponse.from(getOwnedOrThrow(id, CurrentUser.get().userId()));
    }

    /** Every Classroom belonging to the current Parent. No paging in v1 - same reasoning as StudentService#list. */
    public List<ClassroomResponse> list() {
        Long parentId = CurrentUser.get().userId();
        return classroomRepository.query().eq(Classroom::getParentId, parentId).list()
                .stream().map(ClassroomResponse::from).toList();
    }

    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Classroom classroom = getOwnedOrThrow(id, parentId);

        if (studentRepository.query().eq(Student::getClassroomId, classroom.getId()).exists()) {
            throw new BusinessException(QuizErrorCode.CLASSROOM_HAS_STUDENTS);
        }
        if (subjectRepository.query().eq(Subject::getClassroomId, classroom.getId()).exists()) {
            throw new BusinessException(QuizErrorCode.CLASSROOM_HAS_SUBJECTS);
        }
        classroomRepository.deleteById(classroom.getId());
        logInfo("Classroom deleted: id={}, parentId={}", classroom.getId(), parentId);
    }

    /** Loads the Classroom with id {@code id}, throwing if it doesn't exist or doesn't belong to {@code parentId}. Package-private so {@code StudentService}/{@code SubjectService} can reuse it, same pattern as {@code SubjectService#getOwnedOrThrow}. */
    Classroom getOwnedOrThrow(Long id, Long parentId) {
        Classroom classroom = classroomRepository.findById(id);
        if (classroom == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Classroom not found");
        }
        if (!classroom.getParentId().equals(parentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This classroom does not belong to the current parent");
        }
        return classroom;
    }
}
