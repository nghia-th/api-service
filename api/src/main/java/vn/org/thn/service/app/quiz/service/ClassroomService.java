package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.BulkDeleteResponse;
import vn.org.thn.service.app.quiz.dto.ClassroomRequest;
import vn.org.thn.service.app.quiz.dto.ClassroomResponse;
import vn.org.thn.service.app.quiz.entity.Classroom;
import vn.org.thn.service.app.quiz.repository.ClassroomRepository;
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
 * <b>Revision 2026-09-06:</b> {@code delete} used to BLOCK outright when the Classroom still had
 * any {@code Student} or {@code Subject} ({@code CLASSROOM_HAS_STUDENTS}/{@code
 * CLASSROOM_HAS_SUBJECTS}) - per the user's explicit choice (confirmed via AskUserQuestion,
 * "Xoa ca Hoc sinh trong lop"), it now cascades instead: every Subject/Lesson/Question/Test under
 * it AND every Student in it (with that Student's own full data) are deleted along with the
 * Classroom - see {@link CascadeDeleteService#deleteClassroomCascade}. Same "cascade instead of
 * block" rule already applied one level down by {@code SubjectService#delete}/{@code
 * LessonService#delete}/{@code QuestionService#delete}/{@code TestService#delete}.
 */
@Service
public class ClassroomService extends IBase {

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private CascadeDeleteService cascadeDeleteService;

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

    /**
     * Deletes this Classroom and, per the 2026-09-06 revision, everything that depends on it -
     * see {@link CascadeDeleteService#deleteClassroomCascade}. The actual row deletion (Subjects,
     * Lessons, Questions, Tests, Students and all of their own data, ...) happens there; this
     * method only does the ownership check first, same "auth here, cascade there" split as
     * {@code SubjectService#delete}/{@code LessonService#delete}/{@code QuestionService#delete}.
     */
    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Classroom classroom = getOwnedOrThrow(id, parentId);
        cascadeDeleteService.deleteClassroomCascade(classroom.getId());
        logInfo("Classroom deleted (cascade): id={}, parentId={}", classroom.getId(), parentId);
    }

    /**
     * Bulk delete (2026-09-06, "xoa lop") - deletes each of {@code ids} via this class's own
     * {@link #delete}, so every id gets the same ownership check + cascade as a single delete.
     * Best-effort: one id failing (wrong owner, already gone, ...) does not stop the rest - see
     * {@link BulkDeleteSupport}.
     */
    public BulkDeleteResponse deleteMany(List<Long> ids) {
        return BulkDeleteSupport.deleteEach(ids, this::delete);
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
