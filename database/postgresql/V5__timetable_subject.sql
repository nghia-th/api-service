-- Thoi khoa bieu doi tu gan dung 1 Lesson cu the sang chi gan Mon hoc (Subject) - 2026-09-06,
-- theo yeu cau cua anh sau khi test ban dau ("thoi khoa bieu la: toan, anh van, hoa", khong can
-- chon dung 1 bai hoc). Tinh nang "chuan bi bai" (bang lesson_preparation) doi tuong tu sang
-- theo doi theo Subject thay vi Lesson (AskUserQuestion 2026-09-06: "doi sang tick theo Mon
-- hoc") - Hoc sinh tick "da chuan bi" cho tung Mon hoc cua ngay mai thay vi tung Bai hoc.
-- Xoa sach du lieu cu cua 2 bang nay (DROP + CREATE lai, khong ALTER) vi thay doi hoan toan y
-- nghia cot - khong the quy doi 1-1 tu lesson_id sang subject_id ma khong mat thong tin (nhieu
-- Lesson khac nhau cung 1 Subject se bi gop lam 1 dong sau khi quy doi, thu tu/so luong dong moi
-- ngay se sai khac ban goc). Chap nhan mat du lieu vi tinh nang moi lam trong ngay, chua co du
-- lieu that can giu. Ten bang/ten class Java (LessonPreparation, TimetableEntry) giu nguyen
-- khong doi de giam pham vi sua doi, chi field/cot ben trong thay tu lesson sang subject.
DROP TABLE lesson_preparation;
DROP TABLE timetable_entry;

CREATE TABLE timetable_entry
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    classroom_id BIGINT  NOT NULL,
    day_of_week  INTEGER NOT NULL,
    subject_id   BIGINT  NOT NULL,
    order_index  INTEGER NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_timetable_entry_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    CONSTRAINT fk_timetable_entry_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
CREATE INDEX idx_timetable_entry_classroom_day ON timetable_entry (classroom_id, day_of_week);

CREATE TABLE lesson_preparation
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id   BIGINT  NOT NULL,
    target_date  DATE    NOT NULL,
    subject_id   BIGINT  NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    deleted      BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_lesson_preparation_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_lesson_preparation_subject FOREIGN KEY (subject_id) REFERENCES subject (id),
    CONSTRAINT uq_lesson_preparation UNIQUE (student_id, target_date, subject_id)
);
CREATE INDEX idx_lesson_preparation_student_date ON lesson_preparation (student_id, target_date);
