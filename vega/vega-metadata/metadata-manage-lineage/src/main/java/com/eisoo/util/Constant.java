package com.eisoo.util;

import com.eisoo.entity.ColumnLineageEntity;
import com.eisoo.entity.DolphinEntity;
import com.eisoo.entity.IndicatorLineageEntity;
import com.eisoo.entity.TableLineageEntity;

import java.util.HashMap;
import java.util.HashSet;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/7 17:31
 * @Version:1.0
 */
public class Constant {
    public final static HashMap<String, String> LINEAGE_DOMAIN_MAP = new HashMap<String, String>();
    public final static HashMap<String, Class> TYPE_REFERENCE_MAP = new HashMap<>();

    public final static String LINEAGE_TABLE = "lineage_table";
    public final static String LINEAGE_COLUMN = "lineage_column";
    public final static String TABLE_2_COLUMN = "table_2_column";

    public final static String COLUMN_2_COLUMN = "column_2_column";

    public final static String LINEAGE_INDICATOR = "lineage_indicator";
    public static final String BATCH = "batch";
    public static final String STREAM = "stream";

    public static String INDICATOR_2_COLUMN = "indicator_2_column";

    public static String INDICATOR_2_INDICATOR = "indicator_2_indicator";

    public final static String EDGE = "edge";

    // table的类型
    public final static String DATA_TABLE = "data_table";


    public final static HashSet<String> DOLPHIN_JSON_COMMON_FIELD = new HashSet<String>();

    static {
        DOLPHIN_JSON_COMMON_FIELD.add("modelType");

        DOLPHIN_JSON_COMMON_FIELD.add("targetCatalog");
        DOLPHIN_JSON_COMMON_FIELD.add("targetSchema");
        DOLPHIN_JSON_COMMON_FIELD.add("targetTableName");

    }

    static {
        LINEAGE_DOMAIN_MAP.put(TableLineageEntity.class.getName(), LINEAGE_TABLE);
        LINEAGE_DOMAIN_MAP.put(ColumnLineageEntity.class.getName(), LINEAGE_COLUMN);
        LINEAGE_DOMAIN_MAP.put(IndicatorLineageEntity.class.getName(), LINEAGE_INDICATOR);

        TYPE_REFERENCE_MAP.put(Constant.TABLE, TableLineageEntity.class);
        TYPE_REFERENCE_MAP.put(Constant.COLUMN, ColumnLineageEntity.class);
        TYPE_REFERENCE_MAP.put(Constant.INDICATOR, IndicatorLineageEntity.class);
        TYPE_REFERENCE_MAP.put(Constant.DOLPHIN, DolphinEntity.class);

        // 三方的数据
        TYPE_REFERENCE_MAP.put(Constant.EXTERNAL_TABLE, TableLineageEntity.class);
        TYPE_REFERENCE_MAP.put(Constant.EXTERNAL_COLUMN, ColumnLineageEntity.class);
        TYPE_REFERENCE_MAP.put(Constant.EXTERNAL_RELATION_COLUMN, ColumnLineageEntity.class);

    }

    public final static String GLOBAL_SPLIT_COMMA = ",";

    public final static String TABLE = "table";
    public final static String DOLPHIN_ETL_COLUMN = "dolphin_etl_column";


    // external_table : 来自三方的表，这里是业务统一定义的
    public final static String EXTERNAL_TABLE = "external_table";
    public final static String EXTERNAL_COLUMN = "external_column";
    public final static String EXTERNAL_RELATION_COLUMN = "external_relation_column";

    //    public final static String EXTERNAL_NO_ETL_TABLE = "external_no_etl_table";
//    public final static String EXTERNAL_ETL_TABLE = "external_etl_table";
//    public final static String EXTERNAL_NOT_ETL_TABLE = "external_not_etl_table";
    // 三方的表
//    public final static String CUSTOMER_ETL_TABLE = "customer_etl_table";
//    public final static String CUSTOMER_NOT_ETL_TABLE = "customer_not_etl_table";
//    public final static String CUSTOMER_ETL_META_COLUMN = "customer_etl_meta_column";
//    public final static String CUSTOMER_NOT_ETL_META_COLUMN = "customer_not_etl_meta_column";
//    public final static String CUSTOMER_ETL_RELATION_COLUMN = "customer_etl_relation_column";
//    public final static String CUSTOMER_NOT_ETL_RELATION_COLUMN = "customer_not_etl_relation_column";
    public final static String COLUMN = "column";
    public final static String INDICATOR = "indicator";
    public final static String DOLPHIN = "dolphin";
    public final static String FORWARD = "forward";
    public final static String REVERSELY = "reversely";
    public final static String BIDIRECT = "bidirect";
    public final static String DELETE = "delete";
    public final static String INSERT = "insert";
    public final static String UPDATE = "update";
    public final static String UPSERT = "upsert";
    public final static String AD_RESPONSE_SUCCESS = "success";
    public final static String SYNC = "sync";
    public final static String COMPOSE = "compose";
    public final static String UNDER_LINE = "_";
    public final static String AD_API_ERROR = "-1";
    public final static String QUERY_TABLE_INFO = "{\"page\":1,\"size\":0,\"query\":\"\",\"search_config\":[{\"tag\":\"lineage_table\",\"properties\":[{\"name\":\"unique_id\",\"operation\":\"eq\",\"op_value\":\"%s\"}]}],\"kg_id\":\"%s\",\"matching_rule\":\"portion\",\"matching_num\":1}";


}
