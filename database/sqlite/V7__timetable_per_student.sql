-- Thoi khoa bieu doi tu gan theo Lop (classroom_id) sang gan theo Hoc sinh (student_id) -
-- 2026-09-06, theo yeu cau cua anh: "hien tai tao thoi khoa bieu theo lop dung ra la thoi khoa
-- bieu theo hoc sinh boi vi phu huynh co 2 con cung hoc mot lop nhung thoi khoa bieu khac nhau".
-- Truoc day 1 Lop chi co 1 thoi khoa bieu dung chung cho moi Hoc sinh trong lop, nhung 2 anh em
-- hoc chung 1 lop van co the hoc lich khac nhau (hoc them, nghi som,...) nen phai tach rieng theo
-- tung Hoc sinh.
-- Xoa sach du lieu cu (DROP + CREATE lai, khong ALTER) theo dung tien le cua V5 - AskUserQuestion
-- 2026-09-06 ("Xoa sach, lam lai tu dau"): khong the quy doi 1-1 tu classroom_id sang student_id
-- (1 Lop co nhieu Hoc sinh, khong biet gan ban ghi cu cho Hoc sinh nao), tinh nang con moi, chua
-- co du lieu that can giu.
-- Viec kiem tra 1 subject_id co thuoc dung Lop cua Hoc sinh do hay khong van duoc giu nguyen o
-- tang service (khong doi ve schema), chi doi cot khoa ngoai chinh cua bang nay.
DROP TABLE timetable_entry;

CREATE TABLE timetable_entry
(
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    student_id  INTEGER NOT NULL,
    day_of_week INTEGER NOT NULL,
    subject_id  INTEGER NOT NULL,
    order_index INTEGER NOT NULL,
    created_at  DATETIME,
    updated_at  DATETIME,
    created_by  TEXT,
    updated_by  TEXT,
    deleted     BOOLEAN NOT NULL DEFAULT 0,

    CONSTRAINT fk_timetable_entry_student FOREIGN KEY (student_id) REFERENCES student (id),
    CONSTRAINT fk_timetable_entry_subject FOREIGN KEY (subject_id) REFERENCES subject (id)
);
CREATE INDEX idx_timetable_entry_student_day ON timetable_entry (student_id, day_of_week);
