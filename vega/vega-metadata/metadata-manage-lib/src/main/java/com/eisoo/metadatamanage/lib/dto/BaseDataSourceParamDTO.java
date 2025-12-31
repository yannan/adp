package com.eisoo.metadatamanage.lib.dto;

import com.eisoo.metadatamanage.lib.enums.DbType;

import java.io.Serializable;
import java.util.Map;

/**
 * @Author: WangZiYu
 * @description:com.eisoo.metadatamanage.lib.dto
 * @Date: 2023/3/30 14:43
 */
public abstract class BaseDataSourceParamDTO implements Serializable {

    protected Integer id;

    protected String name;

    protected String note;

    protected String host;

    protected Integer port;

    protected String database;

    protected String userName;

    protected String password;

    protected Map<String, String> other;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getOther() {
        return other;
    }

    public void setOther(Map<String, String> other) {
        this.other = other;
    }

    /**
     * Get the datasource type
     * see{@link DbType}
     *
     * @return datasource type code
     */
    public abstract DbType getType();
}
