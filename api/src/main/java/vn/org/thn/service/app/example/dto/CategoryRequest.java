package vn.org.thn.service.app.example.dto;

import lombok.Data;

@Data
public class CategoryRequest {

    private String code;
    private String name;
    private Boolean active;
}
