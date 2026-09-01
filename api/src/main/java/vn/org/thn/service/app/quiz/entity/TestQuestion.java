package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

/**
 * Links a {@link Test} to one of its {@link Question}s and records display order via {@code
 * orderIndex} (0-based, taken from the position of {@code questionIds} in the create request -
 * see {@code docs/dev/05-tao-giao-bai-kiem-tra.md}). No audit fields - fully owned by its Test's
 * lifecycle, same reasoning as {@link Choice}.
 */
@Data
@Entity
@Table(name = "test_question")
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long testId;
    private Long questionId;
    private Integer orderIndex;
}
