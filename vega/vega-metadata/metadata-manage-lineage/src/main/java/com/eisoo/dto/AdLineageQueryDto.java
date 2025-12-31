package com.eisoo.dto;

import com.eisoo.util.Constant;
import com.eisoo.util.LineageUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/16 15:21
 * @Version:1.0
 */
@Data
public class AdLineageQueryDto {
    @JsonProperty(value = "service_name")
    private String serviceName = "血缘查询服务";
    private String uuid;
    private String step = "5";
    @JsonProperty(value = "direction_in")
    private String directionIn = "-";
    @JsonProperty(value = "direction_out")
    private String directionOut = "-";
    private String type = "";
    @JsonProperty(value = "id_type")
    private String idType;


    //    {"service_name": "血缘查询服务", "step": "1","uuid": "column1","direction_in": "<-","type": "column","direction_out": "<-"}
    public AdLineageQueryDto(String id, String type, String direction, String step) {
        switch (direction) {
            case "forward":
                this.directionIn = "-";
                this.directionOut = "->";
                break;
            case "reversely":
                this.directionIn = "<-";
                this.directionOut = "-";
                break;
            case "bidirect":
                this.directionIn = "-";
                this.directionOut = "-";
                break;
        }
        switch (type) {
            case Constant.COLUMN:
                this.idType = "unique_id";
                break;
            case Constant.TABLE:
            case Constant.INDICATOR:
                this.idType = "uuid";
                break;
        }
        this.type = type;
        this.uuid = id;
        if (LineageUtil.isNotEmpty(step)) {
            this.step = step;
        }
    }

    public AdLineageQueryDto(String type, String id) {
        this.serviceName = type;
        this.uuid = id;
    }

}
