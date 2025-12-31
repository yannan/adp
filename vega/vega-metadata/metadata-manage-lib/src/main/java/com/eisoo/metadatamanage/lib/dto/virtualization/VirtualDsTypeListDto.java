package com.eisoo.metadatamanage.lib.dto.virtualization;

import lombok.Data;

import java.util.List;

@Data
public class VirtualDsTypeListDto {
    List<VirtualDsTypeDto> connectorName;
    @Data
    public static class VirtualDsTypeDto {
        String olkConnectorMame;
        String showConnectorName;
    }
}
