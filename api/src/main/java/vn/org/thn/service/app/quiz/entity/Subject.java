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
 * A subject (e.g. "Math", "English"), always owned through exactly one {@link Classroom} ({@code
 * classroomId}) - subjects are not shared across families in v1, and (after Classroom was added)
 * not shared across a family's classrooms either. Has no {@code parentId} of its own - same
 * "child entity, no direct tenant column" shape as {@link Lesson}; ownership is always resolved
 * by walking up to the owning Classroom (see {@code SubjectService#getOwnedOrThrow}), never
 * checked directly on this entity. See {@link Parent} for the {@code callSuper}/audit-field
 * reasoning.
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

    private Long classroomId;
    private String name;
}
