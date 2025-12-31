package com.eisoo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: Lan Tian
 * @Date: 2024/12/17 14:07
 * @Version:1.0
 */
@Data
@AllArgsConstructor
public class RelationForwardDto {
    private String id;
    private Integer type;
    private String child;
}
