package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.SubjectRequest;
import vn.org.thn.service.app.quiz.dto.SubjectResponse;
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
 */
@Service
public class SubjectService extends IBase {

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private LessonRepository lessonRepository;

    public SubjectResponse create(SubjectRequest request) {
        Long parentId = CurrentUser.get().userId();

        LocalDateTime now = LocalDateTime.now();
        Subject subject = new Subject();
        subject.setParentId(parentId);
        subject.setName(request.getName());
        subject.setCreatedAt(now);
        subject.setUpdatedAt(now);
        subject.setCreatedBy("parent:" + parentId);
        subject.setUpdatedBy("parent:" + parentId);
        subject = subjectRepository.save(subject);

        logInfo("Subject created: id={}, parentId={}", subject.getId(), parentId);
        return SubjectResponse.from(subject);
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        Long parentId = CurrentUser.get().userId();
        Subject subject = getOwnedOrThrow(id, parentId);

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

    /** Every Subject belonging to the current Parent. No paging in v1 - same reasoning as StudentService#list. */
    public List<SubjectResponse> list() {
        Long parentId = CurrentUser.get().userId();
        return subjectRepository.query().eq(Subject::getParentId, parentId).list()
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

    /** Loads the Subject with id {@code id}, throwing if it doesn't exist or doesn't belong to {@code parentId}. Also used by {@link LessonService} to resolve a Lesson's indirect owner. */
    Subject getOwnedOrThrow(Long id, Long parentId) {
        Subject subject = subjectRepository.findById(id);
        if (subject == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Subject not found");
        }
        if (!subject.getParentId().equals(parentId)) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "This subject does not belong to the current parent");
        }
        return subject;
    }
}
