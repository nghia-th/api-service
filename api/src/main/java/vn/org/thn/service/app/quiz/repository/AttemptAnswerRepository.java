package vn.org.thn.service.app.quiz.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.quiz.entity.AttemptAnswer;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class AttemptAnswerRepository extends BaseRepositoryImpl<AttemptAnswer, Long> {
}
