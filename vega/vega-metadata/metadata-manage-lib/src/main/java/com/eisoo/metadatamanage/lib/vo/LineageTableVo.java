package com.eisoo.metadatamanage.lib.vo;

import lombok.Data;

import java.util.List;

@Data
public class LineageTableVo {

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
        String vid;
        String dbType;
        String dbName;
        String dbSchema;
        String tbName;
        List<dataSource> dataSources;

        @Data
        public static class dataSource {
            Long dsId;
            String dsName;
            String jdbcUrl;
            String jdbcUser;
        }
    }

}
