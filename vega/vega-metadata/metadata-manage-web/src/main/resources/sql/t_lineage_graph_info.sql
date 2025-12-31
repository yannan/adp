CREATE TABLE `t_lineage_graph_info`
(
    `app_id`   char(20) NOT NULL COMMENT '图谱appId',
    `graph_id` int(20) DEFAULT NULL COMMENT '图谱graphId',
    PRIMARY KEY (`app_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='血缘图谱信息表';