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
 * <p>
 * {@code answerAudioPath}/{@code parentMarkedCorrect} were added 2026-09-01 for the "speaking
 * question" feature (see {@link vn.org.thn.service.app.quiz.entity.QuestionType}'s javadoc) -
 * used INSTEAD of {@code choiceId} when this row's Question is a SPEAKING type ({@code choiceId}
 * stays null for those rows; the two are mutually exclusive by construction, never validated as
 * a DB constraint since MyBatis has no CHECK-constraint support in this codebase). {@code
 * answerAudioPath} stores only the server-side filename of the Student's recorded answer (never
 * the raw bytes), same "path in DB, bytes on disk" pattern as {@code Question.audioPath} - see
 * {@code StudentAttemptService#uploadSpeakingAnswer}/{@code #loadSpeakingAnswerAudio}. {@code
 * parentMarkedCorrect} is a tri-state reference note only the owning Parent sets ({@code
 * ReportService#gradeSpeakingAnswer}, only callable after the Attempt is submitted) - null means
 * "not reviewed yet"; unlike {@code correct}, it is NEVER read by any score computation ({@code
 * Attempt.correctCount}/{@code scorePercent}), per the user's explicit "khong tinh diem, chi de
 * tham khao" answer when this feature was scoped.
 * <p>
 * {@code answerText} was added 2026-09-01 for the Question's own {@code answerMode} (see {@link
 * vn.org.thn.service.app.quiz.entity.AnswerMode}) - the Student's TYPED answer, alongside (not
 * instead of) {@code answerAudioPath}: both may be non-null at once for a BOTH-mode question,
 * there is no exclusivity enforced. Saved via its own endpoint ({@code
 * StudentAttemptService#saveSpeakingTextAnswer}), same "locked once submitted" rule as the audio
 * answer.
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

    /** Duong dan/ten file ghi am cau tra loi cua Hoc sinh cho cau hoi dang SPEAKING, null neu chua tra loi (hoac cau hoi nay la MULTIPLE_CHOICE). */
    private String answerAudioPath;

    /** Ghi chu doi chieu cua Phu huynh cho cau tra loi SPEAKING - null = chua cham, true/false = Phu huynh tu danh gia. Khong anh huong diem so tu dong. */
    private Boolean parentMarkedCorrect;

    /** Cau tra loi go chu (khong phai ghi am) cua Hoc sinh cho cau hoi SPEAKING dang TEXT/BOTH - null neu chua nhap hoac cau hoi la MULTIPLE_CHOICE/AUDIO-only chua duoc dung toi. Doc lap voi answerAudioPath (khong loai tru nhau). */
    private String answerText;
}
