package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

import java.time.LocalDateTime;

/**
 * One file (PDF or PowerPoint slide deck) attached to a {@link LibraryDocument} (2026-09-06,
 * extending the "thu vien sach giao khoa" feature per the user's explicit choice: a document can
 * now be a general "mon hoc" - e.g. "Lap trinh Python" - with no grade/curriculum, and can carry
 * MULTIPLE files instead of exactly one). Before this revision {@code LibraryDocument} held its
 * single file's {@code filePath}/{@code fileSize} directly; those columns moved here as a genuine
 * one-to-many child table.
 * <p>
 * Same "no edit/soft-delete concept, exists or doesn't" reasoning as {@link SubjectLibraryLink}/
 * {@link LessonAttachment} - a lightweight {@code uploadedAt}/{@code uploadedBy} pair instead of
 * the full {@link BaseEntity} audit suite. {@link #originalName} is kept for the same reason as
 * {@link LessonAttachment#getOriginalName()} - several files under one document need
 * human-readable names to tell them apart.
 */
@Data
@Entity
@Table(name = "library_document_file")
public class LibraryDocumentFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long libraryDocumentId;
    private String originalName;
    private String filePath;
    private long fileSize;
    private String contentType;
    private LocalDateTime uploadedAt;
    private String uploadedBy;
}
