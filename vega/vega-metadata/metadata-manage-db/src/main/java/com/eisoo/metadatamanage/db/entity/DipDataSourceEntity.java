package com.eisoo.metadatamanage.db.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;
@Data
@TableName("data_source")
public class DipDataSourceEntity implements Serializable {
	private static final long serialVersionUID = 1L;
	@TableId
    private String id;
	@TableField(value = "name")
	private String name;
	@TableField(value = "type_name")
	private String typeName;
	@TableField(value = "bin_data")
	private byte[] binData;
	@TableField(value = "comment")
	private String comment;
	@TableField(value = "created_by_uid")
	private String createdByUid;
	@TableField(value = "created_at")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createdAt;
	@TableField(value = "updated_by_uid")
	private String updatedByUid;
	@TableField(value = "updated_at")
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
	private Date updatedAt;
//	@TableField(value = "catalog_name")
//	private String catalogName;
//	@TableField(value = "source_type")
//	private Integer sourceType;
//	@TableField(value = "type")
//	private Integer type;
//	@TableField(value = "type_name")
//	private String typName;
//	@TableField(value = "info_system_id")
//	private String infoSystemId;
//	@TableField(value = "database_name")
//	private String databaseName;
//	@TableField(value = "`schema`")
//	private String schema;
//	@TableField(value = "host")
//	private String host;
//	@TableField(value = "port")
//	private Integer port;
//	@TableField(value = "username")
//	private String username;
//	@TableField(value = "password")
//	private String password;
//	@TableField(value = "excel_protocol")
//	private String excelProtocol;
//	@TableField(value = "excel_base")
//	private String excelBase;
//	@TableField(value = "data_view_source")
//	private String dataViewSource;
//	@TableField(value = "status")
//	private Integer status;
//	@TableField(value = "metadata_task_id")
//	private String metadataTaskId;
//	@TableField(value = "token")
//	private String token;
}
