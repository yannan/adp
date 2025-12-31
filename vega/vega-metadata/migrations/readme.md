ALTER TABLE adp.t_schema MODIFY COLUMN f_data_source_id char(36) NOT NULL COMMENT '数据源唯一标识';
ALTER TABLE adp.t_data_source MODIFY COLUMN f_id char(36) NOT NULL COMMENT '唯一id，雪花算法';
ALTER TABLE adp.t_table MODIFY COLUMN f_data_source_id char(36) NOT NULL COMMENT '数据源唯一标识';
ALTER TABLE adp.t_task MODIFY COLUMN f_object_id char(36) DEFAULT NULL NULL COMMENT '任务对象id';
ALTER TABLE adp.t_table ADD f_scan_source TINYINT NULL COMMENT '扫描来源';
