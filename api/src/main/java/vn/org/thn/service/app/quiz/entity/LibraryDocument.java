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
 * A library entry uploaded by an Admin (2026-09-05, "thu vien sach giao khoa" feature) -
 * originally always a textbook organized by {@code grade} (1-12), {@code subjectName} and {@code
 * curriculum} (a name from the Admin-managed {@link Curriculum} lookup list). <b>Revision
 * 2026-09-06</b> (extending the feature per the user's explicit choice, instead of a separate
 * "mon hoc" feature): {@code grade}/{@code curriculum} are now BOTH optional - a general course
 * not tied to any grade/curriculum (the user's own example: "Lap trinh Python") is represented by
 * leaving both {@code null}. {@code subjectName} stays required either way - it is this entry's
 * actual name regardless of whether grade/curriculum apply.
 * <p>
 * Not owned by any Parent - every Admin can manage the whole library (no root-only restriction,
 * unlike {@code AdminManageApi}'s Admin-manages-Admin feature). A Parent links their OWN {@link
 * Subject} rows to documents here via {@link SubjectLibraryLink} (many-to-many) - see {@code
 * LibraryService}'s javadoc for the full access model.
 * <p>
 * FILE STORAGE (revision 2026-09-06): a document may now carry ANY NUMBER of files (was always
 * exactly one before) - {@code filePath}/{@code fileSize} moved out of this entity entirely into
 * the new {@link LibraryDocumentFile} child table (one-to-many).
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

    private Integer grade;
    private String subjectName;
    private String curriculum;
    private String volume;
    private String title;
}
