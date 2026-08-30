package vn.org.thn.service.app.example.dto;

import lombok.Data;

@Data
public class TagRequest {

    private String id;
    private String label;
    private Integer priority;
}
