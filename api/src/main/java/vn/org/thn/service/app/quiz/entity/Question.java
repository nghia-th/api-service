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
 * <p>
 * {@code audioPath}/{@code hideContentInTest} were added for the "listening question" feature
 * (task "Cau hoi dang am thanh", 2026-09-01) so a Question can optionally carry an audio clip a
 * Student listens to instead of (or in addition to) reading {@code content} - e.g. an English
 * listening-comprehension item. This is a per-question OPTIONAL attachment on the existing
 * Question, not a separate "question type" (decided via AskUserQuestion before implementing, same
 * "keep it simple, reuse the existing shape" choice already made for {@code Lesson.imagePath}) -
 * every Question still always has a required {@code content} text (Parent-facing management/
 * report label), audio is purely additive. {@code audioPath} stores only the server-side relative
 * filename of the uploaded audio (never the raw bytes) - see {@code
 * QuestionService#uploadAudio}/{@code #loadAudio} for how the file itself is stored on disk, same
 * pattern as {@code Lesson#imagePath}. {@code hideContentInTest} is the Parent's own per-question
 * choice (AskUserQuestion answer: "phu huynh duoc phep cho hien content hay khong") of whether the
 * Student-facing take-test screen shows {@code content} as text or hides it so the Student must
 * rely on the audio alone (a real "listening test" UX) - enforced server-side in {@code
 * StudentQuestionResponse#from} (never trust the client to hide it), and has no effect at all when
 * {@code audioPath} is null (no audio = content always shown, regardless of this flag).
 * <p>
 * {@code questionType} was added 2026-09-01 for the "speaking question" feature - see {@link
 * QuestionType}'s javadoc for the full design (a SPEAKING question has no {@link Choice}s, is
 * answered by the Student recording their voice instead of picking one, and is never auto-graded
 * or counted toward the test's score). Stored as a plain String (its {@code name()}), same
 * pattern as {@code Test#getStatus()}/{@code Test#getTestType()} - every Question before this
 * field existed was backfilled to {@code MULTIPLE_CHOICE} by {@code
 * V10__speaking_question.sql}, and every new Question must set it explicitly (see {@code
 * QuestionService#newQuestion}), same "DEFAULT only backfills old rows" caveat already documented
 * on {@code Test#testType}.
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

    /** Duong dan tuong doi/ten file audio da luu tren server, null neu cau hoi nay chua co audio. */
    private String audioPath;

    /** true = an "content" khoi man hinh lam bai cua Hoc sinh khi cau hoi co audio (bat buoc nghe moi biet). Khong co tac dung neu audioPath null. Mac dinh false/null (hien content nhu cu). */
    private Boolean hideContentInTest;

    /** {@link QuestionType#name()} - MULTIPLE_CHOICE (mac dinh, cau hoi trac nghiem nhu cu) hoac SPEAKING (cau hoi hoc sinh tra loi bang cach ghi am giong noi, xem QuestionType's javadoc). */
    private String questionType;
}
