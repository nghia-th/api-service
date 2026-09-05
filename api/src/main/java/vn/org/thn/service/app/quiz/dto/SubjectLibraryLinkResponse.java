package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.LibraryDocument;
import vn.org.thn.service.app.quiz.entity.SubjectLibraryLink;

import java.time.LocalDateTime;

/** One row of "documents linked to this Subject" (Parent's {@code GET .../subjects/{id}/links} and the equivalent Student endpoint) - embeds the full {@link LibraryDocumentResponse} so the UI doesn't need a second round-trip per linked document. */
@Data
public class SubjectLibraryLinkResponse {
    private Long id;
    private Long subjectId;
    private LibraryDocumentResponse document;
    private LocalDateTime linkedAt;

    public static SubjectLibraryLinkResponse from(SubjectLibraryLink link, LibraryDocument doc) {
        SubjectLibraryLinkResponse response = new SubjectLibraryLinkResponse();
        response.id = link.getId();
        response.subjectId = link.getSubjectId();
        response.document = LibraryDocumentResponse.from(doc);
        response.linkedAt = link.getLinkedAt();
        return response;
    }
}
