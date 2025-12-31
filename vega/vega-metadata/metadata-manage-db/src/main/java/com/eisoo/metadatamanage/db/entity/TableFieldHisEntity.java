package com.eisoo.metadatamanage.db.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.baomidou.mybatisplus.annotation.*;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 * 
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-02-23 17:12:30
 */
@Data
@TableName("t_table_field_his")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel
public class TableFieldHisEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * 唯一id，雪花算法
	 */
	@TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private Long id;

	/**
	 * 字段名称
	 */
	@Excel(name = "*字段名称", orderNum = "1", width = 30)
	@ApiModelProperty(value = "字段名称", example = "field1", dataType = "java.lang.String")
    @Length(max = 128, message = Messages.MESSAGE_LENGTH_MAX_CHAR_128)
    @NotBlank(message = "字段名称" + Messages.MESSAGE_INPUT_NOT_EMPTY)
	@TableField(value = "f_field_name")
	private String fieldName;

	/**
	 * 字段类型，关联字典表f_dict_type为2|3|4...时的f_dict_key
	 */
	@Excel(name = "*字段类型", orderNum = "2", width = 20 , dict = "fieldType", addressList = true)
	@ApiModelProperty(value = "字段类型", example = "1", dataType = "java.lang.Integer")
    @NotNull(message = "字段类型" + Messages.MESSAGE_INPUT_NOT_EMPTY)
	@TableField(value = "f_field_type")
	private Integer fieldType;

	/**
	 * 字段长度
	 */
	@Excel(name = "字段长度", orderNum = "3")
	@ApiModelProperty(value = "字段长度", example = "10", dataType = "java.lang.Integer")
    @Range(min = 1, message = "字段长度值只能为空或大于等于1")
	@TableField(value = "f_field_length", insertStrategy = FieldStrategy.IGNORED, updateStrategy = FieldStrategy.IGNORED)
	private Integer fieldLength;

	/**
	 * 字段精度
	 */
	@Excel(name = "字段精度", orderNum = "4")
	@ApiModelProperty(value = "字段精度", example = "0", dataType = "java.lang.Integer")
    @Range(min = 0, message = "字段精度值只能为空或大于等于0")
	@TableField(value = "f_field_precision", insertStrategy = FieldStrategy.IGNORED, updateStrategy = FieldStrategy.IGNORED)
	private Integer fieldPrecision;

	/**
	 * 字段注释，默认为空字符串
	 */
	@Excel(name = "*字段注释", orderNum = "5")
	@ApiModelProperty(value = "字段注释", example = "", dataType = "java.lang.String")
    @Length(max = 1024, message = "字段注释长度不能超过1024")
	@TableField(value = "f_field_comment")
	private String fieldComment;

	/**
	 * Table唯一标识
	 */
	@JsonIgnore
	@TableField(value = "f_table_id")
	private Long tableId;
	/**
	 * Table版本号
	 */
	@JsonIgnore
	@TableField(value = "f_version")
	private Integer version;

	/**
	 * 高级参数，默认为"[]"，格式为"[{key:key1, value:value1}]"
	 */
	@TableField(value = "f_advanced_params")
	private String advancedParams;

	public List<String> validate(Set<Integer> fieldTypSet) {
        List<String> errors = new ArrayList<>();
        if (!fieldTypSet.contains(this.fieldType)) {
            errors.add("字段类型非法");
        }

        if (this.fieldPrecision != null) {
            if (this.fieldLength == null) {
                errors.add("字段长度为空时字段精度也必须为空");
            } else if (this.fieldPrecision > this.fieldLength) {
                errors.add("字段精度必须小于等于字段长度");
            }
        }
        return errors;
    }
}
