package com.eisoo.metadatamanage.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.SimpleQuery;
import com.eisoo.metadatamanage.lib.dto.DataSourceRowsDTO;
import com.eisoo.metadatamanage.lib.dto.IndicatorCreateDTO;
import com.eisoo.metadatamanage.lib.dto.PartitionDTO;
import com.eisoo.metadatamanage.lib.dto.SchemaRowsDTO;
import com.eisoo.metadatamanage.db.entity.IndicatorEntity;
import com.eisoo.metadatamanage.db.entity.TableEntity;
import com.eisoo.metadatamanage.db.mapper.IndicatorMapper;
import com.eisoo.metadatamanage.db.mapper.TableMapper;
import com.eisoo.metadatamanage.lib.vo.CheckErrorVo;
import com.eisoo.metadatamanage.lib.vo.CheckVo;
import com.eisoo.metadatamanage.util.constant.Constants;
import com.eisoo.metadatamanage.util.constant.ConvertUtil;
import com.eisoo.standardization.common.enums.ErrorCodeEnum;
import com.eisoo.metadatamanage.web.service.*;
import com.eisoo.standardization.common.api.Result;
import com.eisoo.standardization.common.threadpoolexecutor.MDCThreadPoolExecutor;
import com.eisoo.standardization.common.util.AiShuUtil;
import com.eisoo.standardization.common.util.StringUtil;
import com.github.yulichang.base.MPJBaseServiceImpl;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.service.impl
 * @Date: 2023/5/10 16:48
 */
@Service
@Slf4j
public class IndicatorServiceImpl extends MPJBaseServiceImpl<IndicatorMapper, IndicatorEntity> implements IIndicatorService {

    //指标计算线程
    ExecutorService  indicatorExecutorPool = MDCThreadPoolExecutor.newFixedThreadPool(1);
    @Autowired(required = false)
    IndicatorMapper indicatorMapper;
    @Autowired
    IDataSourceService iDataSourceService;
    @Autowired
    ISchemaService iSchemaService;
    @Autowired
    ITableService iTableService;
    @Autowired
    ITableFieldService iTableFieldService;
    @Autowired(required = false)
    TableMapper tableMapper;
    @Override
    public Result<?> innerTask() {
        indicatorExecutorPool.submit(() -> innerTaskExec());
        return Result.success("指标计算任务已提交，完成后将更新当日指标");
    }

    @Override
    public Boolean create(IndicatorCreateDTO dto) {
        IndicatorEntity entity = new IndicatorEntity();
        entity.setIndicatorName(dto.getName());
        entity.setIndicatorType(dto.getType());
        entity.setIndicatorValue(dto.getValue());
        entity.setCreateTime(new Date());
        return save(entity);
    }

    @Override
    public Boolean deleteByTime(Date startTime, Date endTime) {
        LambdaQueryWrapper<IndicatorEntity> lambdaQueryWrapper= new LambdaQueryWrapper<>();
        lambdaQueryWrapper.ge(startTime != null, IndicatorEntity::getCreateTime,startTime);
        lambdaQueryWrapper.le(endTime != null, IndicatorEntity::getCreateTime,endTime);
        return remove(lambdaQueryWrapper);
    }

    @Override
    public CheckVo<IndicatorEntity> checkID(Long id) {
        String errorCode = "";
        List<CheckErrorVo> checkErrors = Lists.newLinkedList();
        //校验集合-数据元ID参数不能为空
        if (id == null) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.MissingParameter.getErrorCode(), "指标ID参数不能为空"));
        }
        //校验集合-目录ID指向的数据元是否存在
        IndicatorEntity indicatorEntity = getById(id);
        if (indicatorEntity == null) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.InvalidParameter.getErrorCode(), String.format("指标不存在或已删除", id)));
        }
        if (checkErrors.size() > 0) {
            errorCode =ErrorCodeEnum.InvalidParameter.getErrorCode();
        }
        return new CheckVo<>(errorCode, checkErrors, indicatorEntity);
    }

    @Override
    public Result<List<IndicatorEntity>> getList(Date start_time, Date end_time, String keyword, String type, Integer offset, Integer limit, String direction) {
        LambdaQueryWrapper<IndicatorEntity> lambdaQueryWrapper= new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(keyword != null, IndicatorEntity::getIndicatorName, keyword);
        lambdaQueryWrapper.like(type != null, IndicatorEntity::getIndicatorType, type);
        lambdaQueryWrapper.ge(start_time != null, IndicatorEntity::getCreateTime,start_time);
        lambdaQueryWrapper.le(end_time != null, IndicatorEntity::getCreateTime,end_time);
        boolean isAsc = direction.toLowerCase().equals("asc");
        lambdaQueryWrapper.orderBy(true, isAsc, IndicatorEntity::getCreateTime);
        Page<IndicatorEntity> p = new Page<>(offset, limit);
        IPage<IndicatorEntity> page = page(p, lambdaQueryWrapper);
        return Result.success(page.getRecords(), page.getTotal());
    }

    @Override
    public CheckVo<String> checkID(String ids) {
        String errorCode = "";
        List<CheckErrorVo> checkErrors = Lists.newLinkedList();
        //校验集合-ID集合参数不能为空
        if (StringUtils.isBlank(ids)) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.MissingParameter.getErrorCode(), "指标ID集合ids参数不能为空"));
        }
        //校验集合-ID集合形式为 1,2,3 等等,长度在1-2000
        if (!ids.matches(com.eisoo.standardization.common.constant.Constants.getRegexNumVarL(1, 2000))) {
            checkErrors.add(new CheckErrorVo(ErrorCodeEnum.InvalidParameter.getErrorCode(), "指标ID集合ids形式应为 {1,2,3},长度在1-2000"));
        }
        //校验集合-单个id
        List<Long> idList = StringUtil.splitNumByRegex(ids, ",");
        if (AiShuUtil.isNotEmpty(idList)) {
            idList.forEach(id -> {
                String errRode = checkID(id).getCheckCode();
                if (!StringUtils.isEmpty(errRode)) {
                    checkErrors.add(new CheckErrorVo(ErrorCodeEnum.InvalidParameter.getErrorCode(), String.format("指标不存在或已删除", id)));
                }
            });
        }
        if (checkErrors.size() > 0) {
            errorCode = ErrorCodeEnum.InvalidParameter.getErrorCode();
        }
        return new CheckVo<>(errorCode, checkErrors, ids);
    }

    private void innerTaskExec(){
        //获取当前指标表分区情况
        List<PartitionDTO> partitionDTOList = indicatorMapper.getPartitions("t_indicator");
        System.out.println(partitionDTOList.get(1).getPartitionName());
        //如果分区数大于1000，删除历史分区
        if(AiShuUtil.isNotEmpty(partitionDTOList) && partitionDTOList.size() >= 1000){
            indicatorMapper.dropPartition(partitionDTOList.get(1).getPartitionName());
        }
        //判断当前所有分区是否小于当前时间
        Date  currentDate = new Date();
        AtomicReference<Boolean> isAlterPartition = new AtomicReference<>(true);
        partitionDTOList.forEach(item ->{
            System.out.println(item);
            if(!item.getPartitionName().equals("p_max")){
                String partitionDateStr = item.getPartitionDescription().replaceAll("'","");
                Date partitionDate = ConvertUtil.toDate(partitionDateStr);
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(partitionDate);
                calendar.add(calendar.DATE,1); //把日期往后增加一天,整数  往后推,负数往前移动
                partitionDate = calendar.getTime(); //这个时间就是日期往后推一天的结果
                Integer compareResult =  partitionDate.compareTo(currentDate);
                if(compareResult > 0){
                    isAlterPartition.set(false);
                }
            }
        });
        //如果所有分区小于当前时间，创建当日分区
        if(isAlterPartition.get()){
            newPartitiion(currentDate);
        }
        //查询当日已有指标
        LambdaQueryWrapper<IndicatorEntity> wrapper = new LambdaQueryWrapper<>();
        String todayStr = new SimpleDateFormat("yyyy-MM-dd").format(currentDate);
        Date today = ConvertUtil.toDate(todayStr);
        Calendar c = Calendar.getInstance();
        c.setTime(today);
        c.add(Calendar.DAY_OF_MONTH, 1);
        //获取次日日期
        Date tomorrow = c.getTime();
        wrapper.ge(IndicatorEntity::getCreateTime, today);
        wrapper.lt(IndicatorEntity::getCreateTime, tomorrow);
        Map<String, IndicatorEntity> indicatorMap = SimpleQuery.keyMap(wrapper, IndicatorEntity::getIndicatorName);

        List<IndicatorEntity> indicatorEntityList = new ArrayList<>();
        //计算当日全局统计指标
        indicatorEntityList.add(getAllDatabaseCount(indicatorMap));
        indicatorEntityList.add(getAllSchemaCount(indicatorMap));
        indicatorEntityList.add(getAllTableCount(indicatorMap));
        indicatorEntityList.add(getAllColumnCount(indicatorMap));
        indicatorEntityList.add(getAllRowsCount(indicatorMap));
        //计算当日明细统计指标
        //各表行数明细统计
        getDetailTableRows(indicatorMap, indicatorEntityList);
        getDetailSchemaRows(indicatorMap, indicatorEntityList);
        getDetailDataBaseRows(indicatorMap, indicatorEntityList);
        //创建或更新当日内置指标
        saveOrUpdateBatch(indicatorEntityList);
    }

    //计算全部database数量
    private IndicatorEntity getAllDatabaseCount(Map<String, IndicatorEntity> indicatorMap){
        String indicatorName = "收录database总量";
        IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
        indicatorEntity.setIndicatorType(Constants.IndicatorType_TotalCount);
        indicatorEntity.setIndicatorValue(iDataSourceService.count());
        indicatorEntity.setCreateTime(new Date());
        return indicatorEntity;
    }
    //计算全部schema数量
    private IndicatorEntity getAllSchemaCount(Map<String, IndicatorEntity> indicatorMap){
        String indicatorName = "收录schema总量";
        IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
        indicatorEntity.setIndicatorType(Constants.IndicatorType_TotalCount);
        indicatorEntity.setIndicatorValue(iSchemaService.count());
        indicatorEntity.setCreateTime(new Date());
        return indicatorEntity;
    }
    //计算全部table数量
    private IndicatorEntity getAllTableCount(Map<String, IndicatorEntity> indicatorMap){
        String indicatorName = "收录table总量";
        IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
        indicatorEntity.setIndicatorType(Constants.IndicatorType_TotalCount);
        indicatorEntity.setIndicatorValue(iTableService.count());
        indicatorEntity.setCreateTime(new Date());
        return indicatorEntity;
    }
    //计算全部Column数量
    private IndicatorEntity getAllColumnCount(Map<String, IndicatorEntity> indicatorMap){
        String indicatorName = "收录column总量";
        IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
        indicatorEntity.setIndicatorType(Constants.IndicatorType_TotalCount);
        indicatorEntity.setIndicatorValue(iTableFieldService.count());
        indicatorEntity.setCreateTime(new Date());
        return indicatorEntity;
    }
    //计算全部数据行数
    private IndicatorEntity getAllRowsCount(Map<String, IndicatorEntity> indicatorMap){
        String indicatorName = "收录数据总行数";
        IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
        indicatorEntity.setIndicatorType(Constants.IndicatorType_TotalCount);
        indicatorEntity.setIndicatorValue(tableMapper.getTotalRows());
        indicatorEntity.setCreateTime(new Date());
        return indicatorEntity;
    }
    //表行数明细统计
    private void getDetailTableRows(Map<String, IndicatorEntity> indicatorMap, List<IndicatorEntity> indicatorEntityList){
        List<TableEntity> tableEntityList = iTableService.list();
        tableEntityList.forEach(table ->{
            String indicatorName = Constants.IndicatorName_TableRows+"-"+table.getName();
            IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
            indicatorEntity.setIndicatorType(Constants.IndicatorType_TableRows);
            indicatorEntity.setIndicatorValue(table.getTableRows());
            indicatorEntity.setCreateTime(new Date());
            indicatorEntity.setIndicatorObjectId(table.getId());
            indicatorEntityList.add(indicatorEntity);
        });
    }
    //schema行数明细统计
    private void getDetailSchemaRows(Map<String, IndicatorEntity> indicatorMap, List<IndicatorEntity> indicatorEntityList){
        List<SchemaRowsDTO> schemaRows = tableMapper.getSchemaRows();
        schemaRows.forEach(item ->{
            String indicatorName = Constants.IndicatorName_SchemaRows+"-"+item.getSchemaName();
            IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
            indicatorEntity.setIndicatorType(Constants.IndicatorType_SchemaRows);
            indicatorEntity.setIndicatorValue(item.getSchemaRows());
            indicatorEntity.setCreateTime(new Date());
            indicatorEntity.setIndicatorObjectId(item.getSchemaId());
            indicatorEntityList.add(indicatorEntity);
        });
    }
    //database行数明细统计
    private void getDetailDataBaseRows(Map<String, IndicatorEntity> indicatorMap, List<IndicatorEntity> indicatorEntityList){
        List<DataSourceRowsDTO> schemaRows = tableMapper.getDataSourceRows();
        schemaRows.forEach(item ->{
            String indicatorName = Constants.IndicatorName_DatasourceRows+"-"+item.getDataSourceName();
            IndicatorEntity indicatorEntity = getByName(indicatorMap, indicatorName);
            indicatorEntity.setIndicatorType(Constants.IndicatorType_DatasourceRows);
            indicatorEntity.setIndicatorValue(item.getDataSourceRows());
            indicatorEntity.setCreateTime(new Date());
            indicatorEntity.setIndicatorObjectId(item.getDataSourceId());
            indicatorEntityList.add(indicatorEntity);
        });
    }
    //获取指标集合中指定名称的指标实体，无则新建
    IndicatorEntity getByName(Map<String, IndicatorEntity> indicatorMap, String indicatorName){
        IndicatorEntity indicatorEntity = indicatorMap.get(indicatorName);
        if(AiShuUtil.isEmpty(indicatorEntity)){
            indicatorEntity = new IndicatorEntity();
            indicatorEntity.setIndicatorName(indicatorName);
        }
        return indicatorEntity;
    }
    //动态创建分区
    private void newPartitiion(Date  currentDate){
        String currentDateStr="\'"+new SimpleDateFormat("yyyy-MM-dd").format(currentDate)+"\'";
        String currentPartitionName="p"+ new SimpleDateFormat("yyyyMMdd").format(currentDate);
        indicatorMapper.dropPartition("p_max");
        indicatorMapper.addCurrentPartition(currentPartitionName, currentDateStr);
        indicatorMapper.addMaxPartition();
    }
}
