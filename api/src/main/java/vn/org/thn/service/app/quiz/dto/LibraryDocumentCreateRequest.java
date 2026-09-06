package vn.org.thn.service.app.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin's "create a library document" request (2026-09-06 revision) - {@code grade}/{@code
 * curriculum} are now both optional (a general "mon hoc" like "Lap trinh Python" has neither),
 * and there is no file field here anymore - files are added afterward, one at a time, via {@code
 * POST /api/admin/library/{id}/files} ({@link LibraryDocumentFileResponse}), since a document can
 * now carry any number of them. {@code subjectName} stays required - it is the entry's actual
 * name regardless of whether grade/curriculum apply.
 */
@Data
public class LibraryDocumentCreateRequest {
    private Integer grade;
    @NotBlank
    private String subjectName;
    private String curriculum;
    private String volume;
    private String title;
}
