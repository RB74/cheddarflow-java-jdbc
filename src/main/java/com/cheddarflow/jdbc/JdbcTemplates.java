package com.cheddarflow.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource
public final class JdbcTemplates {

    private static final String JDBC_URL_SUFFIX = "?autoReconnect=true&autoReconnectForPools=true&useSSL=false"
      + "&cachePrepStmts=true&prepStmtCacheSize=250&prepStmtCacheSize=2048&prepStmtCacheSqlLimit=2048"
      + "&maintainTimeStats=false&jdbcCompliantTruncation=false&cacheResultSetMetadata=true&useUnicode=true&characterEncoding=UTF-8"
      + "&useCompression=true";

    private static final JdbcTemplates INSTANCE = new JdbcTemplates();

    private final Map<Key, JdbcTemplate> templateMap = new ConcurrentHashMap<>();

    @Value("${db.host}")
    private String dbHost;
    @Value("${db.read-only-host}")
    private String dbReadOnlyHost;
    @Value("${db.name}")
    private String dbName;
    @Value("${db.username}")
    private String dbUser;
    @Value("${db.password}")
    private String dbPassword;

    private static final class Key {
        private final String tenant;
        private final boolean readOnly;

        private Key(String tenant, boolean readOnly) {
            this.tenant = tenant;
            this.readOnly = readOnly;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key)o;

            if (!Objects.equals(tenant, key.tenant)) return false;
            return readOnly == key.readOnly;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenant, readOnly);
        }
    }

    private JdbcTemplates() {
    }

    public static JdbcTemplates getInstance() {
        return INSTANCE;
    }

    public JdbcTemplate getTemplate(boolean readOnly) {
        return this.templateMap.computeIfAbsent(new Key("cflow", readOnly),
          id -> this.createTemplate(id.tenant, id.readOnly));
    }

    private JdbcTemplate createTemplate(final String tenant, final boolean readOnly) {
        final Map<String, String> props = this.getDataSourceProperties(tenant, readOnly);
        final Properties properties = new Properties();
        props.forEach(properties::setProperty);
        final HikariConfig hikariConfig = new HikariConfig(properties);
        final HikariDataSource dataSource = new HikariDataSource(hikariConfig);
        return new JdbcTemplate(dataSource);
    }

    private Map<String, String> getDataSourceProperties(String tenantIdentifier, boolean readOnly) {
        final Map<String, String> props = new HashMap<>();
        props.put("dataSourceClassName", "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource");
        props.put("dataSource.url", String.format("jdbc:mysql://%s/%s" + JDBC_URL_SUFFIX,
          readOnly ? this.dbReadOnlyHost : this.dbHost, this.dbName));
        props.put("dataSource.user", this.dbUser);
        props.put("dataSource.password", this.dbPassword);
        props.put("transactionIsolation", "TRANSACTION_REPEATABLE_READ");
        props.put("poolName", tenantIdentifier + " Master Data Source" + (readOnly ? " (Read-Only)" : ""));
        props.put("maximumPoolSize", "10");
        props.put("minimumIdle", "2");
        props.put("registerMbeans", "false");
        props.put("connectionTestQuery", readOnly ? "select 1" : "/* ping */");
        if (readOnly) {
            props.put("readOnly", "true");
        }
        return props;
    }

    @ManagedOperation
    public void clearTemplates() {
        final List<JdbcTemplate> templateCollection = new ArrayList<>(this.templateMap.values());
        this.templateMap.clear();
        templateCollection.forEach(t ->
          Optional.ofNullable((HikariDataSource)t.getDataSource()).ifPresent(HikariDataSource::close));
    }
}
