package com.eisoo.metadatamanage.web.extra.model;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.model
 * @Date: 2023/3/30 16:11
 */

import java.util.Date;

import com.eisoo.metadatamanage.lib.enums.DbType;
import lombok.Data;

@Data
public class DataSource {
    /**
     * id
     */
    private Integer id;

    /**
     * user id
     */
    private int userId;

    /**
     * user name
     */
    private String userName;

    /**
     * data source name
     */
    private String name;

    /**
     * note
     */
    private String note;

    /**
     * data source type
     */
    private DbType type;

    /**
     * connection parameters
     */
    private String connectionParams;

    /**
     * create time
     */
    private Date createTime;

    /**
     * update time
     */
    private Date updateTime;

    private ConnectionParamObject connectionParamObject;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataSource that = (DataSource) o;

        if (id != that.id) {
            return false;
        }
        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + name.hashCode();
        return result;
    }
}
