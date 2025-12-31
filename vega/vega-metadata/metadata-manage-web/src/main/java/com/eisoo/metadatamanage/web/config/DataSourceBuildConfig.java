package com.eisoo.metadatamanage.web.config;

import com.eisoo.util.LineageUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:21
 * @Version:1.0
 */
@Data
@Component
public class DataSourceBuildConfig {
    @JsonProperty(value = "connect_type")
    private String connectType = "";
    @JsonProperty(value = "dataType")
    private String dataType = "structured";
    @JsonProperty(value = "data_source")
    @Value("${anydata.build.dataSource.dataSource}")
    private String dataSource;
    @Value("${anydata.build.dataSource.dsAddress}")
    @JsonProperty(value = "ds_address")
    private String dsaAddress;
    @JsonProperty(value = "ds_auth")
    private String dsaAuth = "";

    @Value("${anydata.build.dataSource.dsPassword}")
    @JsonProperty(value = "ds_password")
    private String dsPassword;
    @Value("${anydata.build.dataSource.dsPath}")
    @JsonProperty(value = "ds_path")
    private String dsPath;
    @Value("${anydata.build.dataSource.dsPort}")
    @JsonProperty(value = "ds_port")
    private Integer dsPort;
    @Value("${anydata.build.dataSource.dsUser}")
    @JsonProperty(value = "ds_user")
    private String dsUser = "root";
    @Value("${anydata.build.dataSource.dsName}")
    @JsonProperty(value = "dsname")
    private String dsName;
    @Value("${anydata.build.dataSource.extractType}")
    @JsonProperty(value = "extract_type")
    private String extractType;
    @JsonProperty(value = "json_schema")
    private String jsonSchema = "";
    @JsonProperty(value = "knw_id")
    private Integer knwId;
    @JsonProperty(value = "queue")
    private String queue = "";
    @JsonProperty(value = "vhost")
    private String vhost = "";

    @PostConstruct
    @JsonIgnore
    public void setDsPassword() {
        this.dsPassword = LineageUtil.getBase64(dsPassword);
    }
}
