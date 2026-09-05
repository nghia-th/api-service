package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.LibraryDocument;

import java.time.LocalDateTime;

/** Safe response view of {@link LibraryDocument} - excludes {@code filePath} (server-internal storage filename, never exposed to clients - the actual file is only reachable through the download endpoints, see {@code LibraryService}'s javadoc). */
@Data
public class LibraryDocumentResponse {
    private Long id;
    private int grade;
    private String subjectName;
    private String curriculum;
    private String volume;
    private String title;
    private long fileSize;
    // 2026-09-05 (item 1 of the 11-item batch request) - a row created via bulk import has no
    // file yet (LibraryService#createMetadataOnly stores filePath="" rather than a nullable
    // column - see that method's javadoc); the frontend uses this flag to show an "upload PDF"
    // action for such rows instead of the normal view/download icons.
    private boolean hasFile;
    private LocalDateTime createdAt;

    public static LibraryDocumentResponse from(LibraryDocument doc) {
        LibraryDocumentResponse response = new LibraryDocumentResponse();
        response.id = doc.getId();
        response.grade = doc.getGrade();
        response.subjectName = doc.getSubjectName();
        response.curriculum = doc.getCurriculum();
        response.volume = doc.getVolume();
        response.title = doc.getTitle();
        response.fileSize = doc.getFileSize();
        response.hasFile = doc.getFilePath() != null && !doc.getFilePath().isBlank();
        response.createdAt = doc.getCreatedAt();
        return response;
    }
}
