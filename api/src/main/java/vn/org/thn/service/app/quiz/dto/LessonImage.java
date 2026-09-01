package vn.org.thn.service.app.quiz.dto;

/**
 * In-memory result of reading a Lesson's illustrative image back off disk ({@code
 * LessonService#loadImage}) - same shape/reasoning as {@link TemplateFile} for import templates:
 * not wrapped in {@code ApiResponse} since the controller returns it as a raw file download, not
 * JSON.
 */
public record LessonImage(byte[] content, String contentType, String filename) {
}
