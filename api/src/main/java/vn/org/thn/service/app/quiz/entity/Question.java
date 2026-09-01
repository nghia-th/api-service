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
 * A question, always owned by exactly one {@link Lesson} ({@code lessonId}). Created either by
 * hand ({@code POST /api/parent/questions}) or via the Excel/CSV import (task 4) - both paths
 * produce the same {@code Question} + {@link Choice} rows, so there is only ever this one entity
 * for either origin.
 * <p>
 * {@code knowledgeTag} is optional free text (no fixed enum in v1) - see {@code
 * docs/dev/07-ket-qua-bao-cao.md} for how it is used later to group results.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "question")
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lessonId;
    private String content;
    private String knowledgeTag;
}
