package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

@Data
public class SchedulerProjectDto {
    Long id;
    Long userId;
    String userName;
    Long code;
    String name;
    String description;
    String createTime;
    String updateTime;
    Long perm;
    Integer defCount;
    Integer instRunningCount;
}
