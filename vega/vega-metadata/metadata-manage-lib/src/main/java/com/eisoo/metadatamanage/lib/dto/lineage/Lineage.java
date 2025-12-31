package com.eisoo.metadatamanage.lib.dto.lineage;

import com.eisoo.metadatamanage.lib.enums.DbType;
import lombok.Data;

import java.util.Date;

@Data
public class Lineage {


    public static enum Type {
        HIVE, DATAX, SPARK, USER_REPORT
    }

    Vertex source = new Vertex();
    Vertex target = new Vertex();

    Long createTime; // 创建时间
    Type createType; // 创建类型

    /**
     * 生成血缘的内容，hive：sql脚本，spark：sql脚本（获取不到），datax：datax配置文件（不是原始配置文件，拿不到原始配置文件）
     */
    String queryText;


    @Data
    public static class Vertex {
        DbType dbType;
        DataSource dataSource = new DataSource();
        String dbName;
        String dbSchema;
        String tbName;
        String column;

        @Data
        public static class DataSource {
            Long dsId;
            String jdbcUrl;
            String jdbcUser;
        }
    }

}
