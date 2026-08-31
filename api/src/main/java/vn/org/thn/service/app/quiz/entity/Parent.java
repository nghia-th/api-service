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
 * A parent account - the top-level tenant boundary in quiz-service: every Subject/Lesson/
 * Question/Test a parent owns must be filtered by {@code parentId} in every {@code *Service}
 * method (see {@code docs/01-thiet-ke-tong-the.md} muc 3).
 * <p>
 * {@code password} is a BCrypt hash, never the plaintext. {@code @ToString.Exclude} keeps it out
 * of log lines even by accident (e.g. an {@code IBase.logInfo(entity)} call), on top of the
 * existing rule that no API response ever returns this field directly - see {@link
 * vn.org.thn.service.app.quiz.dto.ParentResponse}.
 * <p>
 * {@code @EqualsAndHashCode(callSuper = true)}/{@code @ToString(callSuper = true)} are required
 * because {@link BaseEntity} also carries its own {@code @Data} - without {@code callSuper}, the
 * generated {@code equals}/{@code hashCode}/{@code toString} would silently ignore the 5 inherited
 * audit fields (same reasoning as {@code Article} in {@code api-service}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "parent")
public class Parent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;

    @ToString.Exclude
    private String password;

    private String phone;
}
