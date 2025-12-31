package com.eisoo.metadatamanage.web.util;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class TableCheckError {
    @JsonIgnore
    @JsonProperty("Key")
    private String tableName;

    @JsonProperty("Message")
    private String tableError;
	
	@JsonProperty("FieldMessage")
    private List<FieldCheckError> fieldErrors;
}
