package vn.org.thn.service.app.example.entity;

import lombok.Data;
import vn.org.thn.service.base.db.mybatis.annotation.Entity;
import vn.org.thn.service.base.db.mybatis.annotation.GeneratedValue;
import vn.org.thn.service.base.db.mybatis.annotation.GenerationType;
import vn.org.thn.service.base.db.mybatis.annotation.Id;
import vn.org.thn.service.base.db.mybatis.annotation.Table;

/**
 * DB test entity #1: single auto-increment primary key - the most common shape. Exercises plain
 * CRUD (save/findById/deleteById), {@code InsertExecutor.save()}'s INSERT-vs-UPDATE decision for
 * an identity column, and {@code QueryBuilder} filtering/ordering/paging via
 * {@code CategoryService#search}.
 */
@Data
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;
    private Boolean active = true;
}
