package com.eisoo.metadatamanage.web.util;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CheckErrorVo {

	@JsonProperty("Key")
    private String errorCode; 
	
	@JsonProperty("Message")
    private String errorMsg; 
}
