package com.eisoo.dto.build;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:21
 * @Version:1.0
 */
@Data
public class NetWorkBuildDto {
    @JsonProperty(value = "knw_color")
    private String knwColor = "#126EE3";
    @JsonProperty(value = "knw_des")
    private String knwDes;
    @JsonProperty(value = "knw_name")
    private String knwName;

    public NetWorkBuildDto(String knwName, String knwDes) {
        this.knwName = knwName;
        this.knwDes = knwDes;
    }
}
