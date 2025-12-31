package com.eisoo.metadatamanage.util.constant;

public class Constants extends com.eisoo.standardization.common.constant.Constants {
    public static final String REGEX_BASE64 = "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{4})$";

    /**
     * AT SIGN
     */
    public static final String AT_SIGN = "@";
    /**
     * DOUBLE_SLASH //
     */
    public static final String DOUBLE_SLASH = "//";

    /**
     * comma ,
     */
    public static final String COMMA = ",";

    /**
     * COLON :
     */
    public static final String COLON = ":";

    /**
     * common properties path
     */
    public static final String COMMON_PROPERTIES_PATH = "/common.properties";

    /**
     * resource storage type
     */
    public static final String RESOURCE_STORAGE_TYPE = "resource.storage.type";

    /**
     * hadoop.security.authentication
     */
    public static final String HADOOP_SECURITY_AUTHENTICATION_STARTUP_STATE =
            "hadoop.security.authentication.startup.state";

    /**
     * java.security.krb5.conf.path
     */
    public static final String JAVA_SECURITY_KRB5_CONF_PATH = "java.security.krb5.conf.path";

    /**
     * loginUserFromKeytab user
     */
    public static final String LOGIN_USER_KEY_TAB_USERNAME = "login.user.keytab.username";

    /**
     * loginUserFromKeytab path
     */
    public static final String LOGIN_USER_KEY_TAB_PATH = "login.user.keytab.path";
    /**
     * java.security.krb5.conf
     */
    public static final String JAVA_SECURITY_KRB5_CONF = "java.security.krb5.conf";
    /**
     * hadoop.security.authentication
     */
    public static final String HADOOP_SECURITY_AUTHENTICATION = "hadoop.security.authentication";

    /**
     * kerberos
     */
    public static final String KERBEROS = "kerberos";
    /**
     * data.quality.jar.name
     */
    public static final String DATA_QUALITY_JAR_NAME = "data-quality.jar.name";

    /**
     * hdfs/s3 configuration
     * resource.storage.upload.base.path
     */
    public static final String RESOURCE_UPLOAD_PATH = "resource.storage.upload.base.path";

    /**
     * 总体统计类型指标
     */
    public static final String IndicatorType_TotalCount = "TotalCountType";
    /**
     * 表行数明显统计类型指标
     */
    public static final String IndicatorType_TableRows = "TableRowsType";
    /**
     * schema行数明显统计类型指标
     */
    public static final String IndicatorType_SchemaRows = "SchemaRowsType";
    /**
     * datasource行数明显统计类型指标
     */
    public static final String IndicatorType_DatasourceRows = "DatasourceRowsType";
    /**
     * 明细统计表行数名称统一前缀
     */
    public static final String IndicatorName_TableRows = "表行数";
    /**
     * 明细统计schema行数名称统一前缀
     */
    public static final String IndicatorName_SchemaRows = "schema行数";
    /**
     * 明细统计datasource行数名称统一前缀
     */
    public static final String IndicatorName_DatasourceRows = "datasource行数";

    /**
     * AF配置中心采用的加密算法
     */
    public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

    public static final String PARAMETER_OFFSET = "offset";
    public static final String PARAMETER_LIMIT = "limit";
    public static final String PARAMETER_SORT = "sort";
    public static final String PARAMETER_SORT_CREATE_TIME = "create_time";
    public static final String PARAMETER_SORT_UPDATE_TIME = "update_time";
    public static final String PARAMETER_DIRECTION = "direction";
    public static final String PARAMETER_DIRECTION_ASC = "asc";
    public static final String PARAMETER_DIRECTION_DESC = "desc";

    public static final String KEY_MODEL_TYPE = "modelType";
    public static final String KEY_HTTP_METHOD = "httpMethod";
    public static final String KEY_URL = "url";
    public static final String KEY_DELAY_TIME = "delayTime";
    public static final String KEY_FAIL_RETRY_INTERVAL = "failRetryInterval";
    public static final String KEY_FAIL_RETRY_TIMES = "failRetryTimes";
    public static final String KEY_CONNECT_TIMEOUT = "connectTimeout";
    public static final String KEY_SOCKET_TIMEOUT = "socketTimeout";
    public static final String KEY_HTTP_CHECK_CONDITION = "httpCheckCondition";
    public static final String KEY_CONDITION = "condition";
    public static final String KEY_PROJECT_CODE = "projectCode";
    public static final String KEY_HTTP_PARAMS = "httpParams";
    public static final String MQ_LIVEDDL_PRODUCER_TOPIC = "af.metadata.actual-time.ddl.result";
    public final static String DDL_LOG_TABLE_POSTGRESQL = "aishu_log_ddl";
    public final static String DDL_LOG_TRIGGER_POSTGRESQL = "aishu_trg_ddl_trigger";
    public final static String DDL_LOG_FUNCTION_POSTGRESQL = "aishu_f_trg_ddl()";
    public final static String DDL_LOG_IDX_POSTGRESQL = "idx_log_ddl_created_time";
    public final static String COMMAND_START = "start";
    public final static String COMMAND_STOP = "stop";
    public final static String COMMAND_EXECUTE_SUCCESS = "执行成功";
    public final static String COMMAND_EXECUTE_FAIL = "执行失败";
    public final static String COMMAND_EXECUTE_WAITING = "等待执行";
    public final static String COMMAND_STOP_LIVE_UPDATE_TASK = "中止实时采集任务";
}
