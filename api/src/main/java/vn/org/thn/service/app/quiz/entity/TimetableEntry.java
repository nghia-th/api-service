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
 * One {@link Lesson} scheduled on one day-of-week of a {@link Classroom}'s weekly timetable
 * ("thoi khoa bieu", 2026-09-05, per the user's explicit request "tao chuc nang thoi khoa bieu
 * trong 1 tuan cua con"). Deliberately a SINGLE persistent template per Classroom rather than a
 * dated/per-week snapshot (AskUserQuestion 2026-09-05: "1 mau chung duy nhat") - editing a day's
 * lessons takes effect immediately and forever going forward; there is no separate "this week
 * only" override and no copy-forward step needed, since there is only ever one row set per
 * classroom+dayOfWeek. Any "today"/"tomorrow" screen (Student/Parent, added in a later part of
 * this same feature) resolves a real calendar date to a {@code dayOfWeek} via {@code
 * LocalDate.getDayOfWeek().getValue()} and looks this table up by classroomId+dayOfWeek - the
 * template itself never stores a real date.
 * <p>
 * No time-of-day/period concept (AskUserQuestion 2026-09-05: "chi danh sach mon theo thu tu,
 * khong can gio") - {@code orderIndex} alone controls display order within the day, same
 * "orderIndex from request list position" pattern as {@link TestQuestion}.
 * <p>
 * Deliberately pins an exact {@link Lesson} (AskUserQuestion 2026-09-05: "gan dung 1 Lesson co
 * san"), not just a free-text Subject name - {@code lessonId} is enough to resolve BOTH the
 * Subject (via {@code Lesson#getSubjectId}) and the actual lesson content a Student can review,
 * so this entity does NOT duplicate a separate {@code subjectId} column (same "resolve
 * indirectly, don't duplicate a derivable foreign key" reasoning as {@link Subject} having no
 * {@code parentId} of its own). There is deliberately no "volume/tap" field anywhere - a textbook
 * with multiple volumes is represented as separate {@link Subject} rows with distinguishing names
 * (e.g. "Toan tap 1"/"Toan tap 2"), per the user's explicit answer (AskUserQuestion 2026-09-05:
 * "dung ten mon phan biet") - no schema change was needed for that.
 * <p>
 * {@code dayOfWeek} is 1-7, Monday-Sunday, matching {@code java.time.DayOfWeek#getValue()}
 * exactly (ISO-8601) - never a 0-based or Sunday-first convention, so callers can always use
 * {@code LocalDate.now().getDayOfWeek().getValue()} directly with no translation.
 * <p>
 * Ownership resolves via its own {@code classroomId} column (NOT derived by walking
 * Lesson->Subject->Classroom on every read, since a Classroom's whole-week timetable is read far
 * more often than any single entry is edited) - see {@code TimetableService#getOwnedOrThrow}, and
 * compare {@link Subject#getClassroomId()}'s own direct-column choice for the same reason.
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

    private Long classroomId;
    private Integer dayOfWeek;
    private Long lessonId;
    private Integer orderIndex;
}
