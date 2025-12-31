package com.eisoo.metadatamanage.web.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class VirtualizationConfiguration {
    @Value("${virtualization.host}")
    private String host;
    @Value("${virtualization.port}")
    private Integer port;
    @Value("${virtualization.schemaApi}")
    private String schemaApi;
    @Value("${virtualization.tableApi}")
    private String tableApi;
    @Value("${virtualization.columnApi}")
    private String columnApi;
    @Value("${virtualization.collectorApi}")
    private String collectorApi;
    @Value("${virtualization.protocol}")
    private String protocol;
    @Value("${virtualization.user}")
    private String user;
    @Value("${virtualization.isAuto}")
    private Boolean isAuto;
}
