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

import java.time.LocalDateTime;

/**
 * One Student's attempt at one {@link Test}. v1 allows at most 1 Attempt per Test - {@code
 * StudentAttemptService#start} is idempotent (returns the existing Attempt instead of creating a
 * second one) rather than this being enforced by a DB constraint, see {@code
 * docs/dev/06-hoc-sinh-lam-bai.md}.
 * <p>
 * {@code submittedAt}/{@code correctCount} start null/0 and are only ever set once, by {@code
 * StudentAttemptService#submit} - {@code correctCount} deliberately has no default grading before
 * that point (see {@code docs/dev/07-ket-qua-bao-cao.md} acceptance criteria: a not-yet-submitted
 * Attempt must not be reported as "0 correct", so callers must check {@code submittedAt != null}
 * before trusting {@code correctCount}).
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "attempt")
public class Attempt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long testId;
    private Long studentId;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Integer correctCount;
    private Integer totalQuestions;
}
