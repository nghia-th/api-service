package vn.org.thn.service.app.quiz.dto;

/** In-memory result of reading a library document's PDF back off disk ({@code LibraryService#loadFile}) - same shape/reasoning as {@link LessonImage}: not wrapped in {@code ApiResponse} since the controller returns it as a raw file download, not JSON. */
public record LibraryFile(byte[] content, String contentType, String filename) {
}
