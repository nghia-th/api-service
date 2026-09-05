package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

import java.time.LocalDateTime;

/**
 * Many-to-many join between a Parent's own {@link Subject} and a Admin-managed {@link
 * LibraryDocument} (2026-09-05, "thu vien sach giao khoa" feature, per the user's explicit
 * decision that one Subject may link MULTIPLE documents at once). A Parent creates/removes these
 * rows themselves ({@code ParentLibraryService#link}/{@code #unlink}) after browsing the whole
 * library - unlike {@link TestQuestion} (owned entirely by its Test's lifecycle, no audit fields),
 * this row is a standalone Parent action worth timestamping, so it keeps a lightweight {@code
 * linkedAt}/{@code linkedBy} pair instead of the full {@code BaseEntity} audit suite (no update/
 * soft-delete concept applies to a link - it only ever exists or doesn't).
 * <p>
 * {@code uq_subject_library_link} (subject_id, library_document_id) prevents linking the same pair
 * twice - {@code ParentLibraryService#link} also checks this explicitly first, to return a clean
 * {@code QuizErrorCode#LIBRARY_ALREADY_LINKED} instead of a raw constraint-violation error.
 */
@Data
@Entity
@Table(name = "subject_library_link")
public class SubjectLibraryLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long subjectId;
    private Long libraryDocumentId;
    private LocalDateTime linkedAt;
    private String linkedBy;
}
