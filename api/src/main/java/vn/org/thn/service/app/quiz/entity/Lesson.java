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
 * A lesson (e.g. "Unit 1 - Present Simple"), always owned by exactly one {@link Subject}
 * ({@code subjectId}). {@code Lesson} has no {@code parentId} of its own - ownership by a Parent
 * is always resolved indirectly through its {@link Subject} (see {@code
 * vn.org.thn.service.app.quiz.service.LessonService#getOwnedOrThrow}).
 * <p>
 * {@code summary}/{@code content}/{@code textbookPage}/{@code imagePath} were added so a Student
 * can review the lesson material itself when taking or reviewing a {@code Test} built from its
 * {@link Question}s (task "Backend: them field noi dung cho Lesson", 2026-09-01). All 4 are
 * optional - a Parent may fill in none, some, or all of them. {@code imagePath} stores only the
 * server-side relative path/filename of the uploaded image (never the raw bytes) - see {@code
 * LessonService#uploadImage}/{@code #loadImage} for how the file itself is stored on disk.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "lesson")
public class Lesson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long subjectId;
    private String name;

    /** Tom tat ngan gon noi dung bai hoc - hien thi truoc cho Student khi on lai bai. */
    private String summary;

    /** Noi dung chi tiet cua bai hoc (co the dai) - Student doc lai day du khi can. */
    private String content;

    /** So trang trong sach giao khoa - de Student tra lai sach giay neu muon. */
    private Integer textbookPage;

    /** Duong dan tuong doi/ten file anh minh hoa da luu tren server, null neu chua upload anh. */
    private String imagePath;
}
