package vn.org.thn.service.app.quiz.service;

import vn.org.thn.service.app.quiz.dto.BulkDeleteError;
import vn.org.thn.service.app.quiz.dto.BulkDeleteResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Shared best-effort bulk-delete loop (2026-09-06, per the user's explicit request: "them chuc
 * nang xoa nhieu cau hoi cua mot bai hoac xoa all cau hoi ... tuong tu nhu vay cung cho phep xoa
 * bai cua muon hoc, xoa muon hoc, xoa lop" - bulk/"delete all" for Questions/Lessons/Subjects/
 * Classrooms), reused by {@code QuestionService}/{@code LessonService}/{@code SubjectService}/
 * {@code ClassroomService}'s new {@code deleteMany(ids)} methods.
 * <p>
 * Same "one failure doesn't block the rest" convention as every other bulk operation in this
 * codebase (e.g. {@code QuestionImportService}/{@code SubjectImportService} - see their javadoc).
 * Deliberately a package-private static helper, NOT a Spring {@code @Service} bean: it needs no
 * state and no dependencies of its own, it only wraps whichever entity service's own
 * already-existing single-id {@code delete(id)} method is passed in as {@code deleteOne} -
 * reusing that method's own ownership-check + cascade logic entirely (see {@code
 * CascadeDeleteService}) rather than duplicating any of it here.
 */
final class BulkDeleteSupport {

    private BulkDeleteSupport() {
    }

    /**
     * Calls {@code deleteOne} once per id in {@code ids}, catching any exception it throws (a
     * wrong-owner {@code BusinessException}/{@code FORBIDDEN}, an already-deleted id's {@code
     * NOT_FOUND}, or anything unexpected) so one bad id never stops the rest from being deleted.
     */
    static BulkDeleteResponse deleteEach(List<Long> ids, Consumer<Long> deleteOne) {
        List<BulkDeleteError> errors = new ArrayList<>();
        int deletedCount = 0;
        for (Long id : ids) {
            try {
                deleteOne.accept(id);
                deletedCount++;
            } catch (Exception e) {
                errors.add(new BulkDeleteError(id, e.getMessage()));
            }
        }
        BulkDeleteResponse response = new BulkDeleteResponse();
        response.setRequested(ids.size());
        response.setDeletedCount(deletedCount);
        response.setErrors(errors);
        return response;
    }
}
