package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.LessonAttachment;

import java.time.LocalDateTime;

/** Safe response view of {@link LessonAttachment} - excludes {@code filePath} (server-internal storage filename, never exposed to clients, same convention as {@link LibraryDocumentResponse}). */
@Data
public class LessonAttachmentResponse {
    private Long id;
    private String originalName;
    private long fileSize;
    private String contentType;
    private LocalDateTime createdAt;

    public static LessonAttachmentResponse from(LessonAttachment attachment) {
        LessonAttachmentResponse response = new LessonAttachmentResponse();
        response.id = attachment.getId();
        response.originalName = attachment.getOriginalName();
        response.fileSize = attachment.getFileSize();
        response.contentType = attachment.getContentType();
        response.createdAt = attachment.getCreatedAt();
        return response;
    }
}
