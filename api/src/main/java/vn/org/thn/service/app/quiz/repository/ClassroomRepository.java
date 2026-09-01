package vn.org.thn.service.app.quiz.repository;

import org.springframework.stereotype.Repository;
import vn.org.thn.service.app.quiz.entity.Classroom;
import vn.org.thn.service.base.db.mybatis.repository.BaseRepositoryImpl;

@Repository
public class ClassroomRepository extends BaseRepositoryImpl<Classroom, Long> {
}
