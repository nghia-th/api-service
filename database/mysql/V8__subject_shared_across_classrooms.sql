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
ALTER TABLE subject
    ADD COLUMN parent_id BIGINT;

UPDATE subject s
    JOIN classroom c ON c.id = s.classroom_id
SET s.parent_id = c.parent_id;

ALTER TABLE subject
    MODIFY COLUMN parent_id BIGINT NOT NULL;
ALTER TABLE subject
    MODIFY COLUMN classroom_id BIGINT NULL;
ALTER TABLE subject
    ADD CONSTRAINT fk_subject_parent FOREIGN KEY (parent_id) REFERENCES parent (id);

CREATE INDEX idx_subject_parent ON subject (parent_id);
