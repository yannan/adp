package com.eisoo.metadatamanage.web.service.impl.lineage;


import com.eisoo.metadatamanage.lib.dto.lineage.DataxJson;
import com.eisoo.metadatamanage.lib.dto.lineage.Lineage;
import com.eisoo.metadatamanage.lib.enums.DbType;
import com.eisoo.metadatamanage.web.util.JSONUtils;
import com.eisoo.metadatamanage.web.util.SqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataxLineageService extends LineageKafkaService {

    public void consumer(ConsumerRecords<String, String> records, Acknowledgment ack) {
        for (ConsumerRecord<String, String> record : records) {
            String value = record.value();
            List<Lineage> lineage = parseLineage(value);
            sendToLineageResultTopic(lineage, Lineage.Type.DATAX);
        }
        // 手动提交
        ack.acknowledge();
    }

    public List<Lineage> parseLineage(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        DataxJson dataxJson = JSONUtils.json2Obj(value, DataxJson.class);
        if (dataxJson == null || dataxJson.getInternal() == null || dataxJson.getInternal().getJob() == null) {
            return null;
        }

        DataxJson.Internal.Job job = dataxJson.getInternal().getJob();
        if (job.getContent() == null || job.getContent().isEmpty()) {
            return null;
        }

        DataxJson.Internal.Job.Content content = job.getContent().get(0);
        if (content == null) {
            return null;
        }

        DataxJson.Internal.Job.Content.Reader reader = content.getReader();
        DataxJson.Internal.Job.Content.Writer writer = content.getWriter();
        if (reader == null && writer == null) {
            return null;
        }
        try {
            String readerType = reader.getName();
            Lineage lineage = new Lineage();
            lineage.setCreateTime(dataxJson.getInternal().getTimestamp());
            List<String> sourceColumns = new ArrayList<>();
            if ("mysqlreader".equalsIgnoreCase(readerType)
                    || "oraclereader".equalsIgnoreCase(readerType)
                    || "postgresqlreader".equalsIgnoreCase(readerType)
                    || "rdbmsreader".equalsIgnoreCase(readerType)) {
                createRdbmsReaderParam(lineage, sourceColumns, reader);
            }

            String writeType = writer.getName();
            List<String> targetColumns = new ArrayList<>();
            if ("mysqlwriter".equalsIgnoreCase(writeType)
                    || "oraclewriter".equalsIgnoreCase(writeType)
                    || "postgresqlwriter".equalsIgnoreCase(writeType)
                    || "rdbmswriter".equalsIgnoreCase(writeType)) {
                createRdbmsWriterParam(lineage, targetColumns, writer);
            } else if ("hdfswriter".equalsIgnoreCase(writeType)) {
                createHiveWriterParam(lineage, targetColumns, writer);
            }
            return createLineage(lineage, sourceColumns, targetColumns);
        } catch (Exception e) {
            log.error("datax 血缘日志：{}", value);
            log.error("", e);
        }

        return null;
    }

    private List<Lineage> createLineage(Lineage lineage, List<String> sourceColumns, List<String> targetColumns) {
        List<Lineage> lineages = new ArrayList<>(targetColumns.size());
        // 字段级血缘
        if (sourceColumns.size() >= targetColumns.size()) {
            for (int i = 0; i < targetColumns.size(); i++) {

                Lineage tempLineage = new Lineage();
                tempLineage.setCreateTime(lineage.getCreateTime());
                Lineage.Vertex source = tempLineage.getSource();
                String srcTbName = lineage.getSource().getTbName();
                String srcColName = "";
                String[] sourceCols = sourceColumns.get(i).split("\\.");
                if (sourceCols.length > 1) {
                    srcTbName = sourceCols[0];
                    srcColName = sourceCols[1];
                } else {
                    srcColName = sourceColumns.get(i);
                }

                source.setDbType(lineage.getSource().getDbType());
                source.setDataSource(lineage.getSource().getDataSource());
                source.setDbName(lineage.getSource().getDbName());
                source.setDbSchema(lineage.getSource().getDbSchema());
                source.setTbName(srcTbName);
                source.setColumn(srcColName);

                Lineage.Vertex target = tempLineage.getTarget();
                target.setDbType(lineage.getTarget().getDbType());
                target.setDataSource(lineage.getTarget().getDataSource());
                target.setDbName(lineage.getTarget().getDbName());
                target.setDbSchema(lineage.getTarget().getDbSchema());
                target.setTbName(lineage.getTarget().getTbName());
                target.setColumn(targetColumns.get(i));

                tempLineage.setCreateType(Lineage.Type.DATAX);
                lineages.add(tempLineage);
            }
        }
        // 表级血缘
        else {
            String srcTable = lineage.getSource().getTbName();
            String[] tbs = srcTable.split(",");
            for (String tb : tbs) {
                Lineage tempLineage = new Lineage();
                Lineage.Vertex source = tempLineage.getSource();
                source.setDbType(lineage.getSource().getDbType());
                source.setDataSource(lineage.getSource().getDataSource());
                source.setDbSchema(lineage.getSource().getDbSchema());
                source.setDbName(lineage.getSource().getDbName());
                source.setTbName(tb);

                Lineage.Vertex target = tempLineage.getTarget();
                target.setDbType(lineage.getTarget().getDbType());
                target.setDataSource(lineage.getTarget().getDataSource());
                target.setDbName(lineage.getTarget().getDbName());
                target.setDbSchema(lineage.getTarget().getDbSchema());
                target.setTbName(lineage.getTarget().getTbName());

                lineages.add(tempLineage);
            }
        }
        return lineages;
    }

    private void createRdbmsReaderParam(Lineage lineage, List<String> sourceColumns, DataxJson.Internal.Job.Content.Reader reader) {
        DataxJson.Internal.Job.Content.Reader.ReaderParameter parameter = reader.getParameter();
        if (parameter == null) {
            return;
        }

        String readerName = reader.getName();
        DbType dbType = DbType.of(readerName.replace("reader", ""));
        String jdbcUrl = getJdbcUrl(parameter.getJdbcUrl());
        String userName = parameter.getUsername();
        String database = DbType.ORACLE.equals(dbType) ? null : getDatabase(parameter.getJdbcUrl());
        String schema = DbType.ORACLE.equals(dbType) ? parameter.getUsername() : null;
        String table = parameter.getTable();
        // 获取列
        if (parameter.getIsTableMode()) {
            // 查询配置["*"]时，拿不到列名
            if (parameter.getColumnList() != null) {
                sourceColumns.addAll(parameter.getColumnList());
            }

        } else {
            table = getTableByQuerySql(parameter.getQuerySql(), dbType);
            sourceColumns.addAll(getColumnsByQuerySql(parameter.getQuerySql(), dbType));
        }

        Lineage.Vertex source = lineage.getSource();
        source.setDbType(dbType);
        source.getDataSource().setJdbcUrl(jdbcUrl);
        source.getDataSource().setJdbcUser(userName);
        source.setDbName(database);
        source.setDbSchema(schema);
        source.setTbName(table);
    }

    private String getTableByQuerySql(String querySql, DbType type) {
        List<String> tableNames = SqlUtil.getTable(querySql, com.alibaba.druid.DbType.of(type.getDescp()));
        return StringUtils.join(tableNames, ",");
    }

    /**
     * @param querySql
     * @param type
     * @return
     */
    private List<String> getColumnsByQuerySql(String querySql, DbType type) {
        return SqlUtil.getColumn(querySql, com.alibaba.druid.DbType.of(type.getDescp()));
    }

    private String getDatabase(String jdbcUrl) {
        String[] strs = jdbcUrl.split("\\?");
        String url = strs[0];
        return url.substring(url.lastIndexOf("/") + 1, url.length());

    }


    /**
     * 去掉JDBC URL 问号后面的参数
     * 例如：
     * 输入：jdbc:mysql://10.4.132.224:3306/af_std?yearIsDateType=false&zeroDateTimeBehavior=convertToNull&tinyInt1isBit=false&rewriteBatchedStatements=true
     * 输出：jdbc:mysql://10.4.132.224:3306/af_std
     *
     * @param jdbcUrl
     * @return
     */
    private String getJdbcUrl(String jdbcUrl) {
        return jdbcUrl.split("\\?")[0];
    }


    private void createRdbmsWriterParam(Lineage lineage, List<String> targetColumns, DataxJson.Internal.Job.Content.Writer writer) {
        DataxJson.Internal.Job.Content.Writer.WriterParameter parameter = writer.getParameter();
        if (parameter == null) {
            return;
        }

        String writerName = writer.getName();
        DbType dbType = DbType.of(writerName.replace("writer", ""));
        String jdbcUrl = getJdbcUrl(parameter.getJdbcUrl());
        String userName = parameter.getUsername();
        String database = DbType.ORACLE.equals(dbType) ? null : getDatabase(parameter.getJdbcUrl());
        String schema = DbType.ORACLE.equals(dbType) ? parameter.getUsername() : null;
        String table = parameter.getTable();

        List<Object> columns = parameter.getColumn();
        for (Object row : columns) {
            targetColumns.add((String) row);
        }
        Lineage.Vertex target = lineage.getTarget();
        target.setDbType(dbType);
        target.getDataSource().setJdbcUrl(jdbcUrl);
        target.getDataSource().setJdbcUser(userName);
        target.setDbName(database);
        target.setDbSchema(schema);
        target.setTbName(table);
    }

    private static void createHiveWriterParam(Lineage lineage, List<String> targetColumns, DataxJson.Internal.Job.Content.Writer writer) {
        DataxJson.Internal.Job.Content.Writer.WriterParameter parameter = writer.getParameter();
        if (parameter == null) {
            return;
        }

        String path = parameter.getPath();
        if (StringUtils.isBlank(path)) {
            return;
        }

        if (path.startsWith("/warehouse") || (path.contains("hive") && path.contains(".db"))) {

            DbType dbType = DbType.HIVE;

            String[] strs = path.split(".db/");
            String database = strs[0].substring(strs[0].lastIndexOf("/") + 1, strs[0].length());

            String table = strs[1];
            if (table.contains("/")) {
                table = table.substring(0, table.indexOf("/"));
            }

            Lineage.Vertex target = lineage.getTarget();
            target.setDbType(dbType);
            target.setDbName(database);
            target.setTbName(table);

            List<Object> columns = parameter.getColumn();
            for (Object row : columns) {
                Map<String, String> map = (Map<String, String>) row;
                if (map.containsKey("name")) {
                    targetColumns.add(map.get("name"));
                }
            }
        }
    }
}
