package vn.org.thn.service.app.quiz.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.org.thn.service.app.quiz.dto.CurriculumRequest;
import vn.org.thn.service.app.quiz.dto.CurriculumResponse;
import vn.org.thn.service.app.quiz.entity.Curriculum;
import vn.org.thn.service.app.quiz.entity.LibraryDocument;
import vn.org.thn.service.app.quiz.exception.QuizErrorCode;
import vn.org.thn.service.app.quiz.repository.CurriculumRepository;
import vn.org.thn.service.app.quiz.repository.LibraryDocumentRepository;
import vn.org.thn.service.app.quiz.security.CurrentUser;
import vn.org.thn.service.base.IBase;
import vn.org.thn.service.base.exception.BusinessException;
import vn.org.thn.service.base.exception.CommonErrorCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin CRUD for the "bo sach" (curriculum/textbook series) lookup list used by {@link
 * LibraryDocument#getCurriculum()} (2026-09-05, replaces the previous hardcoded 3-value fixed
 * list - see {@link Curriculum}'s javadoc for the full background and the user's own words).
 * <p>
 * Not owned by any Parent/Admin individually - every Admin can manage the whole list, no root
 * restriction, same access shape as {@code LibraryService} itself. {@link #list} has no ownership
 * filtering at all and is reused by {@code ParentCurriculumApi} (read-only, so a Parent can
 * populate the curriculum filter dropdown when browsing the library - see {@code
 * SubjectLibraryDialog.tsx}).
 * <p>
 * {@link #create}/{@link #update} enforce a unique {@code name} ({@link
 * QuizErrorCode#CURRICULUM_NAME_TAKEN}), same shape as {@code AuthService}'s username-uniqueness
 * check ({@code .ne(Curriculum::getId, id)} excludes the row being updated from its own
 * duplicate-check). {@link #delete} blocks while any {@link LibraryDocument} still has this exact
 * {@code name} ({@link QuizErrorCode#CURRICULUM_IN_USE}) - per the user's explicit choice
 * (AskUserQuestion 2026-09-05: "Chặn xoá, báo lỗi") - same "protect against orphaning data"
 * reasoning as {@code ClassroomService#delete}.
 */
@Service
public class CurriculumService extends IBase {

    @Autowired
    private CurriculumRepository curriculumRepository;

    @Autowired
    private LibraryDocumentRepository libraryDocumentRepository;

    /** Every curriculum, no filtering/paging in v1 - used by both Admin's management page and Parent's read-only browse-library filter dropdown. */
    public List<CurriculumResponse> list() {
        return curriculumRepository.query().list()
                .stream().map(CurriculumResponse::from).toList();
    }

    public CurriculumResponse create(CurriculumRequest request) {
        String name = request.getName().trim();
        if (curriculumRepository.query().eq(Curriculum::getName, name).exists()) {
            throw new BusinessException(QuizErrorCode.CURRICULUM_NAME_TAKEN);
        }

        Long adminId = CurrentUser.get().userId();
        LocalDateTime now = LocalDateTime.now();
        String actor = "admin:" + adminId;

        Curriculum curriculum = new Curriculum();
        curriculum.setName(name);
        curriculum.setCreatedAt(now);
        curriculum.setUpdatedAt(now);
        curriculum.setCreatedBy(actor);
        curriculum.setUpdatedBy(actor);
        curriculum = curriculumRepository.save(curriculum);

        logInfo("Curriculum created: id={}, name={}, adminId={}", curriculum.getId(), name, adminId);
        return CurriculumResponse.from(curriculum);
    }

    public CurriculumResponse update(Long id, CurriculumRequest request) {
        Curriculum curriculum = getByIdOrThrow(id);
        String name = request.getName().trim();
        if (curriculumRepository.query().eq(Curriculum::getName, name).ne(Curriculum::getId, id).exists()) {
            throw new BusinessException(QuizErrorCode.CURRICULUM_NAME_TAKEN);
        }

        Long adminId = CurrentUser.get().userId();
        curriculum.setName(name);
        curriculum.setUpdatedAt(LocalDateTime.now());
        curriculum.setUpdatedBy("admin:" + adminId);
        curriculum = curriculumRepository.save(curriculum);

        logInfo("Curriculum updated: id={}, name={}, adminId={}", curriculum.getId(), name, adminId);
        return CurriculumResponse.from(curriculum);
    }

    public void delete(Long id) {
        Curriculum curriculum = getByIdOrThrow(id);

        if (libraryDocumentRepository.query().eq(LibraryDocument::getCurriculum, curriculum.getName()).exists()) {
            throw new BusinessException(QuizErrorCode.CURRICULUM_IN_USE);
        }
        curriculumRepository.deleteById(id);

        logInfo("Curriculum deleted: id={}, name={}, adminId={}", id, curriculum.getName(), CurrentUser.get().userId());
    }

    /** Whether {@code name} is a currently-known curriculum - used by {@code LibraryService#upload} in place of the old hardcoded {@code Set.of(...)} check. */
    public boolean exists(String name) {
        return name != null && curriculumRepository.query().eq(Curriculum::getName, name).exists();
    }

    private Curriculum getByIdOrThrow(Long id) {
        Curriculum curriculum = curriculumRepository.findById(id);
        if (curriculum == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "Curriculum not found");
        }
        return curriculum;
    }
}
