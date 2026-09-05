package vn.org.thn.service.app.quiz.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;
import vn.org.thn.service.base.entity.BaseEntity;

/**
 * A "bo sach" (curriculum/textbook series, e.g. "Ket noi tri thuc") - Admin-managed lookup list
 * for {@link LibraryDocument#getCurriculum()} (2026-09-05, replaces the previous hardcoded
 * 3-value fixed list per the user's explicit request: "chổ bộ sách phải được admin tạo hiện tại
 * đang set cứng").
 * <p>
 * {@link #name} is the value actually stored on {@link LibraryDocument#getCurriculum()} - this is
 * a NAME-based lookup, not a real foreign key (confirmed with the user, AskUserQuestion
 * 2026-09-05: "Bảng riêng + vẫn lưu chuỗi tên"), so existing {@code LibraryDocument} rows and
 * every filter/response DTO that already reads {@code curriculum} as a plain string need no
 * migration. {@code name} is unique (see {@code CurriculumService#create}/{@code #update}) and
 * this table starts EMPTY after the migration (per the user's explicit choice - the old 3
 * hardcoded values are NOT auto-seeded; an Admin must add them, or any other names, themselves).
 * <p>
 * {@code CurriculumService#delete} blocks while any {@link LibraryDocument} still uses this
 * Curriculum's {@code name} - same "protect against orphaning data" reasoning as {@code
 * ClassroomService#delete} blocking on {@code Student}/{@code Subject} children.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "curriculum")
public class Curriculum extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
