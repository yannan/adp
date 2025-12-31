package com.eisoo.metadatamanage.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
public class ExcelMultiThreadImportConfig {
    @Value("${excel-multi-thread-import.batch-import-size:1}")
    private int batchImportSize;

    @Value("${excel-multi-thread-import.max-thread-num-per-task:1}")
    private int maxThreadNumPerTask;
}
