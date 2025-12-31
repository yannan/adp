package com.eisoo.metadatamanage.lib.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.vo
 * @Date: 2023/5/18 15:33
 */
@AllArgsConstructor
@Data
public class CheckErrorVo {

    @JsonProperty("Key")
    private String errorCode;

    @JsonProperty("Message")
    private String errorMsg;
}
