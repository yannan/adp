package com.eisoo.metadatamanage.lib.dto;


import com.eisoo.standardization.common.constant.Message;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 数据同步模型DTO
 *
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-06-27 16:51:08
 */
@Data
public class SchedulerProcessRelationDto {


    /**
     * AF数据同步模型UUID
     */
    private String model_uuid;

    /**
     * AF模型类型
     */
    private String model_type;

    /**
     * AF模型依赖节点UUID
     */
    private String dependency;
}
