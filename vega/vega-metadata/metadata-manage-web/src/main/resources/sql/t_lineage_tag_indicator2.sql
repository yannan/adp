CREATE TABLE IF NOT EXISTS `t_lineage_tag_indicator2`
(
    `uuid`              char(36)     NOT NULL COMMENT '指标的uuid',
    `name`              varchar(128) NOT NULL COMMENT '指标名称',
    `description`       varchar(300)  DEFAULT NULL COMMENT '指标名称描述',
    `code`              varchar(128) NOT NULL COMMENT '指标编号',
    `indicator_type`    varchar(10)  NOT NULL COMMENT '指标类型:atomic原子derived衍生composite复合',
    `expression`        text          DEFAULT NULL COMMENT '指标表达式，如果指标是原子或复合指标时',
    `indicator_uuids`   varchar(1024) DEFAULT '' COMMENT '引用的指标uuid',
    `time_restrict`     text          DEFAULT NULL COMMENT '时间限定表达式，如果指标是衍生指标时',
    `modifier_restrict` text          DEFAULT NULL COMMENT '普通限定表达式，如果指标是衍生指标时',
    `owner_uid`         varchar(50)   DEFAULT NULL COMMENT '数据ownerID',
    `owner_name`        varchar(128)  DEFAULT NULL COMMENT '数据owner名称',
    `department_id`     char(36)      DEFAULT NULL COMMENT '所属部门id',
    `department_name`   varchar(128)  DEFAULT NULL COMMENT '所属部门名称',
    `column_unique_ids` varchar(1024) DEFAULT '' COMMENT '依赖的字段的unique_id',
    `action_type`       varchar(10)  NOT NULL COMMENT '操作类型:insert update delete',
    `created_at`        datetime(3)   DEFAULT current_timestamp(3) COMMENT '创建时间',
    `updated_at`        datetime(3)   DEFAULT current_timestamp(3) COMMENT '更新时间',
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='指标血缘表';