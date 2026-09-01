package vn.org.thn.service.app.quiz.dto;

/**
 * In-memory result of reading a Question's audio clip back off disk ({@code
 * QuestionService#loadAudio}) - same shape/reasoning as {@link LessonImage} for a Lesson's
 * illustrative image: not wrapped in {@code ApiResponse} since the controller returns it as a raw
 * file download, not JSON.
 */
public record QuestionAudio(byte[] content, String contentType, String filename) {
}
