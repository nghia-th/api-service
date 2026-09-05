package vn.org.thn.service.app.quiz.dto;

import lombok.Data;
import vn.org.thn.service.app.quiz.entity.Curriculum;

@Data
public class CurriculumResponse {
    private Long id;
    private String name;

    public static CurriculumResponse from(Curriculum curriculum) {
        CurriculumResponse response = new CurriculumResponse();
        response.id = curriculum.getId();
        response.name = curriculum.getName();
        return response;
    }
}
