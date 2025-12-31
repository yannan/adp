package com.eisoo.metadatamanage.web.util;

import com.eisoo.metadatamanage.db.entity.DataSourceEntity;
import com.eisoo.metadatamanage.db.entity.SchemaEntity;
import com.eisoo.metadatamanage.lib.dto.DdlLogDto;
import com.eisoo.metadatamanage.lib.dto.LiveDdlDto;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.lib.enums.DdlTypeEnum;
import com.eisoo.metadatamanage.web.extra.inf.ConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.MySQLConnectionParam;
import com.eisoo.metadatamanage.web.extra.model.PostgreSQLConnectionParam;
import com.eisoo.standardization.common.util.AiShuUtil;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class DataSourceUtilsTest {
    private static final String commentColumnPostgresql = "COMMENT ON COLUMN public.table100.field1_new IS 'ddd'";
    private static final String renameTablePostgresql = "ALTER TABLE public.example_table RENAME TO example_table1";
    private static final String commentTablePostgresql = "COMMENT ON TABLE public.example_table1 IS 'xxx'";
    private static final String addColumnPostgresql = "ALTER TABLE public.example_table1 ADD column1 varchar NULL";
    private static final String alterColumnPostgresql = "ALTER TABLE public.example_table1 ALTER COLUMN column1 TYPE real USING column1::real";
    private static final String dropColumnPostgresql = "ALTER TABLE public.example_table1 DROP COLUMN column1";
    private static final String copyTablePostgresql = "CREATE TABLE new_table AS TABLE example_table1 WITH NO DATA";
    private static final String dropTablePostgresql = "DROP TABLE public.new_table";
    private static final String createTablePostgresql = "CREATE TABLE public.aaa (id1 int4, id2 int4, id3 int4, id40 int4)";

    private static final String createTableMysql = "CREATE TABLE if NOT exists t_test (f_id BIGINT NOT NULL AUTO_INCREMENT) comment = 'xxxxxxxxxxxxxxxxx'";
    private static final String commentTableMysql = "ALTER TABLE t_test COMMENT='测试表'";
    private static final String commentColumnMysql = "ALTER TABLE t_test CHANGE COLUMN f_update_message f_update_message VARCHAR(2000) NULL DEFAULT NULL COMMENT '更新信息1' AFTER f_update_status";
    private static final String renameTableMysql = "RENAME TABLE aa.t_live_ddl TO aa.t_live_ddl1";
    private static final String alterColumnMysql = "ALTER TABLE t_test MODIFY COLUMN  f_table_id BIGINT(20) NOT NULL COMMENT 'Table唯一标识' FIRST";
    private static final String dropColumnMysql = "ALTER TABLE t_test DROP COLUMN f_version";
    private static final String dropTableMysql = "DROP TABLE IF EXISTS t_live_ddl";
    private static final DbType dbTypePostgresql = DbType.POSTGRESQL;
    private static final DbType dbTypeMysql = DbType.MYSQL;
    private static final String connectionParamStrPostgresql = "{\"user\":\"aaaa\",\"password\":\"bbbb\",\"address\":\"jdbc:postgresql://www.hologres.aliyuncs.com:80\",\"database\":\"db_dev\",\"jdbcUrl\":\"jdbc:postgresql://www.hologres.aliyuncs.com:80/db_dev\",\"driverClassName\":\"org.postgresql.Driver\",\"validationQuery\":\"select version()\",\"other\":\"currentSchema=public&vCatalogName=holo_001&vConnector=hologres\",\"props\":{\"vConnector\":\"hologres\",\"currentSchema\":\"public\",\"vCatalogName\":\"holo_001\"}}";
    private static final String connectionParamStrMysql = "{\"user\":\"aaaa\",\"password\":\"bbbb\",\"address\":\"jdbc:mysql://localhost:3306\",\"database\":\"db_dev\",\"jdbcUrl\":\"jdbc:mysql://localhost:80/db_dev\",\"driverClassName\":\"com.mysql.jdbc.Driver\",\"validationQuery\":\"select 1\",\"other\":\"currentSchema=db_dev&vCatalogName=mysql_001&vConnector=mysql\",\"props\":{\"vConnector\":\"mysql\",\"currentSchema\":\"db_dev\",\"vCatalogName\":\"mysql_001\"}}";
    private static final String datasourceParamStrPostgresql = "{\"user\":\"aaaa\",\"password\":\"bbbb\",\"address\":\"jdbc:postgresql://www.hologres.aliyuncs.com:80\",\"database\":\"db_dev\",\"jdbcUrl\":\"jdbc:postgresql://www.hologres.aliyuncs.com:80/db_dev\",\"driverLocation\":null,\"driverClassName\":\"org.postgresql.Driver\",\"validationQuery\":\"select version()\",\"other\":\"currentSchema=public&vCatalogName=holo_001&vConnector=hologres\",\"props\":{\"vConnector\":\"hologres\",\"currentSchema\":\"public\",\"vCatalogName\":\"holo_001\"}}";
    private static final String datasourceParamStrMysql = "{\"user\":\"aaaa\",\"password\":\"bbbb\",\"address\":\"jdbc:mysql://localhost:3306\",\"database\":\"db_dev\",\"jdbcUrl\":\"jdbc:mysql://localhost:3306/db_dev\",\"driverLocation\":null,\"driverClassName\":\"org.mysql.jdbc.Driver\",\"validationQuery\":\"select 1\",\"other\":\"currentSchema=db_dev&vCatalogName=mysql_001&vConnector=mysql\",\"props\":{\"vConnector\":\"mysql\",\"currentSchema\":\"db_dev\",\"vCatalogName\":\"mysql_001\"}}";

    @Test
    void getConnection() {
        PostgreSQLConnectionParam connectionParam = JSONUtils.json2Obj(connectionParamStrPostgresql, PostgreSQLConnectionParam.class);
        Assert.assertEquals(connectionParam.getJdbcUrl(), "jdbc:postgresql://www.hologres.aliyuncs.com:80/db_dev");
        //mysql不需要验证连接数据源能力，因为是被动接受binlog解析结果
    }

    @Test
    void getJdbcUrl() {
        PostgreSQLConnectionParam connectionParam = JSONUtils.json2Obj(connectionParamStrPostgresql, PostgreSQLConnectionParam.class);
        Assert.assertEquals(connectionParam.getJdbcUrl(), "jdbc:postgresql://www.hologres.aliyuncs.com:80/db_dev");
        //mysql不需要验证连接数据源能力，因为是被动接受binlog解析结果

    }

    @Test
    void buildConnectionParams() {
        ConnectionParam connectionParam = DataSourceUtils.buildConnectionParams(dbTypePostgresql, datasourceParamStrPostgresql);
        PostgreSQLConnectionParam postgreSQLConnectionParam = (PostgreSQLConnectionParam) connectionParam;
        Assert.assertEquals(postgreSQLConnectionParam.getJdbcUrl(), "jdbc:postgresql://www.hologres.aliyuncs.com:80/db_dev");
        //mysql不需要验证连接数据源能力，因为是被动接受binlog解析结果
    }
    @Test
    void getLiveDdlDto() {
        Date now = new Date();
        List<SchemaEntity> schemaEntityList = new ArrayList<>();
        SchemaEntity schemaEntity = new SchemaEntity();
        schemaEntity.setId(1l);
        schemaEntity.setName("public");
        schemaEntity.setDataSourceId("1l");
        schemaEntityList.add(schemaEntity);

        List<DataSourceEntity> dataSourceEntityList = new ArrayList<>();
        DataSourceEntity dataSourceEntity = new DataSourceEntity();
        dataSourceEntity.setId("1l");
        dataSourceEntity.setName("测试数据源");

        //postgresql解析ddl验证
        dataSourceEntity.setDataSourceType(3);
        dataSourceEntity.setDataSourceTypeName("PostgreSQL");
        dataSourceEntity.setExtendProperty("currentSchema=public&vCatalogName=holo_001&vConnector=hologres&");
        dataSourceEntityList.add(dataSourceEntity);
        Boolean expectExists = true;

        DdlLogDto ddlLogDto = new DdlLogDto();
        ddlLogDto.setStatement(commentColumnPostgresql);
        ddlLogDto.setDdlTime(now);
        List<LiveDdlDto> ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        DdlTypeEnum expectType = DdlTypeEnum.CommentColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(renameTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.RenameTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(commentTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.CommentTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(addColumnPostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(alterColumnPostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(dropColumnPostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(copyTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.CreateTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(dropTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.DropTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
        ddlDtoList.clear();

        ddlLogDto.setStatement(createTablePostgresql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.CreateTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        //mysql解析ddl验证
        dataSourceEntity.setDataSourceType(2);
        dataSourceEntity.setDataSourceTypeName("MySQL");
        dataSourceEntity.setExtendProperty("currentSchema=mysql_001&vCatalogName=mysql_001&vConnector=mysql&");

        ddlLogDto.setStatement(createTableMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.CreateTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        ddlLogDto.setStatement(commentTableMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.CommentTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        ddlLogDto.setStatement(commentColumnMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        ddlLogDto.setStatement(renameTableMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.RenameTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        ddlLogDto.setStatement(alterColumnMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        ddlLogDto.setStatement(dropColumnMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.AlterColumn;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());

        ddlLogDto.setStatement(dropTableMysql);
        ddlDtoList = DataSourceUtils.getLiveDdlDto(ddlLogDto, schemaEntityList, dataSourceEntityList);
        Assert.assertEquals(expectExists, AiShuUtil.isNotEmpty(ddlDtoList));
        expectType = DdlTypeEnum.DropTable;
        Assert.assertEquals(expectType, ddlDtoList.get(0).getType());
    }
}