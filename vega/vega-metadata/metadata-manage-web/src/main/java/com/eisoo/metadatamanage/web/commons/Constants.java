package com.eisoo.metadatamanage.web.commons;

public class Constants {

    public final static String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";
    public final static String DEMENG_DRIVER = "dm.jdbc.driver.DmDriver";

    /**
     * 血缘清洗后存入的topic
     */
    public final static String METADATA_LINEAGE_TOPIC = "metadata_lineage_result";


    /**
     * HIVE 血缘 topic
     */
    public final static String HIVE_LINEAGE_TOPIC = "lineage_log.hive";

    /**
     * spark 血缘 topic
     */
    public final static String SPARK_LINEAGE_TOPIC = "lineage_log.spark";

    /**
     * datax 血缘 topic
     */
    public final static String DATAX_LINEAGE_TOPIC = "lineage_log.datax";


    /**
     * group 前缀
     */
    public final static String KAFKA_GROUP_ID_PREFIX = "group-";

    public final static String KAFKA_GROUP_ID_DATAX_LINEAGE = KAFKA_GROUP_ID_PREFIX + DATAX_LINEAGE_TOPIC;

    public final static String KAFKA_GROUP_ID_SPARK_LINEAGE = KAFKA_GROUP_ID_PREFIX + SPARK_LINEAGE_TOPIC;

    public final static String KAFKA_GROUP_ID_HIVE_LINEAGE = KAFKA_GROUP_ID_PREFIX + HIVE_LINEAGE_TOPIC;
    public final static String KAFKA_GROUP_ID_METADATA_LINEAGE = KAFKA_GROUP_ID_PREFIX + METADATA_LINEAGE_TOPIC;
}
