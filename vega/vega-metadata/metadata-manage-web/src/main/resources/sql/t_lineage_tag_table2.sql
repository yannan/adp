CREATE TABLE IF NOT EXISTS `t_lineage_tag_table2`
(
    `unique_id`        varchar(255) NOT NULL COMMENT '唯一id,主键',
    `uuid`             char(36)              DEFAULT NULL COMMENT '表的uuid',
    `technical_name`   varchar(255)          DEFAULT NULL COMMENT '表技术名称',
    `business_name`    varchar(255)          DEFAULT NULL COMMENT '表业务名称',
    `comment`          varchar(300)          DEFAULT NULL COMMENT '表注释',
    `table_type`       varchar(36)           DEFAULT NULL COMMENT '表类型',
    `datasource_id`    char(36)              DEFAULT NULL COMMENT '数据源id',
    `datasource_name`  varchar(255)          DEFAULT NULL COMMENT '数据源名称',
    `owner_id`         char(36)              DEFAULT NULL COMMENT '数据Ownerid',
    `owner_name`       varchar(128)          DEFAULT NULL COMMENT '数据OwnerName',
    `department_id`    char(36)              DEFAULT NULL COMMENT '所属部门id',
    `department_name`  varchar(128)          DEFAULT NULL COMMENT '所属部门mame',
    `info_system_id`   char(36)              DEFAULT NULL COMMENT '信息系统id',
    `info_system_name` varchar(128)          DEFAULT NULL COMMENT '信息系统名称',
    `database_name`    varchar(128)          DEFAULT NULL COMMENT '数据库名称',
    `catalog_name`     varchar(255)          DEFAULT '' COMMENT '数据源catalog名称',
    `catalog_addr`     varchar(1024)         DEFAULT '' COMMENT '数据源地址',
    `catalog_type`     varchar(128)          DEFAULT NULL COMMENT '数据库类型名称',
    `task_execution_info` varchar(128) DEFAULT NULL COMMENT '表加工任务的相关名称',
    `action_type`      varchar(10)  NOT NULL COMMENT '操作类型:insert update delete',
    `created_at`       datetime(3)  NOT NULL DEFAULT current_timestamp(3) COMMENT '创建时间',
    `updated_at`       datetime(3)  NOT NULL DEFAULT current_timestamp(3) COMMENT '更新时间',
    PRIMARY KEY (`unique_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='血缘表信息';