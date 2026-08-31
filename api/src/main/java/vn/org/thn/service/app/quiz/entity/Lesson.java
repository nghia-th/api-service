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
}
