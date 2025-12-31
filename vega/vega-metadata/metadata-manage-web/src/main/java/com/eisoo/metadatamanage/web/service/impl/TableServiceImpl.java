package com.eisoo.metadatamanage.web.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.eisoo.metadatamanage.db.entity.*;
import com.eisoo.metadatamanage.lib.dto.*;
import com.eisoo.metadatamanage.lib.vo.*;
import com.eisoo.metadatamanage.util.constant.DataSourceConstants;
import com.eisoo.metadatamanage.web.service.*;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.standardization.common.threadpoolexecutor.MDCThreadPoolExecutor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.MDC;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.eisoo.metadatamanage.db.mapper.SchemaMapper;
import com.eisoo.metadatamanage.db.mapper.TableFieldMapper;
import com.eisoo.metadatamanage.db.mapper.TableMapper;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.util.constant.Messages;
import com.eisoo.metadatamanage.web.config.ExcelMultiThreadImportConfig;
import com.eisoo.metadatamanage.web.util.ExcelDictHandler;
import com.eisoo.metadatamanage.web.util.FieldCheckError;
import com.eisoo.metadatamanage.web.util.TableCheckError;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.excel.AiShuExcelExportStylerImpl;
import com.eisoo.standardization.common.exception.AiShuException;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.enmus.ExcelType;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TableServiceImpl extends ServiceImpl<TableMapper, TableEntity> implements ITableService {
    private static final ExecutorService executorService = Executors.newWorkStealingPool();

    @Autowired(required = false)
    ExcelMultiThreadImportConfig excelMultiThreadImportConfig;

    @Autowired(required = false)
    TableMapper tableMapper;

    @Autowired(required = false)
    TableFieldMapper tableFieldMapper;

    @Autowired(required = false)
    SchemaMapper schemaMapper;

    @Autowired(required = false)
    ITableFieldService tableFieldService;

    @Autowired(required = false)
    IDictService dictService;

    @Autowired(required = false)
    @Lazy
    IDataSourceService dataSourceService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    IVegaDataSourceService vegaDataSourceService;
    //数据量更新线程
    ExecutorService updateRowNumExecutorPool = MDCThreadPoolExecutor.newFixedThreadPool(10);

    @Override
    public boolean isSchemaUsed(Long schemaId) {
        LambdaQueryWrapper<TableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableEntity::getSchemaId, schemaId);
        return tableMapper.exists(wrapper);
    }

    @Override
    public Result<List<TableItemVo>> getList(Integer dataSourceType, String dsId, Long schemaId, List<Long> idList, String keyword, Integer offset, Integer limit, String sort, String direction, Boolean checkField) {
        LambdaQueryWrapper<TableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dsId != null, TableEntity::getDataSourceId, dsId);
        wrapper.eq(TableEntity::getDeleted, false);
        wrapper.eq(schemaId != null, TableEntity::getSchemaId, schemaId);
        wrapper.eq(dataSourceType != null, TableEntity::getDataSourceType, dataSourceType);
        wrapper.in(idList != null && !idList.isEmpty(), TableEntity::getId, idList);
//        wrapper.like(keyword != null, TableEntity::getDescription, keyword);

        keyword = StringUtils.trim(keyword);
        if (StringUtils.isNotBlank(keyword)) {
            keyword = StringUtils.substring(keyword, 0, 64);
            keyword = "%" + keyword.toLowerCase() + "%";
            wrapper.apply("(lower(f_name) like {0} or lower(f_description) like {1})", keyword, keyword);
        }
        boolean isAsc = direction.toLowerCase().equals("asc");
        if (sort.toLowerCase().equals("update_time")) {
            wrapper.orderBy(true, isAsc, TableEntity::getUpdateTime);
        } else {
            wrapper.orderBy(true, isAsc, TableEntity::getId);
        }

        Page<TableEntity> p = new Page<>(offset, limit);
        IPage<TableEntity> data = page(p, wrapper);
        List<TableItemVo> result = new ArrayList<>();
        //checkField为true且查询结果非空时，检测每个表是否至少拥有一个字段
        if (AiShuUtil.isNotEmpty(data.getRecords())) {
            List<Long> tableIds = data.getRecords().stream().map(tableItemVo -> tableItemVo.getId()).collect(Collectors.toList());
            Map<Long, TableFieldEntity> fieldMap = SimpleQuery.keyMap(Wrappers.lambdaQuery(TableFieldEntity.class).in(TableFieldEntity::getTableId, tableIds).eq(TableFieldEntity::getDeleteFlag, false), TableFieldEntity::getTableId);
            data.getRecords().forEach(item -> {
                TableItemVo tableItemVo = new TableItemVo();
                AiShuUtil.copyProperties(item, tableItemVo);
                if (checkField && AiShuUtil.isNotEmpty(fieldMap)) {
                    Boolean haveField = false;
                    if (AiShuUtil.isNotEmpty(fieldMap.get(tableItemVo.getId()))) {
                        haveField = true;
                    }
                    tableItemVo.setHaveField(haveField);
                }
                result.add(tableItemVo);
            });
        }
        return Result.success(result, data.getTotal());
    }

    @Override
    public Result<List<TableWithColumnVo>> getListWithColumn(Integer dataSourceType, String dsId, Long schemaId, List<Long> idList, String keyword, Integer offset, Integer limit, String sort, String direction, Boolean checkField) {
        LambdaQueryWrapper<TableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(dsId != null, TableEntity::getDataSourceId, dsId);
//        DataSourceEntity dataSourceEntity = dataSourceService.getById(dsId);
        DipDataSourceEntity dataSourceEntityVega = vegaDataSourceService.getByDataSourceId(dsId);
        if (AiShuUtil.isNotEmpty(dataSourceEntityVega) && AiShuUtil.isEmpty(schemaId)) {
//            Map<String, String> extendPropertyMap = JSONUtils.props2Map(dataSourceEntity.getExtendProperty());
            //查询现有高级参数
            JSONObject jsonProperty = null;
            try {
                jsonProperty = JSONObject.parse(new String(dataSourceEntityVega.getBinData()));
            } catch (Exception e) {
                log.error("获取数据源的binData失败，dataSourceEntity={}", dataSourceEntityVega, e);
                throw new AiShuException(ErrorCodeEnum.UnKnowException);
            }
            String schemaName = jsonProperty.getString("database_name").toLowerCase();
            if (jsonProperty.containsKey("schema")) {
                schemaName = jsonProperty.getString("schema").toLowerCase();
            }
//            if (AiShuUtil.isNotEmpty(extendPropertyMap)) {
//                String schemaName = (extendPropertyMap.get(DataSourceConstants.SCHEMAKEY));
//                wrapper.eq(StringUtils.isNotEmpty(schemaName), TableEntity::getSchemaName, schemaName);
//            }
            wrapper.eq(StringUtils.isNotEmpty(schemaName), TableEntity::getSchemaName, schemaName);
        }
        wrapper.eq(schemaId != null, TableEntity::getSchemaId, schemaId);
        wrapper.eq(dataSourceType != null, TableEntity::getDataSourceType, dataSourceType);
        wrapper.in(idList != null && !idList.isEmpty(), TableEntity::getId, idList);
        wrapper.eq(TableEntity::getDeleted, false);
//        wrapper.like(keyword != null, TableEntity::getDescription, keyword);

        keyword = StringUtils.trim(keyword);
        if (StringUtils.isNotBlank(keyword)) {
            keyword = StringUtils.substring(keyword, 0, 64);
            keyword = "%" + keyword.toLowerCase() + "%";
            wrapper.apply("(lower(f_name) like {0} or lower(t.f_description) like {1})", keyword, keyword);
        }
        boolean isAsc = direction.toLowerCase().equals("asc");
        if (sort.toLowerCase().equals("update_time")) {
            wrapper.orderBy(true, isAsc, TableEntity::getUpdateTime);
        } else {
            wrapper.orderBy(true, isAsc, TableEntity::getId);
        }

        Page<TableEntity> p = new Page<>(offset, limit);
        IPage<TableEntity> data = page(p, wrapper);
        List<TableWithColumnVo> result = new ArrayList<>();


        //拼接字段元数据
        if (AiShuUtil.isNotEmpty(data.getRecords())) {
            List<Long> tableIdList = data.getRecords().stream().map(table -> table.getId()).collect(Collectors.toList());
            LambdaQueryWrapper<TableFieldEntity> fieldWrapper = new LambdaQueryWrapper<>();
            fieldWrapper.in(TableFieldEntity::getTableId, tableIdList);
            fieldWrapper.eq(TableFieldEntity::getDeleteFlag, false);
            List<TableFieldEntity> fieldEntityList = tableFieldService.list(fieldWrapper);
            Map<Long, List<TableFieldEntity>> fieldMap = new HashMap<>();
            if (AiShuUtil.isNotEmpty(fieldEntityList)) {
                fieldEntityList.forEach(field -> {
                    if (AiShuUtil.isEmpty(fieldMap.get(field.getTableId()))) {
                        List<TableFieldEntity> fieldEntities = new ArrayList<>();
                        fieldEntities.add(field);
                        fieldMap.put(field.getTableId(), fieldEntities);
                    } else {
                        fieldMap.get(field.getTableId()).add(field);
                    }
                });
            }
            data.getRecords().forEach(item -> {
                TableWithColumnVo vo = new TableWithColumnVo();
                AiShuUtil.copyProperties(item, vo);
                vo.setFields(tableFieldService.getVoList(fieldMap.get(vo.getId()), vo.getDataSourceType()));
                if (AiShuUtil.isEmpty(vo.getFields())) {
                    vo.setHaveField(false);
                } else {
                    vo.setHaveField(true);
                }
                result.add(vo);
            });
        }

        return Result.success(result, data.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String dsId, Long schemaId, Long tableId) {
        LambdaUpdateWrapper<TableEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(TableEntity::getId, tableId);
        wrapper.eq(TableEntity::getDataSourceId, dsId);
        wrapper.eq(TableEntity::getSchemaId, schemaId);
        if (tableMapper.delete(wrapper) > 0) {
            LambdaUpdateWrapper<TableFieldEntity> tfw = new LambdaUpdateWrapper<>();
            tfw.eq(TableFieldEntity::getTableId, tableId);
            tableFieldMapper.delete(tfw);
        } else {
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
    }

    @Override
    public TableEntity getOne(String dsId, Long schemaId, String tableName) {
        LambdaQueryWrapper<TableEntity> tableWrapper = new LambdaQueryWrapper<>();
        tableWrapper.eq(TableEntity::getDataSourceId, dsId);
        tableWrapper.eq(TableEntity::getSchemaId, schemaId);
        tableWrapper.eq(TableEntity::getName, tableName);
        return getOne(tableWrapper);
    }

    @Override
    public Result<TableVo> getDetail(String dsId, Long schemaId, Long tableId) {
        return Result.success(getDetailProcess(dsId, schemaId, tableId));
    }

    private TableVo getDetailProcess(String dsId, Long schemaId, Long tableId) {
        LambdaQueryWrapper<TableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableEntity::getId, tableId);
        wrapper.eq(TableEntity::getDeleted, false);
        wrapper.eq(TableEntity::getDataSourceId, dsId);
        wrapper.eq(TableEntity::getSchemaId, schemaId);
        TableEntity t = tableMapper.selectOne(wrapper);
        if (t == null) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }

        TableVo table = new TableVo();
        table.setDataSourceType(t.getDataSourceType());
        table.setDataSourceTypeName(t.getDataSourceTypeName());
        table.setDataSourceId(t.getDataSourceId());
        table.setDataSourceName(t.getDataSourceName());
        table.setSchemaId(t.getSchemaId());
        table.setSchemaName(t.getSchemaName());
        table.setId(t.getId());
        table.setName(t.getName());
        table.setDescription(t.getDescription());
        table.setAdvancedParams((List<AdvancedDTO>) JSON.parse(t.getAdvancedParams()));
//        table.setVersion("V"+t.getVersion());
        table.setAuthorityId(t.getAuthorityId());
        table.setCreateTime(t.getCreateTime());
        table.setCreateUser(t.getCreateUser());
        table.setUpdateTime(t.getUpdateTime());
        table.setUpdateUser(t.getUpdateUser());

        LambdaQueryWrapper<TableFieldEntity> tfw = new LambdaQueryWrapper<>();
        tfw.eq(TableFieldEntity::getTableId, tableId);
        table.setFields(tableFieldService.getVoList(tableFieldService.list(tfw), table.getDataSourceType()));
        return table;
    }

    private SchemaEntity getParentResourceInfo(String dsId, Long schemaId) {
        LambdaQueryWrapper<SchemaEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchemaEntity::getId, schemaId);
        wrapper.eq(SchemaEntity::getDataSourceId, dsId);

        SchemaEntity se = null;
        if ((se = schemaMapper.selectOne(wrapper)) == null) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ParentResourceNotExisted);
        }
        return se;
    }

    @Override
    public void add(String dsId, Long schemaId, TableCreateDTO params) {
        SchemaEntity se = getParentResourceInfo(dsId, schemaId);

        // 获取表ID
        Long tableId = IdWorker.getId();
        // 表字段元数据检查
        TableCheckError tableCheckErr = getTableCheckError(tableId, se, params);

        // 获取代理对象，通过代理对象调拥有Transactional注解的方法，否则addProcess的事务不会生效
        TableServiceImpl proxyObj = (TableServiceImpl) AopContext.currentProxy();
        proxyObj.addProcess(tableId, se, params, tableCheckErr);
    }

    @Transactional(rollbackFor = Exception.class)
    public void addProcess(Long tableId, SchemaEntity se, TableCreateDTO params, TableCheckError tableCheckErr) {
        TableEntity table = new TableEntity();
        table.setDataSourceType(se.getDataSourceType());
        table.setDataSourceTypeName(se.getDataSourceTypeName());
        table.setDataSourceId(se.getDataSourceId());
        table.setDataSourceName(se.getDataSourceName());
        table.setSchemaId(se.getId());
        table.setSchemaName(se.getName());
        table.setId(tableId);
        table.setName(params.getName());
        table.setDescription(params.getDescription() == null ? "" : params.getDescription());
        table.setAdvancedParams(params.getAdvancedParams() == null ? "[]" : JSON.toJSONString(params.getAdvancedParams()));
        if (tableCheckErr != null) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, tableCheckErr);
        }

        try {
            tableMapper.insert(table);
        } catch (DuplicateKeyException e) {
            tableCheckErr = new TableCheckError(params.getName(), "表名称冲突", null);
            params.setErrorMsg("表名称冲突");
            throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated, tableCheckErr);
        }

//        if (params.getFields() != null && !params.getFields().isEmpty()) {
//            tableFieldService.saveBatch(
//                    params.getFields()
//                            .stream()
//                            .map(t -> {
//                                TableFieldEntity tfe = new TableFieldEntity();
//                                AiShuUtil.copyProperties(t, tfe);
//                                tfe.setId(null);
//                                return tfe;
//                            })
//                            .collect(Collectors.toList()),
//                    params.getFields().size()
//            );
//        }
    }

    private TableCheckError getTableCheckError(Long tableId, SchemaEntity se, TableCreateDTO params) {
        // 对提交fields校验
        List<FieldCheckError> fieldErrs = tableFieldService.tableFieldsCheck(se.getDataSourceType(), tableId, params.getFields());

        TableCheckError tableCheckErr = null;
        if (!fieldErrs.isEmpty()) {
            tableCheckErr = new TableCheckError(params.getName(), null, fieldErrs);
        }
        return tableCheckErr;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String dsId, Long schemaId, Long tableId, TableAlterDTO params) {
        LambdaQueryWrapper<TableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableEntity::getId, tableId);
        wrapper.eq(TableEntity::getDataSourceId, dsId);
        wrapper.eq(TableEntity::getSchemaId, schemaId);

        if (!tableMapper.exists(wrapper)) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }

        SchemaEntity se = getParentResourceInfo(params.getDataSourceId(), params.getSchemaId());
        TableCheckError tableCheckErr = getTableCheckError(tableId, se, params);

        TableEntity table = new TableEntity();
        table.setDataSourceType(se.getDataSourceType());
        table.setDataSourceTypeName(se.getDataSourceTypeName());
        table.setDataSourceId(se.getDataSourceId());
        table.setDataSourceName(se.getDataSourceName());
        table.setSchemaId(se.getId());
        table.setSchemaName(se.getName());
        table.setId(tableId);
        table.setName(params.getName());
        table.setDescription(params.getDescription() == null ? "" : params.getDescription());
        table.setAdvancedParams(params.getAdvancedParams() == null ? "[]" : JSON.toJSONString(params.getAdvancedParams()));
        table.setUpdateTime(new Date());

        if (tableCheckErr != null) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, tableCheckErr);
        }

        try {
            tableMapper.updateById(table);
        } catch (DuplicateKeyException e) {
            tableCheckErr = new TableCheckError(params.getName(), "表名称冲突", null);
            throw new AiShuException(ErrorCodeEnum.ResourceNameDuplicated, tableCheckErr);
        }

//        tableFieldService.saveOrUpdateBatch(
//                tableId,
//                params.getFields()
//                        .stream()
//                        .map(t -> {
//                            TableFieldEntity tfe = new TableFieldEntity();
//                            AiShuUtil.copyProperties(t, tfe);
//                            return tfe;
//                        })
//                        .collect(Collectors.toList())
//        );
    }

    private void checkFile(MultipartFile file) {
        List<String> checkErrors = Lists.newLinkedList();
        //校验集合-文件不能为空或xlsx、xls以外的文件
        if (!AiShuUtil.checkFileType(file, ".xlsx") && !AiShuUtil.checkFileType(file, ".xls")) {
            checkErrors.add("文件不能为空或xlsx、xls以外的文件");
        } else if (!AiShuUtil.checkFileSize(file.getSize(), Constants.FILE_UPLOAD_LIMIT_SIZE, "M")) {
            //校验集合-文件不能超过10M
            checkErrors.add("文件不能超过10M");
        }
        if (checkErrors.size() > 0) {
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, checkErrors);
        }
    }

    @Override
    public void importExcel(String dsId, Long schemaId, MultipartFile file, HttpServletResponse response) {
        // 附件格式及size检查，检查不通过抛出异常
        checkFile(file);

        // 获取上级资源信息，如不存在则抛出异常
        SchemaEntity se = getParentResourceInfo(dsId, schemaId);

        // 读取并做基础数据校验
        ImportParams params = new ImportParams();
        params.setTitleRows(1);
        params.setHeadRows(2);
        params.setNeedVerify(true);
        params.setVerifyFileSplit(false);
        // 设置导入所需依赖的key-value字典
        Map<Integer, String> dictType2NameMap = new HashMap<>();
        dictType2NameMap.put(se.getDataSourceType() + 1, "fieldType");
        ExcelDictHandler edh = new ExcelDictHandler(dictService, dictType2NameMap);
        params.setDictHandler(edh);

        ExcelImportResult<TableCreateDTO> result = null;
        try {
            result = ExcelImportUtil.importExcelMore(file.getInputStream(), TableCreateDTO.class, params);
        } catch (Exception e) {
            log.error("excel 导入失败：", e);
            throw new AiShuException(ErrorCodeEnum.InvalidParameter, "文件内容与模板不符", null, "请检查excel格式是否正确");
        }

        // 对初筛成功的导入表进行二次筛查(主要针对批量导入表重名以及表字段校验)，对其中出错的table进行分割
        divideErrorTableList(se, result);

        // 获取代理对象，通过代理对象调拥有Transactional注解的方法，否则addProcess的事务不会生效
        TableServiceImpl proxyObj = (TableServiceImpl) AopContext.currentProxy();

        // 提交至创建逻辑进行实际创建处理
        if (!result.getList().isEmpty()) {
            dispatchImportProcess(proxyObj, se, result);
        }

        // 所有均处理完成，判断是否存在处理失败的表并导出
        if (!result.getFailList().isEmpty()) {
            exportProcess(se.getDataSourceType(), "导入失败表信息明细", "表信息列表",
                    "导入失败表信息明细", TableCreateDTO.class, result.getFailList(), response);
        }
    }

    private void divideErrorTableList(SchemaEntity se, ExcelImportResult<TableCreateDTO> result) {
        List<TableCreateDTO> errorList = new ArrayList<>();
        List<TableCreateDTO> successList = new ArrayList<>();
        Map<String, Byte> tableNameMap = new HashMap<>();
        for (TableCreateDTO table : result.getList()) {
            if (!tableNameMap.containsKey(table.getName().toLowerCase())) {
                tableNameMap.put(table.getName().toLowerCase(), null);
            } else {
                table.setErrorMsg("表名称冲突" + (StringUtils.isEmpty(table.getErrorMsg()) ? "" : "," + table.getErrorMsg()));
            }

            // 表字段元数据检查
            if (Strings.isNullOrEmpty(table.getErrorMsg()) && tableFieldService.tableFieldsCheck(se.getDataSourceType(), null, table.getFields()).size() == 0) {
                successList.add(table);
                continue;
            }

            errorList.add(table);
        }

        for (TableCreateDTO table : result.getFailList()) {
            if (!tableNameMap.containsKey(table.getName().toLowerCase())) {
                tableNameMap.put(table.getName().toLowerCase(), null);
            } else {
                table.setErrorMsg("表名称冲突" + (StringUtils.isEmpty(table.getErrorMsg()) ? "" : "," + table.getErrorMsg()));
            }
            // 表字段元数据检查
            tableFieldService.tableFieldsCheck(se.getDataSourceType(), null, table.getFields());
            errorList.add(table);
        }

        tableNameMap.clear();
        result.getList().clear();
        result.getFailList().clear();
        result.getFailList().addAll(errorList);
        result.getList().addAll(successList);
    }

    private int calcRuntimeBatchImportSize(int totalImportSize) {
        if (totalImportSize == 0) {
            return 0;
        }
        int mod = totalImportSize % excelMultiThreadImportConfig.getBatchImportSize() == 0 ? 0 : 1;
        int divide = totalImportSize / excelMultiThreadImportConfig.getBatchImportSize();
        int threadNum = divide + mod > excelMultiThreadImportConfig.getMaxThreadNumPerTask() ?
                excelMultiThreadImportConfig.getMaxThreadNumPerTask() : divide + mod;
        return (int) Math.ceil((double) totalImportSize / (double) threadNum);
    }

    private void dispatchImportProcess(TableServiceImpl proxyObj, SchemaEntity se, ExcelImportResult<TableCreateDTO> result) {
        List<List<TableCreateDTO>> errLists = Collections.synchronizedList(new ArrayList<List<TableCreateDTO>>());
        List<List<TableCreateDTO>> segLists = Lists.partition(result.getList(), calcRuntimeBatchImportSize(result.getList().size()));
        Map<String, String> parentMdcMap = MDC.getCopyOfContextMap();
        for (Integer i = 0; i < segLists.size(); i++) {
            List<TableCreateDTO> segList = segLists.get(i);
            if (i < segLists.size() - 1) {
                executorService.execute(new Runnable() {
                    public void run() {
                        // 将父线程的MDC值赋给子线程实现日志traceId传递
                        for (Map.Entry<String, String> entry : parentMdcMap.entrySet()) {
                            MDC.put(entry.getKey(), entry.getValue());
                        }

                        batchImportProcess(proxyObj, se, segList, errLists);

                        // 执行结束移除从父线程获取的MDC值
                        for (Map.Entry<String, String> entry : parentMdcMap.entrySet()) {
                            MDC.remove(entry.getKey());
                        }
                    }
                });
                continue;
            }
            batchImportProcess(proxyObj, se, segLists.get(i), errLists);
        }

        while (errLists.size() < segLists.size()) {
            try {
                Thread.sleep(0, 100000);
            } catch (Exception e) {
                log.error(e.toString());
            }
        }

        for (List<TableCreateDTO> errList : errLists) {
            result.getFailList().addAll(errList);
        }
    }

    private void batchImportProcess(TableServiceImpl proxyObj, SchemaEntity se, List<TableCreateDTO> inputList, List<List<TableCreateDTO>> errLists) {
        List<TableCreateDTO> errList = new ArrayList<>();
        for (TableCreateDTO tcp : inputList) {
            try {
                // 获取表ID
                Long tableId = IdWorker.getId();
                for (TableFieldDTO tf : tcp.getFields()) {
                    tf.setTableId(tableId);
                }
                TableCheckError tableCheckErr = null;
                proxyObj.addProcess(tableId, se, tcp, tableCheckErr);
            } catch (Exception e) {
                errList.add(tcp);
                e.printStackTrace();
            }
        }
        errLists.add(errList);
    }

    private void exportProcess(Integer dataSourceType, String sheetName, String titleName, String excelName, Class<?> cls, List<?> list, HttpServletResponse response) {
        ExportParams exportParams = new ExportParams(titleName, sheetName);
        exportParams.setType(ExcelType.XSSF);
        exportParams.setStyle(AiShuExcelExportStylerImpl.class);

        // 设置导出所需依赖的key-value字典
        Map<Integer, String> dictType2NameMap = new HashMap<>();
        dictType2NameMap.put(dataSourceType + 1, "fieldType");
        ExcelDictHandler edh = new ExcelDictHandler(dictService, dictType2NameMap);
        exportParams.setDictHandler(edh);

        Workbook workbook = ExcelExportUtil.exportExcel(exportParams, cls, list);
        String fileName = String.format("%s_%s.xlsx", excelName, DateFormatUtils.format(new Date(), "yyyyMMddHHmmss"));
        try {
            response.setCharacterEncoding("UTF-8");
            response.setHeader("content-Type", "application/octet-stream");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
            workbook.write(response.getOutputStream());
            response.getOutputStream().close();
        } catch (IOException e) {
            log.error("excel导出失败：", e);
            throw new AiShuException(ErrorCodeEnum.ExcelExportError, "导出excel失败", null, Messages.MESSAGE_EXPORT_SOLUTION);
        }
    }

    @Override
    public void exportExcel(String dsId, Long schemaId, Long tableId, HttpServletResponse response) {
        List<TableVo> list = new ArrayList<>();
        list.add(getDetailProcess(dsId, schemaId, tableId));
        exportProcess(list.get(0).getDataSourceType(), list.get(0).getName(),
                "表信息列表", list.get(0).getName(), TableVo.class, list, response);
    }

    @Override
    public void exportExcelTemplate(String dsId, Long schemaId, HttpServletResponse response) {
        SchemaEntity se = getParentResourceInfo(dsId, schemaId);
        String dsTypeName = dictService.getDictValue(1, se.getDataSourceType());
        if (dsTypeName == null) {
            // 资源不存在
            throw new AiShuException(ErrorCodeEnum.ResourceNotExisted);
        }
        List<TableVo> list = new ArrayList<>();
        exportProcess(se.getDataSourceType(), dsTypeName,
                "表信息列表", dsTypeName + "元数据导入模板", TableVo.class, list, response);
    }

    @Override
    public Result<?> checkNameConflict(String dsId, Long schemaId, String tableName) {
        LambdaQueryWrapper<TableEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableEntity::getDataSourceId, dsId);
        wrapper.eq(TableEntity::getDeleted, false);
        wrapper.eq(TableEntity::getSchemaId, schemaId);
        wrapper.eq(TableEntity::getName, tableName);
        if (tableMapper.exists(wrapper)) {
            return Result.success(true);
        }
        return Result.success(false);
    }

    @Override
    public AdvancedDTO getAdvancedParamByKey(TableEntity table, String key) {
        List<AdvancedDTO> advancedParams = JSONUtils.toList(AiShuUtil.isEmpty(table) ? null : table.getAdvancedParams(), AdvancedDTO.class);
        AdvancedDTO primaryKeys = AiShuUtil.isEmpty(advancedParams) ? null : advancedParams.stream().filter(param -> key.equals(param.getKey())).findFirst().orElse(null);
        return primaryKeys;
    }

    @Override
    public String isPrimaryKey(String fieldName, TableEntity table) {
        AdvancedDTO primaryKeys = getAdvancedParamByKey(table, DataSourceConstants.PrimaryKeys);
        if (AiShuUtil.isEmpty(primaryKeys) || AiShuUtil.isEmpty(primaryKeys.getValue())) {
            return DataSourceConstants.NO;
        } else if (!primaryKeys.getValue().contains(fieldName)) {
            return DataSourceConstants.NO;
        } else {
            return DataSourceConstants.YES;
        }
    }

    @Override
    public Result<?> queryTableFields(List<TableFieldSearchDto> queryList) {

        String keyTemp = "%s|%s|%s";

        Set<Long> dsIdSet = new HashSet<>();
        Set<String> schemaSet = new HashSet<>();
        Set<String> tableSet = new HashSet<>();
        Set<String> dsIdSchemaTableSet = new HashSet<>();
        for (TableFieldSearchDto row : queryList) {
            dsIdSet.add(row.getDsId());
            schemaSet.add(row.getDbSchema());
            tableSet.add(row.getTbName());
            dsIdSchemaTableSet.add(String.format(keyTemp, row.getDsId(), row.getDbSchema(), row.getTbName()));
        }

        List<Long> dsIdList = new ArrayList<>(dsIdSet.size());
        dsIdList.addAll(dsIdSet);

        List<String> schemaList = new ArrayList<>(schemaSet.size());
        schemaList.addAll(schemaSet);

        List<String> tableList = new ArrayList<>(tableSet.size());
        tableList.addAll(tableSet);


        List<DataSourceEntity> dataSourceEntityList = dataSourceService.queryByIdList(dsIdList);
        List<Integer> dsTypeList = new ArrayList<>();
        Map<String, DataSourceEntity> dsIdEntiryMap = new HashMap<>();
        for (DataSourceEntity row : dataSourceEntityList) {
            if (!dsTypeList.contains(row.getDataSourceType())) {
                dsTypeList.add(row.getDataSourceType());
            }
            dsIdEntiryMap.put(row.getId(), row);
        }

        List<TableEntity> tableDataList = tableMapper.queryByDsIdDbSchemaTbName(dsIdList, schemaList, tableList);
        List<Long> tbIdList = new ArrayList<>();
        Map<String, TableEntity> tableEntityMap = new HashMap<>();
        for (TableEntity row : tableDataList) {
            String key = String.format(keyTemp, row.getDataSourceId(), row.getSchemaName(), row.getName());
            if (dsIdSchemaTableSet.contains(key)) {
                tableEntityMap.put(key, row);
                tbIdList.add(row.getId());
            }
        }
        Map<Long, List<TableFieldEntity>> tableIdFieldMap = new HashMap<>();
        if (!AiShuUtil.isEmpty(tbIdList)) {
            List<TableFieldEntity> fieldList = tableFieldMapper.queryByTableIdList(tbIdList);
            for (TableFieldEntity row : fieldList) {
                if (!tableIdFieldMap.containsKey(row.getTableId())) {
                    List<TableFieldEntity> tempList = new ArrayList<>();
                    tableIdFieldMap.put(row.getTableId(), tempList);
                }
                tableIdFieldMap.get(row.getTableId()).add(row);
            }
        }

        List<TableFieldSearchVo> resultData = new ArrayList<>(queryList.size());
        for (TableFieldSearchDto row : queryList) {

            // 字典表的字段类型是数据源类型+1
            DataSourceEntity dsEntity = dsIdEntiryMap.get(row.getDsId());
//            Integer dsType = dsEntity == null ? null : dsEntity.getDataSourceType() + 1;

            TableFieldSearchVo target = new TableFieldSearchVo();
            target.setDsId(row.getDsId());
            target.setDbName(row.getDbName());
            target.setDbSchema(row.getDbSchema());
            target.setTbName(row.getTbName());
            target.setDsName(dsEntity == null ? "" : dsEntity.getName());
            resultData.add(target);

            String key = String.format(keyTemp, row.getDsId(), row.getDbSchema(), row.getTbName());
            if (tableEntityMap.containsKey(key)) {
                List<TableFieldEntity> tempfieldList = tableIdFieldMap.get(tableEntityMap.get(key).getId());
                if (tempfieldList == null) {
                    continue;
                }
                Set<String> primaryKeySet = getPrimaryKeyField(tableEntityMap.get(key));
                List<TableFieldSearchVo.Field> dtoFieldList = new ArrayList<>(tempfieldList.size());
                target.setFields(dtoFieldList);
                for (TableFieldEntity tempField : tempfieldList) {
                    TableFieldSearchVo.Field f = new TableFieldSearchVo.Field();
                    f.setFieldName(tempField.getFieldName());
//                    String filedTypeName = dictService.getDictValue(dsType, tempField.getFieldType());
//                    f.setFieldType(filedTypeName == null ? "" : filedTypeName);
                    f.setFieldType(tempField.getFieldType());
                    f.setPrimaryKey(false);
                    if (primaryKeySet.contains(tempField.getFieldName())) {
                        f.setPrimaryKey(true);
                    }
                    //取出原始字段类型参数
                    AdvancedDTO originFieldTypeDto = tableFieldService.getAdvancedParamByKey(tempField, DataSourceConstants.ORIGIN_FIELD_TYPE);
                    f.setOriginFieldType(originFieldTypeDto.getValue());
                    //取出虚拟化字段类型参数
                    AdvancedDTO virtualFieldTypeDto = tableFieldService.getAdvancedParamByKey(tempField, DataSourceConstants.VIRTUAL_FIELD_TYPE);
                    f.setVirtualFieldType(virtualFieldTypeDto.getValue());

                    dtoFieldList.add(f);
                }
            }
        }


        return Result.success(resultData);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long tableId) {
        deleteTabById(tableId);
        deleteFieldById(tableId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFieldById(Long tableId) {
        String delFieldSql = "update t_table_field set f_delete_flag=1,f_delete_time=CURRENT_TIMESTAMP() where f_table_id=" + tableId;
        log.info("deleteFieldById 执行 sql:\n{}", delFieldSql);
        jdbcTemplate.execute(delFieldSql);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTabById(Long tableId) {
        String sql = "update t_table set f_delete_flag=1,f_delete_time=CURRENT_TIMESTAMP() where f_id=" + tableId;
        log.info("deleteTabById 执行 sql:\n{}", sql);
        jdbcTemplate.execute(sql);
    }

    private Set<String> getPrimaryKeyField(TableEntity tableEntity) {
        Set<String> set = new HashSet<>();
        String advancedParams = tableEntity.getAdvancedParams();
        List<Map> list = JSONUtils.json2List(advancedParams, Map.class);
        for (Map row : list) {
            if ("primaryKeys".equals(row.get("key"))) {
                String value = (String) row.get("value");
                if (!StringUtils.isBlank(value)) {
                    String[] filedArray = value.split(",");
                    for (String filed : filedArray) {
                        set.add(filed);
                    }
                }
                break;
            }
        }
        return set;
    }


}
