package com.eisoo.metadatamanage.web.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eisoo.metadatamanage.db.entity.TaskLogEntity;
import com.eisoo.metadatamanage.db.mapper.TaskLogMapper;
import com.eisoo.metadatamanage.web.service.ITaskLogService;
import org.springframework.stereotype.Service;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.service.impl
 * @Date: 2023/7/7 14:58
 */
@Service
public class TaskLogServiceImpl extends ServiceImpl<TaskLogMapper, TaskLogEntity> implements ITaskLogService {

}
