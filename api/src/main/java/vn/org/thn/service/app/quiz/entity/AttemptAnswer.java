package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

/**
 * One Question's answer within an {@link Attempt} - {@code choiceId} is null if the Student left
 * that question blank. {@code correct} stays null until {@code
 * StudentAttemptService#submit} grades it (compares {@code choiceId} against the Question's
 * correct {@link Choice}) - not computed at answer-save time, per {@code
 * docs/dev/06-hoc-sinh-lam-bai.md}. No audit fields, same reasoning as {@link Choice}/{@link
 * TestQuestion}.
 */
@Data
@Entity
@Table(name = "attempt_answer")
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long attemptId;
    private Long questionId;
    private Long choiceId;
    private Boolean correct;
}
