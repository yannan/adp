package com.eisoo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/9 21:28
 * @Version:1.0
 */
@Data
@AllArgsConstructor
public class IndicatorLineageDto implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonProperty(value = "uuid")
    private String uuid;
}
