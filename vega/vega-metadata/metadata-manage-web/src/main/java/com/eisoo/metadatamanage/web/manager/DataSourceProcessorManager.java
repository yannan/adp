package com.eisoo.metadatamanage.web.manager;
import static java.lang.String.format;


import java.util.Collections;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.manager
 * @Date: 2023/3/30 15:32
 */
public class DataSourceProcessorManager {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceProcessorManager.class);

    private static final Map<String, DataSourceProcessor> dataSourceProcessorMap = new ConcurrentHashMap<>();

    public Map<String, DataSourceProcessor> getDataSourceProcessorMap() {
        return Collections.unmodifiableMap(dataSourceProcessorMap);
    }

    public void installProcessor() {

//        ServiceLoader.load(DataSourceProcessor.class).forEach(factory -> {
//            final String name = factory.getDbType().name();
//
//            logger.info("start register processor: {}", name);
//            if (dataSourceProcessorMap.containsKey(name)) {
//                throw new IllegalStateException(format("Duplicate datasource plugins named '%s'", name));
//            }
//            loadDatasourceClient(factory);
//
//            logger.info("done register processor: {}", name);
//
//        });

        ServiceLoader.load(DataSourceProcessor.class).forEach(factory -> {
            final String name = factory.getDbType().name();

            logger.info("start register processor: {}", name);
            if (dataSourceProcessorMap.containsKey(name)) {
                throw new IllegalStateException(format("Duplicate datasource plugins named '%s'", name));
            }
            loadDatasourceClient(factory);

            logger.info("done register processor: {}", name);

        });
    }

    private void loadDatasourceClient(DataSourceProcessor processor) {
        DataSourceProcessor instance = processor.create();
        dataSourceProcessorMap.put(processor.getDbType().name(), instance);
    }
}
