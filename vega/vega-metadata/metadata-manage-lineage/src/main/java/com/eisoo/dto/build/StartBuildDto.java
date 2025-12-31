package com.eisoo.dto.build;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:21
 * @Version:1.0
 */
@Data
@Component
public class StartBuildDto {
    @JsonProperty(value = "tasktype")
    private String taskType = "full";//full(全量构建)，increment(增量构建)
//    @Value("${anydata.build.startBuildURL}")
//    @JsonIgnore
//    private String startBuildURL;//full(全量构建)，increment(增量构建)
//    @Value("${anydata.build.startBuildProgressURL}")
//    @JsonIgnore
//    private String startBuildProgressURL;//full(全量构建)，increment(增量构建)
}
