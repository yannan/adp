package com.eisoo.metadatamanage.web.service;

import com.eisoo.metadatamanage.lib.dto.LineageReportDto;

public interface ILineageService {

    void report(LineageReportDto reportDto);
}
