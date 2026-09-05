package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;
import vn.org.thn.service.base.entity.BaseEntity;

/**
 * A textbook PDF uploaded by an Admin (2026-09-05, "thu vien sach giao khoa" feature) - organized
 * by {@code grade} (1-12, fixed list, validated in {@code LibraryService#upload}), {@code
 * subjectName} (free text, e.g. "Toan" - independent from any Parent's own {@link Subject} rows,
 * since this is shared curriculum material managed centrally by Admin, not owned by a Parent) and
 * {@code curriculum} (one of a fixed 3-value list - "Ket noi tri thuc"/"Chan troi sang tao"/
 * "Canh dieu", also validated in {@code LibraryService#upload}), plus an optional {@code volume}
 * (e.g. "Tap 1"). Example from the user's own request: "Lop 4 -&gt; Toan tap 1 -&gt; Ket noi tri
 * thuc".
 * <p>
 * Not owned by any Parent - every Admin can manage the whole library (no root-only restriction,
 * unlike {@code AdminManageApi}'s Admin-manages-Admin feature). A Parent links their OWN {@link
 * Subject} rows to documents here via {@link SubjectLibraryLink} (many-to-many - one Subject can
 * link multiple documents, and in principle one document could be linked from multiple Subjects
 * too, e.g. two different Parents' Subjects for the same grade/curriculum) - see {@code
 * LibraryService}'s javadoc for the full access model.
 * <p>
 * FILE STORAGE: same convention as {@code Lesson#imagePath}/{@code Question#audioPath} - only the
 * server-generated filename lives in {@link #filePath}, the actual PDF bytes live under {@code
 * LibraryService#LIBRARY_DIR} on disk, never in the database.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "library_document")
public class LibraryDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int grade;
    private String subjectName;
    private String curriculum;
    private String volume;
    private String title;
    private String filePath;
    private long fileSize;
}
