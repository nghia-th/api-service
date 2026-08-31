package vn.org.thn.service.app.quiz.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.quiz.entity.Lesson;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class LessonRepository extends BaseRepositoryImpl<Lesson, Long> {
}
