CREATE TABLE  IF NOT EXISTS `t_lineage_tag_column2`
(
    `unique_id`         varchar(255) NOT NULL COMMENT '列的唯一id,主键',
    `uuid`              char(36)      DEFAULT NULL COMMENT '字段的uuid',
    `technical_name`    varchar(255)  DEFAULT NULL COMMENT '列技术名称',
    `business_name`     varchar(255)  DEFAULT NULL COMMENT '列业务名称',
    `comment`           varchar(300)  DEFAULT NULL COMMENT '字段注释',
    `data_type`         varchar(255)  DEFAULT NULL COMMENT '字段的数据类型',
    `primary_key`       tinyint(1)    DEFAULT NULL COMMENT '是否主键',
    `table_unique_id`   char(36)      DEFAULT NULL COMMENT '属于血缘表的unique_id',
    `expression_name`   text          DEFAULT NULL COMMENT 'column的生成表达式',
    `column_unique_ids` varchar(1024) DEFAULT '' COMMENT 'column的生成依赖的column的unique_id',
    `action_type`       varchar(10)   DEFAULT NULL COMMENT '操作类型:insert update delete',
    `created_at`        datetime(3)   DEFAULT current_timestamp(3) COMMENT '创建时间',
    `updated_at`        datetime(3)   DEFAULT current_timestamp(3) COMMENT '更新时间',
    PRIMARY KEY (`unique_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='血缘字段表';