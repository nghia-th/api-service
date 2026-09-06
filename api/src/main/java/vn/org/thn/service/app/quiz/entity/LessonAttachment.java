package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

import java.time.LocalDateTime;

/**
 * One lecture file (PowerPoint or PDF) attached to a {@link Lesson} (2026-09-06, "bai cua mon hoc
 * cho phep upload 1 hoac nhieu file bai giang" feature) - a Lesson may have any number of these,
 * added/removed freely by the owning Parent, and viewable/downloadable by BOTH the Parent and any
 * Student who can already see the Lesson's content (same access rule as {@code
 * Lesson#imagePath} - see {@code StudentLessonService}'s javadoc).
 * <p>
 * Unlike {@code Lesson#imagePath} (a single slot on the Lesson row itself, replaced on every
 * upload), this is a genuine one-to-many child table, and unlike {@link BaseEntity}-based rows an
 * attachment has no edit/soft-delete concept of its own - it only ever exists or doesn't (same
 * reasoning as {@link SubjectLibraryLink}'s javadoc), so it keeps a lightweight {@code
 * createdAt}/{@code createdBy} pair instead of the full audit suite.
 * <p>
 * {@link #originalName} is kept (unlike every single-file upload elsewhere in this codebase, e.g.
 * {@code Lesson#imagePath}/{@code LibraryDocument#filePath}) because a Lesson can have SEVERAL
 * attachments at once - the Parent/Student need a human-readable name to tell them apart in a
 * list. {@link #filePath} is still always server-generated (never the client's original filename/
 * extension) for the same path-traversal reasons as every other upload in this codebase.
 */
@Data
@Entity
@Table(name = "lesson_attachment")
public class LessonAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lessonId;
    private String originalName;
    private String filePath;
    private long fileSize;
    private String contentType;
    private LocalDateTime createdAt;
    private String createdBy;
}
