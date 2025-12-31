package com.eisoo.metadatamanage.web.provider;
import com.eisoo.metadatamanage.web.extra.inf.DataSourceProcessor;
import com.eisoo.metadatamanage.web.manager.DataSourceProcessorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.web.provider
 * @Date: 2023/3/30 15:29
 */
public class DataSourceProcessorProvider {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceProcessorProvider.class);

    private DataSourceProcessorManager dataSourcePluginManager;

    private DataSourceProcessorProvider() {
        initDataSourceProcessorPlugin();
    }

    private static class DataSourceClientProviderHolder {
        private static final DataSourceProcessorProvider INSTANCE = new DataSourceProcessorProvider();
    }

    public static DataSourceProcessorProvider getInstance() {
        return DataSourceClientProviderHolder.INSTANCE;
    }

    public Map<String, DataSourceProcessor> getDataSourceProcessorMap() {
        return dataSourcePluginManager.getDataSourceProcessorMap();
    }

    private void initDataSourceProcessorPlugin() {
        dataSourcePluginManager = new DataSourceProcessorManager();
        dataSourcePluginManager.installProcessor();
    }
}
