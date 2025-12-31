package com.eisoo.metadatamanage.web.service.impl.lineage;


import com.eisoo.metadatamanage.lib.dto.lineage.Lineage;
import com.eisoo.metadatamanage.web.commons.Constants;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

//@Service
public class LineageKafkaService {

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    public void sendToLineageResultTopic(List<Lineage> lineages, Lineage.Type createType) {
        if (lineages == null) {
            return;
        }
        for (Lineage row : lineages) {
            row.setCreateType(createType);
        }
        kafkaTemplate.send(Constants.METADATA_LINEAGE_TOPIC, JSONUtils.obj2json(lineages));
    }
}
