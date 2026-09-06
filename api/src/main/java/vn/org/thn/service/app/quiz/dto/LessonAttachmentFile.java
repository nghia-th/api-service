package vn.org.thn.service.app.quiz.dto;

/** In-memory result of reading a lesson attachment's bytes back off disk ({@code LessonService#loadAttachmentFile}) - same shape/reasoning as {@link LessonImage}: not wrapped in {@code ApiResponse} since the controller returns it as a raw file download, not JSON. {@code filename} here is the attachment's {@code originalName} (unlike {@link LessonImage}/{@link LibraryFile}, which use the server-generated storage filename - a lesson can have several attachments, so the download's filename should be the human-readable one). */
public record LessonAttachmentFile(byte[] content, String contentType, String filename) {
}
