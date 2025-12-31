package com.eisoo.metadatamanage.lib.dto;

import lombok.Data;

import java.util.List;

@Data
public class SchedulerTaskDto {
    String name;
    String base_task_type;
    String uuid;
    List<AdvancedDTO> advanced_params;
}
