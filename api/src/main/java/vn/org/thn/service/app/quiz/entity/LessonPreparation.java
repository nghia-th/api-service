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
 * One {@link Subject} the current Student has marked as "prepared" ("da chuan bi bai") for a
 * specific calendar date - item 9 of the 2026-09-05 batch request ("hoc sinh chuan bi bai cho
 * ngay mai theo thoi khoa bieu bang cach danh dau da chuan bi bai va gui cho phu huynh"). A row's
 * mere EXISTENCE means "prepared" - there is no boolean flag, unmarking simply deletes the row
 * (same "presence = true, absence = false" shape used for {@code Choice#isCorrect} elsewhere in
 * this codebase, just at the row level instead of a column).
 * <p>
 * <b>Revision 2026-09-06:</b> originally tracked an exact {@link Lesson} (see {@link
 * TimetableEntry}'s javadoc for the matching revision on the timetable side) - once {@code
 * TimetableEntry} stopped pinning a specific Lesson (Parent's request became Subject-level only,
 * "thoi khoa bieu la: toan, anh van, hoa"), this checklist could no longer key off a lessonId
 * either, so it now keys off {@code subjectId} (AskUserQuestion 2026-09-06: "doi sang tick theo
 * Mon hoc" - Recommended option). The class/table name ({@code LessonPreparation}/{@code
 * lesson_preparation}) was kept unchanged to minimize the size of this change - only the tracked
 * unit moved from Lesson to Subject.
 * <p>
 * {@code targetDate} (NOT {@code dayOfWeek}) is a real calendar date, deliberately different from
 * {@link TimetableEntry#getDayOfWeek()} - the timetable is a recurring WEEKLY template with no
 * concept of a specific day, but "prepared for tomorrow" is inherently about ONE specific
 * upcoming date (today's Monday prep and next Monday's prep must never collide, and once marked,
 * the prepared status for a given date should never retroactively change just because the
 * timetable template was edited afterward - see {@link
 * vn.org.thn.service.app.quiz.service.LessonPreparationService}'s javadoc). No {@code
 * classroomId} column - resolved indirectly via {@code subjectId} exactly like {@link
 * TimetableEntry} does, and {@code studentId} is enough to scope every query here (a Student
 * always has exactly 1 Classroom).
 * <p>
 * (studentId, targetDate, subjectId) is the natural key - {@code LessonPreparationService} checks
 * for an existing row before inserting (idempotent mark), so marking the same subject twice for
 * the same date never creates a duplicate row.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "lesson_preparation")
public class LessonPreparation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private LocalDate targetDate;
    private Long subjectId;
}
