package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.LibraryDocumentFile;

import java.time.LocalDateTime;

/** Safe response view of {@link LibraryDocumentFile} - excludes {@code filePath} (server-internal storage filename, never exposed to clients, same convention as {@link LessonAttachmentResponse}). */
@Data
public class LibraryDocumentFileResponse {
    private Long id;
    private String originalName;
    private long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;

    public static LibraryDocumentFileResponse from(LibraryDocumentFile file) {
        LibraryDocumentFileResponse response = new LibraryDocumentFileResponse();
        response.id = file.getId();
        response.originalName = file.getOriginalName();
        response.fileSize = file.getFileSize();
        response.contentType = file.getContentType();
        response.uploadedAt = file.getUploadedAt();
        return response;
    }
}
