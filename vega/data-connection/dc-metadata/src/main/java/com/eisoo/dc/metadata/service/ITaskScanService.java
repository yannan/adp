package com.eisoo.dc.metadata.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eisoo.dc.common.metadata.entity.TaskScanEntity;
import com.eisoo.dc.metadata.domain.vo.QueryStatementVO;
import com.eisoo.dc.metadata.domain.vo.TableRetryVO;
import com.eisoo.dc.metadata.domain.vo.TableStatusVO;
import com.eisoo.dc.metadata.domain.vo.TaskScanVO;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Tian.lan
 */
public interface ITaskScanService extends IService<TaskScanEntity> {
    ResponseEntity<?> createScanTaskAndStart(HttpServletRequest request, TaskScanVO taskScanVO);

    ResponseEntity<?> getScanTaskInfo(HttpServletRequest request, String taskId);

    ResponseEntity<?> getScanTaskStatus(HttpServletRequest request, TableStatusVO req);

    ResponseEntity<?> retryScanTable(HttpServletRequest request, TableRetryVO req);
    ResponseEntity<?> getScanTaskTableStatus(String userId, String taskId,String status, String keyword, int limit, int offset, String sort, String direction);
    ResponseEntity<?> getScanTaskList(String userId, String dsId, String status, String keyword, int limit, int offset, String sort, String direction);

    ResponseEntity<?> queryDslStatement(HttpServletRequest request, QueryStatementVO req);

    ResponseEntity<?> createScanTaskAndStartBatch(HttpServletRequest request, List<TaskScanVO> req);

    void submitDsScanTask(String taskId, String userId) throws Exception;

    void submitTablesScanTask(String taskId, String userId);


    void updateByIdNewRequires(TaskScanEntity taskScanEntity);

}
