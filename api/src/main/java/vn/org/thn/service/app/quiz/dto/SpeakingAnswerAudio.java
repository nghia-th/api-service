package vn.org.thn.service.app.quiz.dto;

/**
 * In-memory result of reading a Student's recorded SPEAKING answer back off disk ({@code
 * StudentAttemptService#loadSpeakingAnswerAudio}) - same shape/reasoning as {@link QuestionAudio}
 * for a Question's own listening-prompt clip: not wrapped in {@code ApiResponse} since the
 * controller returns it as a raw file download, not JSON. Kept as its own record rather than
 * reusing {@link QuestionAudio} even though the shape is identical, so the type name at each call
 * site says whose audio it is (the Question's prompt vs. the Student's answer) - the two are
 * frequently in play on the same screen (a "listen then speak" question).
 */
public record SpeakingAnswerAudio(byte[] content, String contentType, String filename) {
}
