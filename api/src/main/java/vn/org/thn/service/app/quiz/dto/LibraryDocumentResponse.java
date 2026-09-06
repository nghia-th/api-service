package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.LibraryDocument;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safe response view of {@link LibraryDocument}. <b>Revision 2026-09-06:</b> {@code grade}/{@code
 * curriculum} are now nullable (a general "mon hoc" has neither - see the entity's javadoc), and
 * the single {@code fileSize}/{@code hasFile} pair is replaced by the full {@link #files} list
 * (a document can carry any number of files now) - {@link #isHasFile()} is kept as a convenience
 * derived from {@code !files.isEmpty()} so the frontend's existing "has a file yet" status check
 * needs no rework.
 */
@Data
public class LibraryDocumentResponse {
    private Long id;
    private Integer grade;
    private String subjectName;
    private String curriculum;
    private String volume;
    private String title;
    private List<LibraryDocumentFileResponse> files;
    private boolean hasFile;
    private LocalDateTime createdAt;

    public static LibraryDocumentResponse from(LibraryDocument doc, List<LibraryDocumentFileResponse> files) {
        LibraryDocumentResponse response = new LibraryDocumentResponse();
        response.id = doc.getId();
        response.grade = doc.getGrade();
        response.subjectName = doc.getSubjectName();
        response.curriculum = doc.getCurriculum();
        response.volume = doc.getVolume();
        response.title = doc.getTitle();
        response.files = files;
        response.hasFile = !files.isEmpty();
        response.createdAt = doc.getCreatedAt();
        return response;
    }
}
