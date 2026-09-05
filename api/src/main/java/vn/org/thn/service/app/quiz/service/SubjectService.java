package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.SubjectRequest;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
import vn.org.thn.service.app.quiz.entity.Classroom;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Subject CRUD for the currently logged-in Parent (task 3). Same shape as {@link
 * StudentService}: every method reads {@link CurrentUser#get()} itself, and ownership is enforced
 * here rather than trusted from the caller.
 * <p>
 * {@code delete} blocks when the Subject still has {@link Lesson} children, per the task 3 spec -
 * unlike {@code StudentService#delete} (task 2), this rule is fully implementable now because
 * {@code Lesson} already exists (created in this same task), so there is no "child entity doesn't
 * exist yet" deferral needed here.
 * <p>
 * Subject has no {@code parentId} of its own (see the entity's javadoc, added when Classroom was
 * introduced) - ownership always resolves through {@link ClassroomService#getOwnedOrThrow}, same
 * "child entity, resolve via owner" shape {@link LessonService} already used for Lesson->Subject.
 */
@Service
public class SubjectService extends IBase {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private vn.org.thn.service.app.quiz.repository.ClassroomRepository classroomRepository;

    public SubjectResponse create(SubjectRequest request) {
        Long parentId = CurrentUser.get().userId();
        classroomService.getOwnedOrThrow(request.getClassroomId(), parentId);

        LocalDateTime now = LocalDateTime.now();
        Subject subject = new Subject();
        subject.setClassroomId(request.getClassroomId());
        subject.setName(request.getName());
        subject.setCreatedAt(now);
        subject.setUpdatedAt(now);
        subject.setCreatedBy("parent:" + parentId);
        subject.setUpdatedBy("parent:" + parentId);
        subject = subjectRepository.save(subject);

        logInfo("Subject created: id={}, classroomId={}, parentId={}", subject.getId(), subject.getClassroomId(), parentId);
        return SubjectResponse.from(subject);
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        Long parentId = CurrentUser.get().userId();
        Subject subject = getOwnedOrThrow(id, parentId);
        // classroomId is required on this shared create/update DTO (see SubjectRequest's
        // javadoc), so a normal update call can reassign the subject to a different Classroom -
        // re-validate ownership of the (possibly new) target the same way create() does.
        classroomService.getOwnedOrThrow(request.getClassroomId(), parentId);

        subject.setClassroomId(request.getClassroomId());
        subject.setName(request.getName());
        subject.setUpdatedAt(LocalDateTime.now());
        subject.setUpdatedBy("parent:" + parentId);
        subject = subjectRepository.save(subject);

        logInfo("Subject updated: id={}, parentId={}", subject.getId(), parentId);
        return SubjectResponse.from(subject);
    }

    public SubjectResponse get(Long id) {
        return SubjectResponse.from(getOwnedOrThrow(id, CurrentUser.get().userId()));
    }

    /**
     * Subjects belonging to the current Parent, optionally narrowed to one Classroom -
     * {@code classroomId == null} means "every classroom" (mirrors {@code TestService#list}'s
     * optional {@code studentId} narrowing). Subject has no direct {@code parentId} column
     * (see the entity's javadoc), so the "every classroom" case first resolves every Classroom
     * id owned by this parent, then filters Subject by {@code classroomId IN (...)} - guarded
     * against a parent with zero classrooms, since {@code .in()} is a silent no-op on an empty
     * collection (would otherwise return every OTHER parent's subjects too, a real data leak).
     */
    public List<SubjectResponse> list(Long classroomId) {
        Long parentId = CurrentUser.get().userId();
        if (classroomId != null) {
            classroomService.getOwnedOrThrow(classroomId, parentId);
            return subjectRepository.query().eq(Subject::getClassroomId, classroomId).list()
                    .stream().map(SubjectResponse::from).toList();
        }

        List<Long> classroomIds = classroomRepository.query().eq(Classroom::getParentId, parentId).list()
                .stream().map(Classroom::getId).toList();
        if (classroomIds.isEmpty()) {
            return List.of();
        }
        return subjectRepository.query().in(Subject::getClassroomId, classroomIds).list()
                .stream().map(SubjectResponse::from).toList();
    }

    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Subject subject = getOwnedOrThrow(id, parentId);

        if (lessonRepository.query().eq(Lesson::getSubjectId, subject.getId()).exists()) {
            throw new BusinessException(QuizErrorCode.SUBJECT_HAS_LESSONS);
        }
        subjectRepository.deleteById(subject.getId());
        logInfo("Subject deleted: id={}, parentId={}", subject.getId(), parentId);
    }

    /** Loads the Subject with id {@code id}, throwing if it doesn't exist or its Classroom doesn't belong to {@code parentId}. Also used by {@link LessonService} to resolve a Lesson's indirect owner. */
    Subject getOwnedOrThrow(Long id, Long parentId) {
        Subject subject = getById(id);
        // Resolves ownership through the owning Classroom - throws NOT_FOUND/FORBIDDEN the same
        // way a direct Subject.parentId check would, if that column still existed.
        classroomService.getOwnedOrThrow(subject.getClassroomId(), parentId);
        return subject;
    }

    /** Loads the Subject with id {@code id} with NO ownership check at all. Package-private (2026-09-05) so {@code StudentLibraryService} can resolve it after doing its own (Parent-unrelated) Student->Classroom accessibility check - same "getById + caller does its own check" shape {@code LessonService#getById} already established for {@code StudentLessonService}. */
    Subject getById(Long id) {
        Subject subject = subjectRepository.findById(id);
        if (subject == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Subject not found");
        }
        return subject;
    }
}
