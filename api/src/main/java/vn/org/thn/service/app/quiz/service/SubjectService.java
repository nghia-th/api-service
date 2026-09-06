package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.SubjectRequest;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.app.quiz.repository.SubjectRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Subject CRUD for the currently logged-in Parent (task 3). Same shape as {@link
 * StudentService}: every method reads {@link CurrentUser#get()} itself, and ownership is enforced
 * here rather than trusted from the caller.
 * <p>
 * <b>Revision 2026-09-06 (a):</b> {@code delete} used to BLOCK outright when the Subject still had
 * {@code Lesson} children (task 3's original rule). Per the user's explicit request ("phu huynh
 * can duoc xoa cac du lieu nhu mon hoc, bai hoc, de on, neu cac du lieu do duoc lien ket voi hoc
 * sinh thi xoa luon nhung du lieu lien quan"), it now cascades instead - see {@link
 * CascadeDeleteService}'s javadoc for the full cascade design (every Lesson/Question/Test that
 * depends on this Subject is deleted with it, including any Test's Attempt/score history).
 * <p>
 * <b>Revision 2026-09-06 (c):</b> per the user's request ("anh có 1 môn học mà không thuộc lớp
 * nào... các học sinh của phụ huynh đều học môn này không phân biệt lớp"), a Subject can now be
 * SHARED across every Classroom of a Parent instead of pinned to exactly one - see {@link
 * Subject}'s javadoc. This added a direct {@code parentId} column to Subject (previously
 * ownership only resolved indirectly through the owning Classroom, since Subject had no column
 * of its own), and made {@code classroomId} optional: null on the request/entity means "every
 * Classroom of this Parent". Every method below that used to resolve ownership via {@code
 * classroomService.getOwnedOrThrow(subject.getClassroomId(), ...)} now checks {@code
 * subject.getParentId()} directly instead - the same "child entity, direct parentId column"
 * shape {@link StudentService}/{@link ClassroomService} already use, needed because a shared
 * Subject has no single owning Classroom left to resolve through.
 */
@Service
public class SubjectService extends IBase {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private ClassroomService classroomService;

    @Autowired
    private CascadeDeleteService cascadeDeleteService;

    public SubjectResponse create(SubjectRequest request) {
        Long parentId = CurrentUser.get().userId();
        if (request.getClassroomId() != null) {
            classroomService.getOwnedOrThrow(request.getClassroomId(), parentId);
        }

        LocalDateTime now = LocalDateTime.now();
        Subject subject = new Subject();
        subject.setParentId(parentId);
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
        // classroomId is now optional on this shared create/update DTO (null = shared across
        // every Classroom) - only re-validate ownership of the (possibly new) target Classroom
        // when one is actually given.
        if (request.getClassroomId() != null) {
            classroomService.getOwnedOrThrow(request.getClassroomId(), parentId);
        }

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
     * {@code classroomId == null} means "every classroom" and now simply queries by the
     * Subject's own direct {@code parentId} column (Revision 2026-09-06 (c) - previously had to
     * resolve every owned Classroom id first and filter by {@code classroomId IN (...)}, since
     * Subject had no {@code parentId} of its own).
     * <p>
     * When {@code classroomId} is given, a SHARED Subject ({@code classroomId == null} on the
     * row) must still be included - it applies to every Classroom of this Parent, this one
     * included - so the query is {@code parentId = :parentId AND (classroom_id = :classroomId OR
     * classroom_id IS NULL)}. The OR needs to stay inside its own parentheses so it doesn't spill
     * into the surrounding {@code parentId} check (the query builder joins WHERE fragments with
     * plain AND/OR and does not add grouping on its own), hence the single {@code raw()} fragment
     * below instead of separate {@code eq}/{@code isNull}/{@code orEq} calls.
     */
    public List<SubjectResponse> list(Long classroomId) {
        Long parentId = CurrentUser.get().userId();
        if (classroomId != null) {
            classroomService.getOwnedOrThrow(classroomId, parentId);
            return subjectRepository.query()
                    .eq(Subject::getParentId, parentId)
                    .raw("(classroom_id = #{cid} OR classroom_id IS NULL)", Map.of("cid", classroomId))
                    .list()
                    .stream().map(SubjectResponse::from).toList();
        }

        return subjectRepository.query().eq(Subject::getParentId, parentId).list()
                .stream().map(SubjectResponse::from).toList();
    }

    /**
     * Deletes this Subject and, per the 2026-09-06 (a) revision, everything that depends on it -
     * see {@link CascadeDeleteService#deleteSubjectCascade}. The actual row deletion (Lessons,
     * Questions, Tests, Attempts, Timetable/preparation rows, ...) happens there; this method
     * only does the ownership check first, same "auth here, cascade there" split as {@link
     * LessonService#delete}/{@link QuestionService#delete}/{@link TestService#delete}.
     */
    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Subject subject = getOwnedOrThrow(id, parentId);
        cascadeDeleteService.deleteSubjectCascade(subject.getId());
        logInfo("Subject deleted (cascade): id={}, parentId={}", subject.getId(), parentId);
    }

    /**
     * Loads the Subject with id {@code id}, throwing if it doesn't exist or it doesn't belong to
     * {@code parentId}. Revision 2026-09-06 (c): checks the Subject's own direct {@code
     * parentId} column now, instead of resolving ownership indirectly through the owning
     * Classroom - a SHARED Subject ({@code classroomId == null}) has no single Classroom left to
     * resolve through, so the old indirection can no longer work for every Subject. Also used by
     * {@link LessonService} to resolve a Lesson's indirect owner.
     */
    Subject getOwnedOrThrow(Long id, Long parentId) {
        Subject subject = getById(id);
        if (!subject.getParentId().equals(parentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This subject does not belong to the current parent");
        }
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

    /**
     * Creates one Subject row with NO ownership re-check (2026-09-05, item 2 of the 11-item batch
     * request, "Phu huynh bulk-tao Mon hoc qua import file") - {@code classroomId} ownership must
     * already be validated ONCE, up front, by the caller ({@code SubjectImportService#importFile},
     * before the file is even read) - same "check once up front, never re-check per row" shape as
     * {@code QuestionService#createFromImportRow} for {@code lessonId}. Package-private, only ever
     * called from {@code SubjectImportService}. Revision 2026-09-06 (c): now also sets the new
     * direct {@code parentId} column (import always assigns a real {@code classroomId}, never a
     * shared Subject, but every row still needs {@code parentId} filled for ownership checks).
     */
    Subject createFromImportRow(Long classroomId, Long parentId, String name) {
        LocalDateTime now = LocalDateTime.now();
        Subject subject = new Subject();
        subject.setParentId(parentId);
        subject.setClassroomId(classroomId);
        subject.setName(name);
        subject.setCreatedAt(now);
        subject.setUpdatedAt(now);
        subject.setCreatedBy("parent:" + parentId);
        subject.setUpdatedBy("parent:" + parentId);
        subject = subjectRepository.save(subject);
        logInfo("Subject created (import): id={}, classroomId={}, parentId={}", subject.getId(), classroomId, parentId);
        return subject;
    }

    /**
     * Whether {@code classroomId} already has a Subject with this exact (trimmed) name - used
     * only by {@code SubjectImportService} to reject a duplicate row during import (same "bao loi
     * dong do, bo qua" convention as {@code LibraryService#existsExact}, applied here for
     * consistency even though the user was not asked again for this specific feature). Unchanged
     * by the 2026-09-06 (c) revision - import always passes a real {@code classroomId}, never
     * checks against shared Subjects.
     */
    boolean existsByName(Long classroomId, String name) {
        return subjectRepository.query()
                .eq(Subject::getClassroomId, classroomId)
                .eq(Subject::getName, name)
                .exists();
    }
}
