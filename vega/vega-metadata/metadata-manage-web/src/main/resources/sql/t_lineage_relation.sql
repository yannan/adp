CREATE TABLE IF NOT EXISTS `t_lineage_relation`
(
    `unique_id`  varchar(255) NOT NULL COMMENT '实体ID',
    `class_type` tinyint(1)  DEFAULT NULL COMMENT '类型，1:column,2:indicator',
    `parent`     text        DEFAULT '' COMMENT '上一个节点',
    `child`      text        DEFAULT '' COMMENT '下一个节点',
    `created_at` datetime(3) DEFAULT current_timestamp(3) COMMENT '创建时间',
    `updated_at` datetime(3) DEFAULT current_timestamp(3) COMMENT '更新时间',
    PRIMARY KEY (`unique_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='血缘关系表';