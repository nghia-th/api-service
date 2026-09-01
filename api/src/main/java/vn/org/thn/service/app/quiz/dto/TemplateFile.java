package vn.org.thn.service.app.quiz.dto;

/** In-memory result of {@code QuestionImportService#generateTemplate} - not wrapped in {@code ApiResponse} since the controller returns it as a raw file download, not JSON. */
public record TemplateFile(byte[] content, String contentType, String filename) {
}
