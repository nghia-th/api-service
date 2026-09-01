package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.LessonCreateRequest;
import vn.org.thn.service.app.quiz.dto.LessonResponse;
import vn.org.thn.service.app.quiz.dto.LessonUpdateRequest;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.app.quiz.repository.LessonRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lesson CRUD for the currently logged-in Parent (task 3). {@code Lesson} has no {@code
 * parentId} of its own (see the entity's javadoc), so every ownership check here goes through
 * {@link SubjectService#getOwnedOrThrow} on the lesson's {@code subjectId} - reused (package-
 * private) rather than duplicating the "not found vs. forbidden" logic a second time. This
 * class's own {@code getOwnedOrThrow} is package-private for the same reason - task 4's {@code
 * QuestionService} reuses it to resolve a Question's indirect owner through its Lesson.
 * <p>
 * {@code delete} does NOT block on {@code Question} children - task 4 added that check to {@code
 * QuestionService#delete} instead (blocking a Question's own deletion when it is used in a
 * {@code TestQuestion}), which is a different rule than "does this Lesson have any Question at
 * all". Revisiting whether Lesson delete should also block on having Questions was intentionally
 * left as-is (v1 hard delete, see task 3's decision #12) since the task 3 spec's concern - not
 * losing a question bank silently - is now covered by Question's own delete guard.
 */
@Service
public class LessonService extends IBase {

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private SubjectService subjectService;

    public LessonResponse create(LessonCreateRequest request) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(request.getSubjectId(), parentId);

        LocalDateTime now = LocalDateTime.now();
        Lesson lesson = new Lesson();
        lesson.setSubjectId(request.getSubjectId());
        lesson.setName(request.getName());
        lesson.setCreatedAt(now);
        lesson.setUpdatedAt(now);
        lesson.setCreatedBy("parent:" + parentId);
        lesson.setUpdatedBy("parent:" + parentId);
        lesson = lessonRepository.save(lesson);

        logInfo("Lesson created: id={}, subjectId={}, parentId={}", lesson.getId(), lesson.getSubjectId(), parentId);
        return LessonResponse.from(lesson);
    }

    public LessonResponse update(Long id, LessonUpdateRequest request) {
        Long parentId = CurrentUser.get().userId();
        Lesson lesson = getOwnedOrThrow(id, parentId);

        lesson.setName(request.getName());
        lesson.setUpdatedAt(LocalDateTime.now());
        lesson.setUpdatedBy("parent:" + parentId);
        lesson = lessonRepository.save(lesson);

        logInfo("Lesson updated: id={}, parentId={}", lesson.getId(), parentId);
        return LessonResponse.from(lesson);
    }

    public LessonResponse get(Long id) {
        return LessonResponse.from(getOwnedOrThrow(id, CurrentUser.get().userId()));
    }

    /** Every Lesson under {@code subjectId}. {@code subjectId} is a required query param (not "every lesson of the current parent") to keep the list short for a Subject -> Lesson picker UI - see task 3 spec. */
    public List<LessonResponse> list(Long subjectId) {
        Long parentId = CurrentUser.get().userId();
        subjectService.getOwnedOrThrow(subjectId, parentId);
        return lessonRepository.query().eq(Lesson::getSubjectId, subjectId).list()
                .stream().map(LessonResponse::from).toList();
    }

    public void delete(Long id) {
        Long parentId = CurrentUser.get().userId();
        Lesson lesson = getOwnedOrThrow(id, parentId);

        lessonRepository.deleteById(lesson.getId());
        logInfo("Lesson deleted: id={}, parentId={}", lesson.getId(), parentId);
    }

    /** Loads the Lesson with id {@code id}, throwing if it doesn't exist or its Subject doesn't belong to {@code parentId}. Package-private (not private) so {@code QuestionService} can reuse it - same reasoning as {@code SubjectService#getOwnedOrThrow}. */
    Lesson getOwnedOrThrow(Long id, Long parentId) {
        Lesson lesson = lessonRepository.findById(id);
        if (lesson == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Lesson not found");
        }
        // Resolves ownership through the parent Subject - throws NOT_FOUND/FORBIDDEN the same way
        // a direct Lesson.parentId check would, if that field existed.
        subjectService.getOwnedOrThrow(lesson.getSubjectId(), parentId);
        return lesson;
    }
}
