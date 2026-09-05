-- Admin-managed "bo sach" (curriculum) lookup list (2026-09-05), replacing the previously
-- hardcoded 3-value list validated in LibraryService#upload. LibraryDocument.curriculum keeps
-- storing the plain name (not a foreign key - see Curriculum.java's javadoc), so no change is
-- needed to that column or any existing row. Starts empty - the old 3 names are NOT auto-seeded,
-- per the user's explicit choice (AskUserQuestion 2026-09-05: "Khong, de trong, Admin tu them").
CREATE TABLE curriculum
(
    id         BIGINT IDENTITY (1,1) PRIMARY KEY,
    name       NVARCHAR(255) NOT NULL,
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT           NOT NULL DEFAULT 0,

    CONSTRAINT uq_curriculum_name UNIQUE (name)
);
