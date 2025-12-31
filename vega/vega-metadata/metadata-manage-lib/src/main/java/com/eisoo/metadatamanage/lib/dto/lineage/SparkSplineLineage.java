package com.eisoo.metadatamanage.lib.dto.lineage;

import lombok.Data;

import java.util.List;

@Data
public class SparkSplineLineage {

    String id;
    String name;
    Operations operations;
    List<Attribute> attributes;
    Expressions expressions;

    @Data
    public static class Operations {
        Write write;
        List<Read> reads;
        List<Other> other;

        @Data
        public static class Write {
            String id;
            List<String> childIds;
            Params params;
            Extra extra;

            @Data
            public static class Extra {
                String destinationType;
            }
        }

        @Data
        public static class Read {
            String id;
            List<String> output;
            Params params;
            Extra extra;

            @Data
            public static class Extra {
                String sourceType;
            }
        }

        @Data
        public static class Other {
            String id;
            List<String> childIds;
            List<String> output;
        }

        @Data
        public static class Params {
            Table table;

            @Data
            public static class Table {
                Identifier identifier;

                @Data
                public static class Identifier {
                    String table;
                    String database;
                }
            }
        }


    }


    @Data
    public static class Attribute {
        String id;
        String name;
        List<ChildRef> childRefs;

        @Data
        public static class ChildRef {
            String __exprId;
        }
    }


    @Data
    public static class Expressions {
        List<Function> functions;

        @Data
        public static class Function {
            String id;
            List<ChildRef> childRefs;
            Extra extra;
            String name;

            @Data
            public static class ChildRef {
                String __attrId;
                String __exprId;
            }

            @Data
            public static class Extra {
                String simpleClassName;
                String _typeHint;
            }
        }
    }


}
