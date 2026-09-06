-- Cho phep 1 Mon hoc (Subject) KHONG thuoc Lop nao - ap dung CHUNG cho MOI Lop cua Phu huynh
-- (2026-09-06, theo yeu cau cua anh: "anh co 1 mon hoc ma khong thuoc lop nao vi du anh co mon
-- lap trinh python thi cac hoc sinh cua phu huynh deu hoc muon nay khong phan biet lop"). Truoc
-- day 1 Subject bat buoc thuoc DUNG 1 Classroom, khong co cot parent_id rieng (quyen so huu suy
-- ra qua Classroom). Tu ban sua nay: them cot parent_id TRUC TIEP tren subject (backfill tu
-- classroom.parent_id cua du lieu cu), va noi long classroom_id thanh NULLABLE - classroom_id
-- NULL nghia la "Mon nay dung chung cho moi Lop cua Phu huynh nay" (khac voi truoc, luon bat buoc
-- chon dung 1 Lop).
--
-- KHONG DROP+CREATE nhu vai migration truoc (V5, V7) - Subject/Lesson/Question/Test da co du lieu
-- that (Phu huynh da tao mon/bai/de that su, khac han Timetable/LessonPreparation la tinh nang
-- moi lam trong ngay o thoi diem do). Day la ALTER migration DAU TIEN cua du an nay giu nguyen du
-- lieu cu - xac nhan voi anh (2026-09-06) truoc khi lam: hien chua co Subject nao trung ten tao
-- rieng theo tung Lop can gop lai, nen KHONG can cong cu gop du lieu - tinh nang nay chi phuc vu
-- Subject dung chung tao MOI tu bay gio tro di.
-- SQLite khong ho tro ALTER COLUMN de doi NOT NULL/them FK constraint tren cot cu - phai dung
-- cach "rebuild bang": tao bang moi dung schema, copy du lieu qua (JOIN sang classroom de backfill
-- parent_id), xoa bang cu, doi ten bang moi thanh ten cu. Khong co index/trigger nao khac tren
-- subject can tao lai (da kiem tra khong co CREATE INDEX nao tren bang nay o cac migration truoc).
PRAGMA foreign_keys = OFF;

CREATE TABLE subject_new
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    classroom_id INTEGER,
    parent_id    INTEGER NOT NULL,
    name         TEXT    NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   TEXT,
    updated_by   TEXT,
    deleted      BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_subject_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id)
);

INSERT INTO subject_new (id, classroom_id, parent_id, name, created_at, updated_at, created_by, updated_by, deleted)
SELECT s.id, s.classroom_id, c.parent_id, s.name, s.created_at, s.updated_at, s.created_by, s.updated_by, s.deleted
FROM subject s
         JOIN classroom c ON c.id = s.classroom_id;

DROP TABLE subject;
ALTER TABLE subject_new RENAME TO subject;

CREATE INDEX idx_subject_parent ON subject (parent_id);

UPDATE sqlite_sequence SET name = 'subject' WHERE name = 'subject_new';

PRAGMA foreign_keys = ON;
