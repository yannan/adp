package com.eisoo.metadatamanage.db.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Data;

/**
 * 
 * 
 * @author aishu.cn
 * @email xxxx@aishu.cn
 * @date 2023-02-23 17:12:29
 */
@Data
@TableName("t_schema")
public class SchemaEntity implements Serializable {
	/**
	 * 唯一id，雪花算法
	 */
	@TableId(value = "f_id", type = IdType.ASSIGN_ID)
    private Long id;

	/**
	 * schema名称
	 */
	@TableField(value = "f_name")
	private String name;

	/**
	 * 数据源唯一标识
	 */
	@TableField(value = "f_data_source_id")
	private String dataSourceId;

	/**
	 * 冗余字段，数据源名称
	 */
	@TableField(value = "f_data_source_name")
	private String dataSourceName;

	/**
	 * 冗余字段，数据源类型，关联字典表f_dict_type为1时的f_dict_key
	 */
	@TableField(value = "f_data_source_type")
	private Integer dataSourceType;

	/**
	 * 冗余字段，数据源类型名称，对应字典表f_dict_type为1时的f_dict_value
	 */
	@TableField(value = "f_data_source_type_name")
	private String dataSourceTypeName;

	/**
	 * 权限域（目前为预留字段），默认0
	 */
	@TableField(value = "f_authority_id")
	private String authorityId;

	/**
	 * 创建时间
	 */
	@TableField(value = "f_create_time")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 创建用户（ID），默认空字符串
	 */
	@TableField(value = "f_create_user")
	private String createUser;

	/**
	 * 修改时间，默认当前时间
	 */
	@TableField(value = "f_update_time")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
	private Date updateTime;

	/**
	 * 修改用户（ID），默认空字符串
	 */
	@TableField(value = "f_update_user")
	private String updateUser;


}
