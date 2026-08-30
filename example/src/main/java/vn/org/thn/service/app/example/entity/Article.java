package vn.org.thn.service.app.example.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;
import vn.org.thn.service.base.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * DB test entity #4: extends {@link BaseEntity} to exercise the shared audit columns
 * (createdAt/updatedAt/createdBy/updatedBy/deleted). As documented on {@link BaseEntity}, none of
 * those 4 fields are auto-populated - {@code ArticleService} sets them by hand before {@code save()}.
 * Also has its own auto-increment id (like {@link Category}) plus a {@code views} counter updated
 * via {@code UpdateBuilder#set} instead of a full-row {@code save()}, to exercise that builder too.
 * <p>
 * {@code @EqualsAndHashCode(callSuper = true)}/{@code @ToString(callSuper = true)} are required
 * here because {@link BaseEntity} also carries its own {@code @Data}: without {@code callSuper},
 * the generated {@code equals}/{@code hashCode}/{@code toString} would silently ignore the 5
 * inherited audit fields.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "article")
public class Article extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private Long views = 0L;
}
