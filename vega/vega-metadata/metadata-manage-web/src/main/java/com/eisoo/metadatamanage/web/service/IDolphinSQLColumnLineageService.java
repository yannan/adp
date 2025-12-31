package com.eisoo.metadatamanage.web.service;

import com.eisoo.entity.DolphinLineageEntity;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
/**
 * @Author: Lan Tian
 * @Date: 2024/5/11 16:12
 * @Version:1.0
 */
public interface IDolphinSQLColumnLineageService {
    void sendDolphinSQLColumnInstructionToADLineage(ArrayList<DolphinLineageEntity.DolphinColumnLineage> lineageDomainList) throws UnsupportedEncodingException;
}
