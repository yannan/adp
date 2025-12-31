package com.eisoo.metadatamanage.lib.dto.lineage;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class HiveLogLineage {
    private String database;
    private String queryText;
    private List<Edge> edges;
    private Set<Vertex> vertices;
    private Long timestamp;


    @Data
    public static final class Edge {
        public static enum Type {
            PROJECTION, PREDICATE
        }

        private Set<Integer> sources;
        private Set<Integer> targets;
        private Type edgeType;
    }

    @Data
    public static final class Vertex {

        public static enum Type {
            COLUMN, TABLE
        }

        private Type vertexType;
        private int id;
        private String vertexId;
    }

}
