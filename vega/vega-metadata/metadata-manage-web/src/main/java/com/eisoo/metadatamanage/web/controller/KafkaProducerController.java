package com.eisoo.metadatamanage.web.controller;

import com.eisoo.metadatamanage.web.commons.KafkaUtil;
import com.eisoo.metadatamanage.web.config.AnyDataGraphConfig;
import com.eisoo.metadatamanage.web.service.KafkaProduceWithTransaction;
import com.eisoo.metadatamanage.web.service.impl.lineage.customer.CustomerLineageService;
import com.eisoo.standardization.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/6 10:28
 * @Version:1.0
 */
@RestController
@RequestMapping("/v1/kafka")
@Slf4j
public class KafkaProducerController {
    @Autowired
    private KafkaProduceWithTransaction kafkaProduceWithTransaction;
    @Autowired
    private AnyDataGraphConfig anyDataGraphConfig;
    @Autowired
    private CustomerLineageService customerLineageService;

    @ResponseBody
    @PostMapping(value = "/produce", produces = "application/json")
    public Result<?> produce(@RequestBody String message) {
        KafkaUtil.checkKafkaMessage(message);
        return kafkaProduceWithTransaction.sendMessage(anyDataGraphConfig.getKafkaTopic(), message);
    }

    /***
     * 同步三方table血缘信息
     * @param message：json格式血缘信息
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/data-lineage/table-info", produces = "application/json")
    public Result<?> customerMetaTableLineage(@RequestBody String message) {
        customerLineageService.dealCustomerMetaTable(message);
        return Result.success();
    }

    /***
     * 同步三方task类型的table血缘信息
     * @param message：json格式血缘信息
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/data-lineage/task-table-info", produces = "application/json")
    public Result<?> customerTaskTableLineage(@RequestBody String message) {
        customerLineageService.dealCustomerMetaTable(message);
        return Result.success();
    }

    /***
     * 同步三方column血缘信息
     * @param message：json格式血缘信息
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/data-lineage/column-detail", produces = "application/json")
    public Result<?> customerMetaColumnLineage(@RequestBody String message) {
        customerLineageService.dealCustomerColumn(message, false);
        return Result.success();
    }

    /***
     * 同步三方task类型的column血缘信息
     * @param message：json格式血缘信息
     * @return
     */
    @ResponseBody
    @PostMapping(value = "/data-lineage/task-column-detail", produces = "application/json")
    public Result<?> customerTaskColumnLineage(@RequestBody String message) {
        customerLineageService.dealCustomerColumn(message, true);
        return Result.success();
    }
}
