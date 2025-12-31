package com.eisoo.metadatamanage.lib.vo;

import lombok.Data;

import java.util.List;

@Data
public class LineageColumnVo {

    List<Edge> edges;
    List<Table> vertices;

    @Data
    public static class Edge {

        String source;
        String target;
        long createTime;
        long createType;
        String content;
    }

    @Data
    public static class Table {
        String dbType;
        String dbName;
        String dbSchema;
        String tbName;
        List<dataSource> data_sources;
        List<Column> columns;

        @Data
        public static class dataSource {
            Long dsId;
            String dsName;
            String jdbcUrl;
            String jdbcUser;
        }

        @Data
        public static class Column {
            String vid;
            String columnName;
        }
    }

}
