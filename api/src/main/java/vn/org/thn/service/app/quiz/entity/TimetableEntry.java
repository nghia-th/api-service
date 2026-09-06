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
 * One {@link Subject} scheduled on one day-of-week of a {@link Student}'s weekly timetable
 * ("thoi khoa bieu", 2026-09-05, per the user's explicit request "tao chuc nang thoi khoa bieu
 * trong 1 tuan cua con"). Deliberately a SINGLE persistent template per Student rather than a
 * dated/per-week snapshot (AskUserQuestion 2026-09-05: "1 mau chung duy nhat") - editing a day's
 * subjects takes effect immediately and forever going forward; there is no separate "this week
 * only" override and no copy-forward step needed, since there is only ever one row set per
 * student+dayOfWeek. Any "today"/"tomorrow" screen (Student/Parent, added in a later part of
 * this same feature) resolves a real calendar date to a {@code dayOfWeek} via {@code
 * LocalDate.getDayOfWeek().getValue()} and looks this table up by studentId+dayOfWeek - the
 * template itself never stores a real date.
 * <p>
 * No time-of-day/period concept (AskUserQuestion 2026-09-05: "chi danh sach mon theo thu tu,
 * khong can gio") - {@code orderIndex} alone controls display order within the day, same
 * "orderIndex from request list position" pattern as {@link TestQuestion}.
 * <p>
 * <b>Revision 2026-09-06 (a):</b> originally pinned an exact {@link Lesson} (AskUserQuestion
 * 2026-09-05: "gan dung 1 Lesson co san"). After the Parent tested part 1 of this feature, the
 * request was clarified to be Subject-level only ("thoi khoa bieu la: toan, anh van, hoa") - a
 * day just lists which Subjects are studied, in what order, with no specific Lesson pinned. The
 * column was changed from {@code lessonId} to {@code subjectId} accordingly (see the V5 database
 * migration) - the class/table name ({@code TimetableEntry}/{@code timetable_entry}) was kept
 * unchanged to minimize the size of this change, only the column's meaning moved one level up
 * the hierarchy (Lesson -> Subject). The "prepared for tomorrow" checklist ({@link
 * LessonPreparation}) was updated the same way in the same revision, for the same reason.
 * <p>
 * <b>Revision 2026-09-06 (b):</b> changed from per-{@link Classroom} to per-{@link Student}, per
 * the user's explicit request: "hien tai tao thoi khoa bieu theo lop dung ra la thoi khoa bieu
 * theo hoc sinh boi vi phu huynh co 2 con cung hoc mot lop nhung thoi khoa bieu khac nhau" - two
 * children of the same Parent can share a Classroom yet legitimately need different individual
 * schedules, which a single shared-per-Classroom row could never represent. The column was
 * changed from {@code classroomId} to {@code studentId} (see the V7 database migration, DROP +
 * CREATE, all existing rows wiped per the user's explicit answer "Xoa sach, lam lai tu dau" -
 * same precedent as revision (a)'s V5 migration). Validating that a chosen {@code subjectId}
 * actually belongs to the Student's OWN Classroom still happens (see {@code
 * TimetableService#setDay}), just resolved through the Student rather than stored redundantly
 * here.
 * <p>
 * There is deliberately no "volume/tap" field anywhere - a textbook with multiple volumes is
 * represented as separate {@link Subject} rows with distinguishing names (e.g. "Toan tap
 * 1"/"Toan tap 2"), per the user's explicit answer (AskUserQuestion 2026-09-05: "dung ten mon
 * phan biet") - no schema change was needed for that.
 * <p>
 * {@code dayOfWeek} is 1-7, Monday-Sunday, matching {@code java.time.DayOfWeek#getValue()}
 * exactly (ISO-8601) - never a 0-based or Sunday-first convention, so callers can always use
 * {@code LocalDate.now().getDayOfWeek().getValue()} directly with no translation.
 * <p>
 * Ownership resolves via its own {@code studentId} column (NOT derived by walking
 * Subject->Classroom->Student on every read, since a Student's whole-week timetable is read far
 * more often than any single entry is edited) - see {@code TimetableService#getOwnedOrThrow},
 * and compare {@link Subject#getClassroomId()}'s own direct-column choice for the same reason.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "timetable_entry")
public class TimetableEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long studentId;
    private Integer dayOfWeek;
    private Long subjectId;
    private Integer orderIndex;
}
