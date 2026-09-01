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
 * A test (bai kiem tra), created by a Parent and always assigned to exactly one Student at
 * creation time - there is no separate "assign" step in v1 (see {@code
 * docs/dev/05-tao-giao-bai-kiem-tra.md}). {@code status} is a plain string, one of {@link
 * TestStatus}'s names - kept as {@code String} on the entity (not the enum type) because the
 * MyBatis mapping in this codebase maps columns by simple field type, matching every other
 * entity's plain-String-column style rather than introducing enum-column mapping just here.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "test")
public class Test extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    private Long studentId;
    private String name;
    private String status;
}
