package com.eisoo.metadatamanage.lib.dto.virtualization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class VirtualConnectorListDto {
    // {"connectors":[{"type":"SQL","olk_connector_name":"mysql","show_connector_name":"MySQL","connect_protocol":"jdbc"}
    List<VirtualConnectorDto> connectors;
    @Data
    public static class VirtualConnectorDto {
        String type;
        @JsonProperty(value = "olk_connector_name")
        String olkConnectorName;
        @JsonProperty(value = "show_connector_name")
        String showConnectorName;
        @JsonProperty(value = "connect_protocol")
        String connectProtocol;
    }
}
