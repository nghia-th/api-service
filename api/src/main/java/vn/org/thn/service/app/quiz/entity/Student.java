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
 * A student account, always owned by exactly one {@link Parent} ({@code parentId}). Created only
 * through {@code POST /api/parent/students} (task 2) - there is no student self-registration, so
 * this class has no counterpart to {@code Parent}'s register flow.
 * <p>
 * {@code username} is unique system-wide (not just per parent), since it doubles as the student's
 * login identifier alongside {@code password}. See {@link Parent} for the {@code callSuper}/
 * {@code @ToString.Exclude} reasoning - identical here.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "student")
public class Student extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    private String fullName;
    private String grade;
    private String username;

    @ToString.Exclude
    private String password;
}
