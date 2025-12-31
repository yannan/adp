package com.eisoo.metadatamanage.web.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class DpDataSourceConfiguration {
    @Value("${dpDataSource.host}")
    private String host;
    @Value("${dpDataSource.port}")
    private Integer port;
    @Value("${dpDataSource.catalogApi}")
    private String catalogApi;
    @Value("${dpDataSource.connectorsApi}")
    private String connectorsApi;
    //    @Value("${dpDataSource.user}")
//    private String user;
    @Value("${dpDataSource.protocol}")
    private String protocol;
    @Value("${dpDataSource.collector}")
    private String collector;
    @Value("${dpDataSource.metaDataHost}")
    private String metaDataHost;
    @Value("${dpDataSource.metaDataPort}")
    private Integer metaDataPort;
    @Value("${dpDataSource.metaDataUser}")
    private String metaUser;
    @Value("${dpDataSource.metaDataPassWord}")
    private String metaPassword;
    @Value("${dpDataSource.metaDataConnector}")
    private String metaDataConnector;
}
