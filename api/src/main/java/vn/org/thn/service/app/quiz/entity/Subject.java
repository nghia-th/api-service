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
 * A subject (e.g. "Math", "English"), normally owned through exactly one {@link Classroom}
 * ({@code classroomId}) - subjects are not shared across families.
 * <p>
 * <b>Revision 2026-09-06:</b> {@code classroomId} became NULLABLE and a direct {@code parentId}
 * column was added, per the user's explicit request: "anh co 1 mon hoc ma khong thuoc lop nao vi
 * du anh co mon lap trinh python thi cac hoc sinh cua phu huynh deu hoc muon nay khong phan biet
 * lop" - a Subject can now be SHARED across every Classroom of a Parent by leaving {@code
 * classroomId} null, instead of always pinning exactly one Classroom. {@code parentId} is a real
 * direct tenant column now (unlike before this revision, when ownership was ALWAYS resolved by
 * walking up to the owning Classroom, see {@code SubjectService#getOwnedOrThrow}) - a shared
 * Subject has no single owning Classroom to walk up through, so it needs its own column; a
 * classroom-scoped Subject also gets it filled in (kept in sync with its Classroom's own {@code
 * parentId} at create/update time) so every ownership check can use the SAME column regardless of
 * whether the Subject is shared or classroom-scoped.
 * <p>
 * Every place that used to check "does this Subject belong to the Student's Classroom" (Test
 * creation, Timetable, Lesson report, Textbook library, "On tap kien thuc") now also accepts
 * {@code classroomId == null} (shared - applies to every Classroom of this Subject's Parent,
 * hence every Student of that Parent regardless of which Classroom they are in) - see {@code
 * TimetableService#getSubjectInClassroomOrThrow} for the canonical version of that check.
 * <p>
 * Migrated via an ALTER (V8 database migration), NOT a DROP+CREATE like some earlier revisions
 * (V5, V7) - unlike those, this table already holds real Parent-authored data (Subjects with real
 * Lessons/Questions/Tests under them), so no data was discarded; {@code parentId} was backfilled
 * from each row's existing {@code classroom.parentId}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "subject")
public class Subject extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    /** Null means this Subject is SHARED - it applies to every Classroom of {@link #getParentId()}'s Parent, not just one. See this entity's 2026-09-06 revision javadoc. */
    private Long classroomId;
    private String name;
}
