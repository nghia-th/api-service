package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

/**
 * One answer choice of a {@link Question} ({@code questionId}). No audit fields - its lifecycle
 * is fully owned by its parent Question (created/replaced/deleted together with it), so it does
 * not extend {@code BaseEntity} - see {@code docs/01-thiet-ke-tong-the.md} section 2 (data model).
 */
@Data
@Entity
@Table(name = "choice")
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long questionId;
    private String content;
    private Boolean correct;
}
