package com.eisoo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/7 11:21
 * @Version:1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnyDataBuilderParaDto {
//  {"graph_id": 263, "name": "lineage_table","action": "delete","data_type": "entity","graph_data": [{"uuid": "table3"}]}
    @JsonProperty("graph_id")
    private Integer graphId;
    private String name;
    private String action;
    @JsonProperty(value = "data_type", defaultValue = "entity")
    private String dataType = "entity";
    @JsonProperty("graph_data")
    private List graphData;

    public AnyDataBuilderParaDto(final Integer graphId, final String name, final String action, final String dataType) {
        this.graphId = graphId;
        this.name = name;
        this.action = action;
        this.dataType = dataType;
    }

    public AnyDataBuilderParaDto(final Integer graphId, final String name, final String action, final List graphData) {
        this.graphId = graphId;
        this.name = name;
        this.action = action;
        this.graphData = graphData;
    }

    public AnyDataBuilderParaDto(final Integer graphId, final String name, final String action) {
        this.graphId = graphId;
        this.name = name;
        this.action = action;
    }

    public AnyDataBuilderParaDto(final Integer graphId, final String name) {
        this.graphId = graphId;
        this.name = name;
    }
}
