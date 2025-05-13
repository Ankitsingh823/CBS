package com.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class ServiceConfigCompareDTO {

    @EqualsAndHashCode.Include
    private Long configId;

    private String name;
    private Integer version1;
    private Integer version2;
    private String value1;
    private String value2;
}
