package com.eisoo.metadatamanage.lib.vo;

import com.eisoo.standardization.common.util.AiShuUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.vo
 * @Date: 2023/5/18 15:31
 */
@AllArgsConstructor
@Data
public class CheckVo<T> {

    protected CheckVo() {

    }

    public CheckVo(String checkCode, T checkData) {
        this.checkCode = checkCode;
        this.checkData = checkData;
    }

    public CheckVo(String checkCode, List<CheckErrorVo> checkErrors, T checkData) {
        this.checkCode = checkCode;
        this.checkErrors = checkErrors;
        this.checkData = checkData;
    }

    /**
     * 校验码
     */
    private String checkCode;

    /**
     * 校验返回对象
     */
    private T checkData;

    /**
     * 校验返回的错误信息
     */
    private List<CheckErrorVo> checkErrors;

    public String getCheckErrorsString(List<CheckErrorVo> list) {
        if (AiShuUtil.isEmpty(list)) {
            return null;
        } else {
            StringBuilder result = new StringBuilder();
            list.forEach(error -> {
                        result.append(error.getErrorMsg());
                        result.append("；");
                    }
            );
            if (result.length() > 0) {
                result.deleteCharAt(result.length() - 1);
            }
            return result.toString();
        }
    }

}
