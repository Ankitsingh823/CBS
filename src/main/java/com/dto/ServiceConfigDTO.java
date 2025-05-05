package com.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ServiceConfigDTO {

    private String name;
    private String description;
    private String value;
    private String status;
    private String updatedBy;
}
