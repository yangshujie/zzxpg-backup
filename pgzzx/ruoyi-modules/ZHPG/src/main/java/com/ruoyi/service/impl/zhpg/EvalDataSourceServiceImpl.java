package com.ruoyi.service.impl.zhpg;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.domain.zhpg.EvalDataSource;
import com.ruoyi.domain.zhpg.dto.DataSourceTestResult;
import com.ruoyi.mapper.zhpg.EvalDataSourceMapper;
import com.ruoyi.service.zhpg.IEvalDataSourceService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评估数据源 Service 实现。
 */
@Service
public class EvalDataSourceServiceImpl extends ServiceImpl<EvalDataSourceMapper, EvalDataSource>
        implements IEvalDataSourceService {

    private static final AtomicLong CODE_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    private static final Set<String> DB_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "MYSQL", "POSTGRESQL", "ORACLE", "KINGBASE", "HIVE"
    )));
    private static final Set<String> FILE_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "CSV", "EXCEL", "TXT", "WORD", "WPS", "PDF", "DAT", "JSON"
    )));
    private static final String API_TYPE = "REST_API";

    @Autowired
    private EvalDataSourceFileStorageService fileStorageService;

    @Override
    @SuppressWarnings("unchecked")
    public Page<EvalDataSource> selectDataSourcePage(Page<?> page, EvalDataSource query) {
        QueryWrapper<EvalDataSource> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time");
        return baseMapper.selectPage((Page<EvalDataSource>) page, wrapper);
    }

    @Override
    public List<EvalDataSource> selectDataSourceList(EvalDataSource query) {
        QueryWrapper<EvalDataSource> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc("update_time");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<EvalDataSource> selectEnabledList(EvalDataSource query) {
        QueryWrapper<EvalDataSource> wrapper = buildQueryWrapper(query);
        wrapper.eq("status", "ENABLED");
        wrapper.orderByAsc("source_name");
        return baseMapper.selectList(wrapper);
    }

    @Override
    public EvalDataSource selectDataSourceDetail(Long id) {
        EvalDataSource dataSource = baseMapper.selectById(id);
        if (dataSource == null) {
            throw new ServiceException("评估数据源不存在");
        }
        return dataSource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDataSource(EvalDataSource dataSource) {
        normalizeBeforeSave(dataSource, false);
        validateDataSource(dataSource, null);
        return baseMapper.insert(dataSource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDataSource(EvalDataSource dataSource) {
        EvalDataSource existing = baseMapper.selectById(dataSource.getId());
        if (existing == null) {
            throw new ServiceException("评估数据源不存在");
        }
        normalizeBeforeSave(dataSource, true);
        validateDataSource(dataSource, dataSource.getId());
        return baseMapper.updateById(dataSource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDataSourceByIds(Long[] ids) {
        return baseMapper.deleteBatchIds(Arrays.asList(ids));
    }

    @Override
    public DataSourceTestResult testConnection(EvalDataSource dataSource) {
        EvalDataSource candidate = mergeWithExistingIfNecessary(dataSource);
        normalizeForTest(candidate);
        validateTestInput(candidate);
        if (isFileType(candidate.getSourceType())) {
            throw new ServiceException("文件类数据源通过文件服务上传接入，无需测试连接");
        }

        DataSourceTestResult result = new DataSourceTestResult();
        result.setTestTime(new Date());
        try {
            doTest(candidate);
            result.setSuccess(Boolean.TRUE);
            result.setStatus("SUCCESS");
            result.setMessage("连接测试成功");
        } catch (Exception e) {
            result.setSuccess(Boolean.FALSE);
            result.setStatus("FAILED");
            result.setMessage(resolveErrorMessage(e));
        }

        if (candidate.getId() != null) {
            updateLastTestResult(candidate.getId(), result);
        }
        return result;
    }

    private QueryWrapper<EvalDataSource> buildQueryWrapper(EvalDataSource query) {
        QueryWrapper<EvalDataSource> wrapper = new QueryWrapper<>();
        if (query == null) {
            return wrapper;
        }
        if (StringUtils.isNotBlank(query.getSourceName())) {
            wrapper.like("source_name", query.getSourceName().trim());
        }
        if (StringUtils.isNotBlank(query.getSourceType())) {
            wrapper.eq("source_type", query.getSourceType().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq("status", query.getStatus().trim().toUpperCase(Locale.ROOT));
        }
        return wrapper;
    }

    private void normalizeBeforeSave(EvalDataSource dataSource, boolean update) {
        dataSource.setSourceName(StringUtils.trimToEmpty(dataSource.getSourceName()));
        dataSource.setSourceType(normalizeEnum(dataSource.getSourceType()));
        dataSource.setStatus(StringUtils.defaultIfBlank(normalizeEnum(dataSource.getStatus()), "ENABLED"));
        dataSource.setRequestMethod(StringUtils.defaultIfBlank(normalizeEnum(dataSource.getRequestMethod()), "GET"));
        dataSource.setHost(StringUtils.trimToNull(dataSource.getHost()));
        dataSource.setDatabaseName(StringUtils.trimToNull(dataSource.getDatabaseName()));
        dataSource.setSchemaName(StringUtils.trimToNull(dataSource.getSchemaName()));
        dataSource.setTableName(StringUtils.trimToNull(dataSource.getTableName()));
        dataSource.setApiUrl(StringUtils.trimToNull(dataSource.getApiUrl()));
        dataSource.setFilePath(fileStorageService.normalizeManagedFilePath(dataSource.getFilePath()));
        dataSource.setUsername(StringUtils.trimToNull(dataSource.getUsername()));
        dataSource.setPassword(StringUtils.trimToNull(dataSource.getPassword()));
        dataSource.setFieldNames(StringUtils.trimToNull(dataSource.getFieldNames()));
        dataSource.setRequestParams(StringUtils.trimToNull(dataSource.getRequestParams()));
        dataSource.setDescription(StringUtils.trimToNull(dataSource.getDescription()));
        if (!update && StringUtils.isBlank(dataSource.getSourceCode())) {
            dataSource.setSourceCode("DS" + String.format("%06d", CODE_SEQ.incrementAndGet()));
        }
    }

    private void normalizeForTest(EvalDataSource dataSource) {
        dataSource.setSourceType(normalizeEnum(dataSource.getSourceType()));
        dataSource.setRequestMethod(StringUtils.defaultIfBlank(normalizeEnum(dataSource.getRequestMethod()), "GET"));
        dataSource.setHost(StringUtils.trimToNull(dataSource.getHost()));
        dataSource.setDatabaseName(StringUtils.trimToNull(dataSource.getDatabaseName()));
        dataSource.setSchemaName(StringUtils.trimToNull(dataSource.getSchemaName()));
        dataSource.setTableName(StringUtils.trimToNull(dataSource.getTableName()));
        dataSource.setApiUrl(StringUtils.trimToNull(dataSource.getApiUrl()));
        dataSource.setFilePath(fileStorageService.normalizeManagedFilePath(dataSource.getFilePath()));
        dataSource.setUsername(StringUtils.trimToNull(dataSource.getUsername()));
        dataSource.setPassword(StringUtils.trimToNull(dataSource.getPassword()));
        dataSource.setRequestParams(StringUtils.trimToNull(dataSource.getRequestParams()));
    }

    private void validateDataSource(EvalDataSource dataSource, Long excludeId) {
        if (StringUtils.isBlank(dataSource.getSourceName())) {
            throw new ServiceException("数据源名称不能为空");
        }
        if (StringUtils.isBlank(dataSource.getSourceType())) {
            throw new ServiceException("数据源类型不能为空");
        }
        validateUniqueName(dataSource.getSourceName(), excludeId);
        validateByType(dataSource);
    }

    private void validateTestInput(EvalDataSource dataSource) {
        if (StringUtils.isBlank(dataSource.getSourceType())) {
            throw new ServiceException("请先选择数据源类型");
        }
        validateByType(dataSource);
    }

    private void validateUniqueName(String name, Long excludeId) {
        QueryWrapper<EvalDataSource> wrapper = new QueryWrapper<>();
        wrapper.eq("source_name", name);
        if (excludeId != null) {
            wrapper.ne("id", excludeId);
        }
        if (baseMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("数据源名称已存在，请使用其他名称");
        }
    }

    private void validateByType(EvalDataSource dataSource) {
        String sourceType = dataSource.getSourceType();
        if (isDatabaseType(sourceType)) {
            if (StringUtils.isBlank(dataSource.getHost())) {
                throw new ServiceException("数据库类数据源必须填写主机地址");
            }
            if (StringUtils.isBlank(dataSource.getDatabaseName())) {
                throw new ServiceException("数据库类数据源必须填写数据库/服务名称");
            }
            return;
        }
        if (API_TYPE.equals(sourceType)) {
            if (StringUtils.isBlank(dataSource.getApiUrl())) {
                throw new ServiceException("REST接口类数据源必须填写接口地址");
            }
            return;
        }
        if (isFileType(sourceType)) {
            if (StringUtils.isBlank(dataSource.getFilePath())) {
                throw new ServiceException("文件类数据源必须上传文件并生成文件服务路径");
            }
            fileStorageService.validateManagedFilePath(sourceType, dataSource.getFilePath());
            return;
        }
        throw new ServiceException("暂不支持的数据源类型: " + sourceType);
    }

    private EvalDataSource mergeWithExistingIfNecessary(EvalDataSource dataSource) {
        if (dataSource.getId() == null) {
            return dataSource;
        }
        EvalDataSource existing = baseMapper.selectById(dataSource.getId());
        if (existing == null) {
            return dataSource;
        }
        EvalDataSource merged = new EvalDataSource();
        merged.setId(existing.getId());
        merged.setSourceType(StringUtils.defaultIfBlank(dataSource.getSourceType(), existing.getSourceType()));
        merged.setHost(StringUtils.defaultIfBlank(dataSource.getHost(), existing.getHost()));
        merged.setPort(dataSource.getPort() != null ? dataSource.getPort() : existing.getPort());
        merged.setDatabaseName(StringUtils.defaultIfBlank(dataSource.getDatabaseName(), existing.getDatabaseName()));
        merged.setSchemaName(StringUtils.defaultIfBlank(dataSource.getSchemaName(), existing.getSchemaName()));
        merged.setTableName(StringUtils.defaultIfBlank(dataSource.getTableName(), existing.getTableName()));
        merged.setApiUrl(StringUtils.defaultIfBlank(dataSource.getApiUrl(), existing.getApiUrl()));
        merged.setRequestMethod(StringUtils.defaultIfBlank(dataSource.getRequestMethod(), existing.getRequestMethod()));
        merged.setFilePath(StringUtils.defaultIfBlank(dataSource.getFilePath(), existing.getFilePath()));
        merged.setUsername(StringUtils.defaultIfBlank(dataSource.getUsername(), existing.getUsername()));
        merged.setPassword(dataSource.getPassword() != null ? dataSource.getPassword() : existing.getPassword());
        merged.setRequestParams(StringUtils.defaultIfBlank(dataSource.getRequestParams(), existing.getRequestParams()));
        return merged;
    }

    private void doTest(EvalDataSource dataSource) throws Exception {
        if (isDatabaseType(dataSource.getSourceType())) {
            testDatabaseConnection(dataSource);
            return;
        }
        if (API_TYPE.equals(dataSource.getSourceType())) {
            testApiConnection(dataSource);
            return;
        }
        throw new ServiceException("暂不支持测试该类型数据源");
    }

    private void testDatabaseConnection(EvalDataSource dataSource) throws Exception {
        String driverClass = resolveDriverClass(dataSource.getSourceType());
        if (StringUtils.isBlank(driverClass)) {
            throw new ServiceException("当前服务未集成该数据源类型驱动，暂不支持测试连接");
        }
        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new ServiceException("当前服务未集成 " + dataSource.getSourceType() + " 驱动，暂不支持测试连接");
        }
        String jdbcUrl = buildJdbcUrl(dataSource);
        try (Connection ignored = DriverManager.getConnection(
                jdbcUrl,
                StringUtils.defaultString(dataSource.getUsername()),
                StringUtils.defaultString(dataSource.getPassword()))) {
            // 成功建立连接即可
        }
    }

    private void testApiConnection(EvalDataSource dataSource) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(dataSource.getApiUrl()).openConnection();
        String requestMethod = normalizeRequestMethod(dataSource.getRequestMethod());
        connection.setRequestMethod(requestMethod);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json, text/plain, */*");
        if (requiresRequestBody(requestMethod) && StringUtils.isNotBlank(dataSource.getRequestParams())) {
            byte[] bytes = dataSource.getRequestParams().getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(bytes);
            }
        }
        int code = connection.getResponseCode();
        if (code < 200 || code >= 400) {
            throw new ServiceException("接口返回状态码异常: " + code);
        }
    }

    private void updateLastTestResult(Long id, DataSourceTestResult result) {
        EvalDataSource update = new EvalDataSource();
        update.setId(id);
        update.setLastTestStatus(result.getStatus());
        update.setLastTestMessage(result.getMessage());
        update.setLastTestTime(result.getTestTime());
        try {
            update.setUpdateBy(SecurityUtils.getUsername());
        } catch (Exception ignored) {
            // 非登录态调用时不影响测试记录保存
        }
        update.setUpdateTime(new Date());
        baseMapper.updateById(update);
    }

    private String buildJdbcUrl(EvalDataSource dataSource) {
        String type = dataSource.getSourceType();
        String host = dataSource.getHost();
        int port = resolvePort(dataSource);
        String databaseName = dataSource.getDatabaseName();
        if ("MYSQL".equals(type)) {
            return String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false",
                    host, port, databaseName
            );
        }
        if ("POSTGRESQL".equals(type)) {
            return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
        }
        if ("KINGBASE".equals(type)) {
            return String.format("jdbc:kingbase8://%s:%d/%s", host, port, databaseName);
        }
        if ("ORACLE".equals(type)) {
            return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, databaseName);
        }
        if ("HIVE".equals(type)) {
            return String.format("jdbc:hive2://%s:%d/%s", host, port, databaseName);
        }
        throw new ServiceException("不支持的数据库类型: " + type);
    }

    private int resolvePort(EvalDataSource dataSource) {
        if (dataSource.getPort() != null && dataSource.getPort() > 0) {
            return dataSource.getPort();
        }
        switch (dataSource.getSourceType()) {
            case "MYSQL":
                return 3306;
            case "POSTGRESQL":
                return 5432;
            case "KINGBASE":
                return 54321;
            case "ORACLE":
                return 1521;
            case "HIVE":
                return 10000;
            default:
                return 0;
        }
    }

    private String resolveDriverClass(String sourceType) {
        switch (sourceType) {
            case "MYSQL":
                return "com.mysql.cj.jdbc.Driver";
            case "POSTGRESQL":
                return "org.postgresql.Driver";
            case "KINGBASE":
                return "com.kingbase8.Driver";
            case "ORACLE":
                return "oracle.jdbc.OracleDriver";
            case "HIVE":
                return "org.apache.hive.jdbc.HiveDriver";
            default:
                return null;
        }
    }

    private boolean isDatabaseType(String sourceType) {
        return DB_TYPES.contains(sourceType);
    }

    private boolean isFileType(String sourceType) {
        return FILE_TYPES.contains(sourceType);
    }

    private String normalizeEnum(String value) {
        return StringUtils.isBlank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequestMethod(String requestMethod) {
        String method = StringUtils.defaultIfBlank(requestMethod, "GET").toUpperCase(Locale.ROOT);
        switch (method) {
            case "GET":
            case "POST":
            case "PUT":
            case "DELETE":
            case "HEAD":
                return method;
            default:
                return "GET";
        }
    }

    private boolean requiresRequestBody(String requestMethod) {
        return "POST".equals(requestMethod) || "PUT".equals(requestMethod);
    }

    private String resolveErrorMessage(Exception exception) {
        if (exception instanceof ServiceException) {
            return exception.getMessage();
        }
        String message = exception.getMessage();
        if (StringUtils.isBlank(message)) {
            return "连接测试失败";
        }
        return message;
    }
}
