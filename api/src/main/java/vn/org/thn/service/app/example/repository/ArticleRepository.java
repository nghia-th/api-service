package vn.org.thn.service.app.example.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.example.entity.Article;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class ArticleRepository extends BaseRepositoryImpl<Article, Long> {
}
