package com.eisoo.metadatamanage.web.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.eisoo.entity.GraphInfoEntity;
import com.eisoo.metadatamanage.web.service.impl.GraphInfoServiceImpl;
import com.eisoo.util.HttpRequestUtils;
import com.eisoo.util.JsonUtils;
import com.eisoo.util.PasswordEncoder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author: Lan Tian
 * @Date: 2024/4/25 15:49
 * @Version:1.0
 */
@Configuration
@Data
@Slf4j
public class AnyDataGraphConfig {
    @Autowired
    private GraphInfoServiceImpl graphInfoServiceImpl;

    @Value("${anydata.initMaxCount}")
    private Integer initMaxCount;
    @Value("${anydata.url}")
    private String url;
    @Value("${anydata.nodesInfoURL}")
    private String nodesInfoURL;
    @Value("${anydata.nodeInfoCommonURL}")
    private String nodeInfoCommonURL;
    @Value("${anydata.neighborsURL}")
    private String neighborsURL;
    @Value("${anydata.edgeBatchDeleteURL}")
    private String edgeBatchDeleteURL;
    @Value("${anydata.kafka.group-topic}")
    private String kafkaTopic;
    @Value("${anydata.appIdURL}")
    private String appIdURL;
    @Value("${anydata.userName}")
    private String userName;
    @Value("${anydata.password}")
    private String password;
    private String appId;
    @Value("${anydata.build.graphName}")
    private String graphName;
    @Value("${anydata.build.graphDesc}")
    private String graphDesc;
    @Value("${anydata.build.networkURL}")
    private String networkURL;
    @Value("${anydata.build.networkGetIdByNameURL}")
    private String networkGetIdByNameURL;
    @Value("${anydata.build.dsURL}")
    private String dsURL;
    @Value("${anydata.build.dsGetInfoURL}")
    private String dsGetInfoURL;
    @Value("${anydata.build.dsDeleteURL}")
    private String dsDeleteURL;

    @Value("${anydata.build.uploadURL}")
    private String uploadURL;
    @Value("${anydata.build.serviceBuildURL}")
    private String serviceBuildURL;
    @Value("${anydata.build.serviceQueryURL}")
    private String serviceQueryURL;
    @Value("${anydata.build.serviceCancelURL}")
    private String serviceCancelURL;
    @Value("${anydata.build.serviceDeletelURL}")
    private String serviceDeletelURL;
    private Map<String, Object> headMap;
    private Integer knwId;
    private Integer dsId;
    private Integer graphId;

    @Value("${anydata.build.dataSource.dsOldId}")
    @JsonIgnore
    private String dsOldId;
    @Value("${anydata.build.startBuildURL}")
    @JsonIgnore
    private String startBuildURL;
    @Value("${anydata.build.startBuildProgressURL}")
    @JsonIgnore
    private String startBuildProgressURL;//full(全量构建)，increment(增量构建)
    @Value("${anydata.build.startDeleteURL}")
    private String startDeleteURL;//删除图谱
    @Value("${anydata.build.startQueryAllGraphURL}")
    private String startQueryAllGraphURL;//根据图谱name查询出所有的图谱信息

    @Value("${anydata.build.upLoadGraphPath}")
    private String upLoadGraphPath;
    @Value("${anydata.build.upLoadGraphServicePath}")
    private String upLoadGraphServicePath;

    @Value("${anydata.anyDataLineageQueryURL}")
    private String anyDataLineageQueryURL;
    @PostConstruct
    public void initHeadMap() {
//        initTask();
    }
    private void initTask() {
        this.headMap = new HashMap<String, Object>();
        try {
            this.password = PasswordEncoder.encrypt(this.password);
        } catch (Exception e) {
            log.error("PasswordEncoder.encrypt(this.password)失败!", e);
            throw new RuntimeException(e);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("isRefresh", 0);
        map.put("password", this.password);
        map.put("username", this.userName);
        String response = null;
        try {
            response = HttpRequestUtils.sendHttpsPosToAdLineageJson(this.appIdURL, null, JsonUtils.toJsonString(map));
        } catch (UnsupportedEncodingException e) {
            log.error("请求ad失败!", e);
            throw new RuntimeException(e);
        }
        JsonNode jsonNode = JsonUtils.toJsonNode(response);
        assert jsonNode != null;
        if (jsonNode.hasNonNull("res")) {
            String res = jsonNode.get("res").asText();
            this.appId = res;
            this.headMap.put("appId", this.appId);
            QueryWrapper<GraphInfoEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("app_id", this.appId);
            GraphInfoEntity graphInfoEntity = graphInfoServiceImpl.getOne(queryWrapper);
            if (null != graphInfoEntity) {
                this.graphId = graphInfoEntity.getGraphId();
            }
        } else {
            log.error("更新appId失败!细节如下:{}", response);
            throw new RuntimeException();
        }
    }
}
