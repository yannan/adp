package com.eisoo.lineage.presto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Lan Tian
 * @Date: 2024/5/15 10:00
 * @Version:1.0
 */
@Data
public class ColumnMetaEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String type;
    private String origType;
    private Boolean primaryKey;
    private Boolean nullAble;
    private Integer columnDef;
    private String comment;
}
