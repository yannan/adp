package com.eisoo.metadatamanage.web.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.druid.util.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class FieldCheckError {
    @JsonIgnore
    private Integer fieldIndex;

    @JsonProperty("Key")
    private String key; 
	
	@JsonProperty("Message")
    private List<String> errors; 

    public FieldCheckError(Integer fieldIndex, String fieldName, List<String> errors) {
        this.fieldIndex = fieldIndex;
        this.errors = errors;
        this.key = String.format("fields[%d].%s", fieldIndex, fieldName);
    }

    public boolean addError(String err) {
        if (StringUtils.isEmpty(err)) {
            return false;
        }
        return errors.add(err);
    }

    public boolean addError(String... errs) {
        return errors.addAll(Arrays.stream(errs).collect(Collectors.toList()));
    }

    public boolean addError(List<String> errs) {
        return errors.addAll(errs);
    }
}
