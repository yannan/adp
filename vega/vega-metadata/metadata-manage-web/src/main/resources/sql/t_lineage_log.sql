CREATE TABLE IF NOT EXISTS `t_lineage_log`
(
    `id`          char(36)    NOT NULL DEFAULT uuid(),
    `class_id`    char(36)    NOT NULL COMMENT '实体的主键id',
    `class_type`  char(36)    NOT NULL COMMENT '实体类型',
    `action_type` char(10)    NOT NULL COMMENT '操作类型：insert update delete',
    `class_data`  text        NOT NULL COMMENT '血缘实体json',
    `created_at`  datetime(3) NOT NULL DEFAULT current_timestamp(3) COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `t_lineage_log_class_id_IDX` (`class_id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='血缘日志记录表';