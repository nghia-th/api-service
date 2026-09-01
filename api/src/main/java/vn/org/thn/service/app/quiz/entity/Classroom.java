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
 * A classroom (e.g. "Lop 5A"), always owned by exactly one {@link Parent} ({@code parentId}) -
 * classrooms are not shared across families, same tenancy rule as {@link Subject}/{@link Student}.
 * <p>
 * Top of the hierarchy added after task 7: Classroom -> Subject -> Lesson -> Question (Lesson/
 * Question unchanged), and Classroom -> Student (1 classroom per student, replacing the old
 * free-text {@code Student.grade} field - see {@code StudentService}). A {@link
 * vn.org.thn.service.app.quiz.entity.Test} still targets one Student directly, same as before -
 * Classroom is purely an organizing/filtering concept for the Subject/Student pickers, not a new
 * assignment target (confirmed with the user rather than assumed).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "classroom")
public class Classroom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    private String name;
}
