-- "Bao bai" - Hoc sinh bao da hoc xong 1 Bai hoc cu the trong 1 Mon (2026-09-06, tinh nang moi
-- "hom nay con hoc gi": "hom nay con hoc toan -> con hoc bai 1"). Khac voi timetable_entry (lich
-- MON hoc theo tuan, khong gan Bai cu the - xem V5 revision) va khac voi lesson_preparation (chi
-- la 1 checklist "da chuan bi hay chua", khong ghi Bai cu the) - bang nay la NHAT KY thuc te: moi
-- dong la 1 lan Hoc sinh xac nhan "da hoc xong Bai nay", co ngay thuc (report_date), giu lai mai
-- mai lam lich su cho Phu huynh xem lai (AskUserQuestion 2026-09-06).
--
-- Quyet dinh thiet ke (AskUserQuestion 2026-09-06):
-- 1. Chi duoc bao Bai cua Mon hoc CO trong Thoi khoa bieu hom do (kiem tra o service, khong phai
--    o DB) - khong luu classroom_id/subject_id rieng, suy ra qua lesson_id -> lesson.subject_id
--    giong quy uoc "khong duplicate FK co the suy ra duoc" da dung xuyen suot du an nay.
-- 2. Duoc bao NHIEU Bai/Mon/ngay (khong gioi han 1 Bai/Mon/ngay).
-- 3. Chi sua/huy duoc trong CHINH NGAY hom do bao (service tu kiem tra report_date = hom nay khi
--    huy, khong co cot rieng danh dau "da khoa").
-- 4. 1 Bai da bao O BAT KY NGAY NAO thi AN VINH VIEN khoi danh sach chon cua Mon do sau nay - the
--    hien qua UNIQUE (student_id, lesson_id): 1 Hoc sinh khong bao giu 1 Bai qua 2 lan.
CREATE TABLE lesson_report
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id   BIGINT  NOT NULL,
    lesson_id    BIGINT  NOT NULL,
    report_date  DATE    NOT NULL,
    created_at   DATETIME,
    updated_at   DATETIME,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_report_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_report_lesson FOREIGN KEY (lesson_id) REFERENCES lesson (id),
    CONSTRAINT uq_lesson_report_student_lesson UNIQUE (student_id, lesson_id),
    INDEX idx_lesson_report_student_date (student_id, report_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
