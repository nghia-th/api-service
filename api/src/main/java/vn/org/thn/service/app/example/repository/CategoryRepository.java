package vn.org.thn.service.app.example.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.example.entity.Category;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class CategoryRepository extends BaseRepositoryImpl<Category, Long> {
}
