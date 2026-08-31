package vn.org.thn.service.app.example.dto;

import lombok.Data;

@Data
public class ArticleRequest {

    private String title;
    private String content;
    /** No auth/security layer in {@code base} (removed on purpose - see IBase's javadoc), so the
     * acting user is simply supplied by the caller for the audit columns. */
    private String author;
}
