package com.eisoo.metadatamanage.web.extra.service.dipDataSourceService;

import com.eisoo.metadatamanage.lib.dto.virtualization.VirtualConnectorListDto;

public interface IDipDataSourceService {
    Boolean createConnector();
    VirtualConnectorListDto getConnectors();
}
