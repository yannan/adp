package com.eisoo.metadatamanage.lib.dto.lineage;

import lombok.Data;

import java.util.List;

@Data
public class DataxJson {

    Internal internal;

    @Data
    public static class Internal {

        Job job;
        Long timestamp;

        @Data
        public static class Job {
            List<Content> content;

            @Data
            public static class Content {
                Reader reader;
                Writer writer;

                @Data
                public static class Reader {
                    String name;
                    ReaderParameter parameter;


                    @Data
                    public static class ReaderParameter {
                        String querySql;
                        String jdbcUrl;
                        String table;
                        List<String> columnList;
                        Boolean isTableMode;
                        String username;

                    }
                }

                @Data
                public static class Writer {
                    String name;
                    WriterParameter parameter;

                    @Data
                    public static class WriterParameter {
                        String jdbcUrl;
                        String table;
                        List<Object> column;
                        String path;
                        String defaultFS;
                        String fileName;
                        String username;
                    }
                }
            }
        }
    }
}
