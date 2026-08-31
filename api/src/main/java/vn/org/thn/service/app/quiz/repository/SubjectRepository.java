package vn.org.thn.service.app.quiz.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.quiz.entity.Subject;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class SubjectRepository extends BaseRepositoryImpl<Subject, Long> {
}
