package com.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ServiceConfigCompareDTO {

    private Long configId;
    private String name;
    private Integer version1;
    private Integer version2;
    private String value1;
    private String value2;
}
