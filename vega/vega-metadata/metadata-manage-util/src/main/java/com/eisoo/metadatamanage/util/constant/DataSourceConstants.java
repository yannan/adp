package com.eisoo.metadatamanage.util.constant;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.util.constant
 * @Date: 2023/3/31 15:17
 */
public class DataSourceConstants {
    public static final String DATASOURCE = "datasource";

    /**
     * driver
     */
    public static final String ORG_POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String COM_MYSQL_CJ_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String COM_MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    public static final String ORG_APACHE_HIVE_JDBC_HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
    public static final String COM_CLICKHOUSE_JDBC_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";
    public static final String COM_ORACLE_JDBC_DRIVER = "oracle.jdbc.OracleDriver";
    public static final String COM_SQLSERVER_JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String COM_DB2_JDBC_DRIVER = "com.ibm.db2.jcc.DB2Driver";
    public static final String COM_PRESTO_JDBC_DRIVER = "com.facebook.presto.jdbc.PrestoDriver";
    public static final String COM_REDSHIFT_JDBC_DRIVER = "com.amazon.redshift.jdbc42.Driver";
    public static final String COM_ATHENA_JDBC_DRIVER = "com.simba.athena.jdbc.Driver";

    /**
     * validation Query
     */
    public static final String POSTGRESQL_VALIDATION_QUERY = "select version()";
    public static final String MYSQL_VALIDATION_QUERY = "select 1";
    public static final String HIVE_VALIDATION_QUERY = "select 1";
    public static final String CLICKHOUSE_VALIDATION_QUERY = "select 1";
    public static final String ORACLE_VALIDATION_QUERY = "select 1 from dual";
    public static final String SQLSERVER_VALIDATION_QUERY = "select 1";
    public static final String DB2_VALIDATION_QUERY = "select 1 from sysibm.sysdummy1";
    public static final String PRESTO_VALIDATION_QUERY = "select 1";
    public static final String REDHIFT_VALIDATION_QUERY = "select 1";
    public static final String ATHENA_VALIDATION_QUERY = "select 1";

    /**
     * jdbc url
     */
    public static final String JDBC_MYSQL = "jdbc:mysql://";
    public static final String JDBC_POSTGRESQL = "jdbc:postgresql://";
    public static final String JDBC_HIVE_2 = "jdbc:hive2://";
    public static final String JDBC_CLICKHOUSE = "jdbc:clickhouse://";
    public static final String JDBC_ORACLE_SID = "jdbc:oracle:thin:@";
    public static final String JDBC_ORACLE_SERVICE_NAME = "jdbc:oracle:thin:@//";
    public static final String JDBC_SQLSERVER = "jdbc:sqlserver://";
    public static final String JDBC_DB2 = "jdbc:db2://";
    public static final String JDBC_PRESTO = "jdbc:presto://";
    public static final String JDBC_REDSHIFT = "jdbc:redshift://";
    public static final String JDBC_ATHENA = "jdbc:awsathena://";

    /**
     * database type
     */
    public static final String MYSQL = "MYSQL";
    public static final String HIVE = "HIVE";

    /**
     * dataSource sensitive param
     */
    public static final String DATASOURCE_PASSWORD_REGEX =
            "(?<=((?i)password((\":\")|(=')))).*?(?=((\")|(')))";

    /**
     * datasource encryption salt
     */
    public static final String DATASOURCE_ENCRYPTION_SALT_DEFAULT = "!@#$%^&*";
    public static final String DATASOURCE_ENCRYPTION_ENABLE = "datasource.encryption.enable";
    public static final String DATASOURCE_ENCRYPTION_SALT = "datasource.encryption.salt";

    /**
     * datasource config
     */
    public static final String SPRING_DATASOURCE_MIN_IDLE = "spring.datasource.minIdle";

    public static final String SPRING_DATASOURCE_MAX_ACTIVE = "spring.datasource.maxActive";

    public static final String SPRING_DATASOURCE_TEST_ON_BORROW = "spring.datasource.testOnBorrow";


    public static final String TABLE_NAME = "TABLE_NAME";

    public static final String TABLE = "TABLE";
    public static final String VIEW = "VIEW";
    public static final String[] TABLE_TYPES = new String[]{TABLE, VIEW};

    public static final String TABLE_SCHEM = "TABLE_SCHEM";
    public static final String REMARKS = "REMARKS";
    public static final String COLUMN_NAME = "COLUMN_NAME";
    public static final String COLUMN_SIZE = "COLUMN_SIZE";
    public static final String DECIMAL_DIGITS = "DECIMAL_DIGITS";
    public static final String DATA_TYPE = "DATA_TYPE";
    public static final String TYPE_NAME = "TYPE_NAME";
    public static final String JDBC_DATA_TYPE = "jdbcType";
    public static final String PrimaryKeys = "primaryKeys";
    public static final String CHECKPRIMARYKEY = "checkPrimaryKey";
    public static final String YES = "YES";
    public static final String NO = "NO";
    public static final String COLUMN_DEF = "COLUMN_DEF";
    public static final String IS_NULLABLE = "IS_NULLABLE";
    public static final String SCHEMAKEY = "currentSchema";
    /**
     * AF配置中心同步数据源的渠道用户标识
     */
    public static final String AF_CREATEUSER = "anyfabric";
    /**
     * 虚拟化目录高级参数名
     */
    public static final String VCATALOGNAME = "vCatalogName";

    public static final String VCONNECTOR = "vConnector";

    /**
     * 虚拟化字段类型
     */
    public static final String VIRTUAL_FIELD_TYPE = "virtualFieldType";

    /**
     * 虚拟化字段类型
     */
    public static final String ORIGIN_FIELD_TYPE = "originFieldType";
    /**
     * 数据源ddl监视触发器
     */
    public static final String DDL_MONITOR_TRIGGER = "ddlMonitorTrigger";
    /**
     * 数据源dd日志记录表
     */
    public static final String DDL_LOG_TABLE = "ddlLogTable";
}
