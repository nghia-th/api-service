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

import java.time.LocalDate;

/**
 * One {@link Lesson} the current Student has confirmed studying on a real calendar date ("bao
 * bai", 2026-09-06 - "hom nay con hoc gi": "hom nay con hoc toan -> con hoc bai 1"). Different
 * from {@link TimetableEntry} (a recurring weekly Subject-only schedule with no Lesson pinned,
 * see its 2026-09-06 revision) and from {@link LessonPreparation} (a forward-looking "will
 * study tomorrow" checklist scoped to Subject) - this is a backward-looking, permanent LOG: each
 * row is one real, dated confirmation that a specific Lesson was actually covered.
 * <p>
 * (AskUserQuestion 2026-09-06 design decisions:)
 * <ul>
 *     <li>Only a Lesson whose Subject is on TODAY's timetable may be reported - checked in
 *     {@code LessonReportService#reportLesson}, not in the DB.</li>
 *     <li>Multiple Lessons per Subject per day are allowed - no per-day uniqueness here.</li>
 *     <li>Only TODAY's own reports can be undone ({@code LessonReportService#unreportLesson}
 *     rejects any row whose {@code reportDate} is not today) - a row surviving past its day
 *     becomes permanent history.</li>
 *     <li>A Lesson reported on ANY date is hidden from the picker FOREVER after - enforced by
 *     the unique (studentId, lessonId) constraint below: a Student can never report the same
 *     Lesson twice, on any date.</li>
 * </ul>
 * <p>
 * No {@code subjectId} column - resolved indirectly via {@code lessonId} -> {@link
 * Lesson#getSubjectId()}, same "don't duplicate a derivable foreign key" reasoning used
 * throughout this codebase (e.g. {@link Subject} has no {@code parentId}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "lesson_report")
public class LessonReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Long lessonId;
    private LocalDate reportDate;
}
