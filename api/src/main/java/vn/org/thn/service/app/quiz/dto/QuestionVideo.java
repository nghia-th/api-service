package vn.org.thn.service.app.quiz.dto;

/**
 * In-memory result of reading a Question's video clip back off disk ({@code
 * QuestionService#loadVideo}, 2026-09-04) - same shape/reasoning as {@link QuestionAudio} for a
 * Question's audio clip: not wrapped in {@code ApiResponse} since the controller returns it as a
 * raw file download, not JSON.
 */
public record QuestionVideo(byte[] content, String contentType, String filename) {
}
