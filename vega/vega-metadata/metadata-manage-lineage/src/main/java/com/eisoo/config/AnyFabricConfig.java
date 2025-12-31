package com.eisoo.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @Author: Lan Tian
 * @Date: 2025/1/6 10:42
 * @Version:1.0
 */
@Configuration
@Data
@Slf4j
public class AnyFabricConfig {
    @Value("${af-data-view.indicator-lineage}")
    private String indicatorLineageUrl;
    @Value("${af-data-view.data-lineage}")
    private String dataLineageUrl;
    @Value("${af-data-view.table-column-info-lineage}")
    private String tableColumnInfoLineageUrl;
    @Value("${af-data-view.check-task-lineage}")
    private String checkTaskLineageUrl;
}
