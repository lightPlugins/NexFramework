package io.nexstudios.framework.data.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.nexstudios.framework.config.FileConfiguration;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.data.configuration.DatabaseConfiguration;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Dependencies({
    FileReaderService.class,
    DataFolderService.class
})
public final class DefaultDatabaseService implements DatabaseService {

  private static final Path DATABASE_CONFIGURATION_PATH = Path.of("database.yml");
  private static final String DATABASE_CONFIGURATION_RESOURCE = "database.yml";

  private final FileReaderService fileReaderService;
  private final DataFolderService dataFolderService;

  private final Object lifecycleLock = new Object();

  private volatile boolean started = false;

  private volatile DatabaseConfiguration databaseConfiguration;
  private volatile HikariDataSource hikariDataSource;
  private volatile EntityManagerFactory entityManagerFactory;

  public DefaultDatabaseService(ServiceAccessor serviceAccessor) {
    Objects.requireNonNull(serviceAccessor, "serviceAccessor");
    this.fileReaderService = serviceAccessor.getService(FileReaderService.class);
    this.dataFolderService = serviceAccessor.getService(DataFolderService.class);
  }

  @Override
  public void start() {
    if (started) {
      return;
    }

    synchronized (lifecycleLock) {
      if (started) {
        return;
      }

      FileConfiguration fileConfiguration = fileReaderService.load(
          DATABASE_CONFIGURATION_PATH,
          DATABASE_CONFIGURATION_RESOURCE,
          true
      );

      Path dataFolder = dataFolderService.getDataFolder();
      DatabaseConfiguration loadedConfiguration = readAndValidateConfiguration(fileConfiguration, dataFolder);

      HikariDataSource createdDataSource = createHikariDataSource(loadedConfiguration);
      EntityManagerFactory createdEntityManagerFactory = createEntityManagerFactory(loadedConfiguration, createdDataSource);

      this.databaseConfiguration = loadedConfiguration;
      this.hikariDataSource = createdDataSource;
      this.entityManagerFactory = createdEntityManagerFactory;
      this.started = true;
    }
  }

  @Override
  public void shutdown() {
    synchronized (lifecycleLock) {
      if (!started) {
        return;
      }

      EntityManagerFactory localEntityManagerFactory = this.entityManagerFactory;
      this.entityManagerFactory = null;

      HikariDataSource localHikariDataSource = this.hikariDataSource;
      this.hikariDataSource = null;

      this.databaseConfiguration = null;
      this.started = false;

      // Close Hibernate/JPA first (it may still reference connections)
      if (localEntityManagerFactory != null) {
        try {
          localEntityManagerFactory.close();
        } catch (RuntimeException exception) {
          // best-effort close; nothing we can reliably do here
        }
      }

      if (localHikariDataSource != null) {
        try {
          localHikariDataSource.close();
        } catch (RuntimeException exception) {
          // best-effort close
        }
      }
    }
  }

  @Override
  public EntityManagerFactory entityManagerFactory() {
    EntityManagerFactory localEntityManagerFactory = entityManagerFactory;
    if (localEntityManagerFactory == null) {
      throw new IllegalStateException(
          "DatabaseService is not started yet. Ensure DatabaseService.start() is called during platform startup."
      );
    }
    return localEntityManagerFactory;
  }

  private static DatabaseConfiguration readAndValidateConfiguration(FileConfiguration fileConfiguration, Path dataFolder) {
    Objects.requireNonNull(fileConfiguration, "fileConfiguration");
    Objects.requireNonNull(dataFolder, "dataFolder");

    String type = fileConfiguration.getString("database.type", "sqlite");
    if (type == null || type.isBlank()) {
      type = "sqlite";
    }
    type = type.trim().toLowerCase(java.util.Locale.ROOT);

    String poolName = fileConfiguration.getString("database.advanced.poolName", "nexframework-hikari");

    int maximumPoolSize = fileConfiguration.getInt("database.advanced.maximumPoolSize", -1);
    int minimumIdle = fileConfiguration.getInt("database.advanced.minimumIdle", -1);

    long connectionTimeoutMillis = readLongOrDefault(fileConfiguration, "database.advanced.connectionTimeoutMillis", 30_000L);
    long idleTimeoutMillis = readLongOrDefault(fileConfiguration, "database.advanced.idleTimeoutMillis", 600_000L);
    long maxLifetimeMillis = readLongOrDefault(fileConfiguration, "database.advanced.maxLifetimeMillis", 1_800_000L);

    String hbm2ddlAuto = fileConfiguration.getString("hibernate.hbm2ddl.auto", "validate");
    if (hbm2ddlAuto == null || hbm2ddlAuto.isBlank()) {
      throw new IllegalStateException("Invalid configuration: 'hibernate.hbm2ddl.auto' must not be blank.");
    }

    Map<String, String> additionalHibernateProperties =
        readStringMap(fileConfiguration);

    String jdbcParameters = fileConfiguration.getString("database.jdbcParameters", "");
    if (jdbcParameters == null) {
      jdbcParameters = "";
    }
    jdbcParameters = jdbcParameters.trim();

    String jdbcUrl;
    String host = fileConfiguration.getString("database.host", "127.0.0.1");
    int port = fileConfiguration.getInt("database.port", 3306);
    String database = fileConfiguration.getString("database.name", "nexframework");
    String username = fileConfiguration.getString("database.username", "");
    String password = fileConfiguration.getString("database.password", "");
    boolean useSSL = fileConfiguration.getBoolean("database.useSSL", false);

    String file = fileConfiguration.getString("database.file", "database.sqlite");

    switch (type) {
      case "sqlite" -> {
        if (file == null || file.isBlank()) {
          throw new IllegalStateException("Invalid configuration: 'database.file' must not be blank for sqlite.");
        }

        Path sqlitePath = dataFolder.resolve(file).normalize();
        jdbcUrl = "jdbc:sqlite:" + sqlitePath.toAbsolutePath();

        // SQLite doesn't use username/password
        username = "";
        password = "";

        // Sensible defaults for SQLite if not explicitly set
        if (maximumPoolSize <= 0) maximumPoolSize = 2;
        if (minimumIdle < 0) minimumIdle = 1;
        if (maxLifetimeMillis <= 0) maxLifetimeMillis = 0L; // often better disabled for SQLite
      }
      case "mariadb", "mysql" -> {
        if (host == null || host.isBlank()) {
          throw new IllegalStateException("Invalid configuration: 'database.host' must not be blank for " + type + ".");
        }
        if (database == null || database.isBlank()) {
          throw new IllegalStateException("Invalid configuration: 'database.name' must not be blank for " + type + ".");
        }
        if (port <= 0 || port > 65535) {
          throw new IllegalStateException("Invalid configuration: 'database.port' must be between 1 and 65535.");
        }
        if (username == null || username.isBlank()) {
          throw new IllegalStateException("Invalid configuration: 'database.username' must not be blank for " + type + ".");
        }
        if (password == null) {
          throw new IllegalStateException("Invalid configuration: 'database.password' must not be null.");
        }

        String base = "jdbc:" + type + "://" + host + ":" + port + "/" + database;

        String params = "useSSL=" + (useSSL ? "true" : "false");
        if (!jdbcParameters.isEmpty()) {
          params = params + "&" + jdbcParameters;
        }

        jdbcUrl = base + "?" + params;

        // Pool defaults if not explicitly set
        if (maximumPoolSize <= 0) maximumPoolSize = 10;
        if (minimumIdle < 0) minimumIdle = 2;
      }
      default -> throw new IllegalStateException(
          "Invalid configuration: unsupported database.type '" + type + "'. Supported: sqlite, mariadb, mysql."
      );
    }

    if (minimumIdle > maximumPoolSize) {
      throw new IllegalStateException(
          "Invalid configuration: 'database.advanced.minimumIdle' (" + minimumIdle + ") must be <= " +
              "'database.advanced.maximumPoolSize' (" + maximumPoolSize + ")."
      );
    }

    return DatabaseConfiguration.builder()
        .type(type)
        .file(file)
        .host(host)
        .port(port)
        .database(database)
        .username(username)
        .password(password)
        .useSSL(useSSL)
        .jdbcParameters(jdbcParameters)
        .jdbcUrl(jdbcUrl)
        .poolName(poolName)
        .maximumPoolSize(maximumPoolSize)
        .minimumIdle(minimumIdle)
        .connectionTimeoutMillis(connectionTimeoutMillis)
        .idleTimeoutMillis(idleTimeoutMillis)
        .maxLifetimeMillis(maxLifetimeMillis)
        .additionalHibernateProperties(additionalHibernateProperties)
        .hbm2ddlAuto(hbm2ddlAuto)
        .build();
  }

  private static long readLongOrDefault(FileConfiguration fileConfiguration, String path, long def) {
    Object rawValue = fileConfiguration.node().node((Object[]) path.split("\\.")).raw();
    if (rawValue == null) {
      return def;
    }
    return parseLongOrThrow(path, rawValue);
  }

  private static HikariDataSource createHikariDataSource(DatabaseConfiguration databaseConfiguration) {
    Objects.requireNonNull(databaseConfiguration, "databaseConfiguration");

    HikariConfig hikariConfig = new HikariConfig();

    hikariConfig.setJdbcUrl(databaseConfiguration.getJdbcUrl());
    hikariConfig.setUsername(databaseConfiguration.getUsername());
    hikariConfig.setPassword(databaseConfiguration.getPassword());

    String type = databaseConfiguration.getType();
    ClassLoader pluginLoader = DefaultDatabaseService.class.getClassLoader();

    switch (type) {
      case "mariadb" -> {
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        try {
          Class.forName("org.mariadb.jdbc.Driver", true, pluginLoader);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(
              "MariaDB JDBC Driver not found. Ensure 'org.mariadb.jdbc:mariadb-java-client' is available to the plugin.",
              e
          );
        }
      }
      case "mysql" -> {
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        try {
          Class.forName("com.mysql.cj.jdbc.Driver", true, pluginLoader);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(
              "MySQL JDBC Driver not found. Ensure 'com.mysql:mysql-connector-j' is available to the plugin.",
              e
          );
        }
      }
      case "sqlite" -> {
        // SQLite file path is already baked into jdbcUrl (jdbc:sqlite:/absolute/path/to/file.sqlite)
        hikariConfig.setDriverClassName("org.sqlite.JDBC");
        try {
          Class.forName("org.sqlite.JDBC", true, pluginLoader);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException(
              "SQLite JDBC Driver not found. Ensure 'org.xerial:sqlite-jdbc' is available to the plugin.",
              e
          );
        }
      }
      case null, default -> throw new IllegalStateException(
          "Unsupported database.type '" + type + "'. Supported: sqlite, mariadb, mysql."
      );
    }

    hikariConfig.setPoolName(databaseConfiguration.getPoolName());
    hikariConfig.setMaximumPoolSize(databaseConfiguration.getMaximumPoolSize());
    hikariConfig.setMinimumIdle(databaseConfiguration.getMinimumIdle());

    hikariConfig.setConnectionTimeout(databaseConfiguration.getConnectionTimeoutMillis());
    hikariConfig.setIdleTimeout(databaseConfiguration.getIdleTimeoutMillis());
    hikariConfig.setMaxLifetime(databaseConfiguration.getMaxLifetimeMillis());

    // Conservative, performant defaults (driver-level)
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
    hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");

    hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
    hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
    hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
    hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
    hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");

    hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

    // Extra robust: set TCCL to plugin classloader during pool init
    ClassLoader prev = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(pluginLoader);
      return new HikariDataSource(hikariConfig);
    } finally {
      Thread.currentThread().setContextClassLoader(prev);
    }
  }

  private static EntityManagerFactory createEntityManagerFactory(
      DatabaseConfiguration databaseConfiguration,
      HikariDataSource hikariDataSource
  ) {
    Objects.requireNonNull(databaseConfiguration, "databaseConfiguration");
    Objects.requireNonNull(hikariDataSource, "hikariDataSource");

    Map<String, Object> hibernateProperties = new LinkedHashMap<>();

    // Important: do NOT use hibernate.hikari.* (we manage Hikari ourselves)
    hibernateProperties.put(AvailableSettings.JAKARTA_JTA_DATASOURCE, hikariDataSource);

    // Performance/sanity defaults
    hibernateProperties.put(AvailableSettings.SHOW_SQL, "false");
    hibernateProperties.put(AvailableSettings.FORMAT_SQL, "false");
    hibernateProperties.put(AvailableSettings.HBM2DDL_AUTO, databaseConfiguration.getHbm2ddlAuto());

    // Reasonable batching defaults (safe baseline; can be overridden via additionalHibernateProperties)
    hibernateProperties.put(AvailableSettings.STATEMENT_BATCH_SIZE, "25");
    hibernateProperties.put(AvailableSettings.ORDER_INSERTS, "true");
    hibernateProperties.put(AvailableSettings.ORDER_UPDATES, "true");

    // User overrides / additions
    hibernateProperties.putAll(databaseConfiguration.getAdditionalHibernateProperties());

    StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
        .applySettings(hibernateProperties)
        .build();

    try {
      // Note: This does NOT auto-register annotated entities.
      // You can either:
      // - rely on integrators / your own bootstrap layer later
      // - or extend this service with explicit entity registration
      MetadataSources metadataSources = new MetadataSources(serviceRegistry);

      return metadataSources
          .buildMetadata()
          .buildSessionFactory();
    } catch (RuntimeException exception) {
      try {
        StandardServiceRegistryBuilder.destroy(serviceRegistry);
      } catch (RuntimeException destroyException) {
        exception.addSuppressed(destroyException);
      }
      throw exception;
    }
  }

  private static String requireNonBlankString(FileConfiguration fileConfiguration, String path) {
    String value = fileConfiguration.getString(path, null);
    if (value == null) {
      throw new IllegalStateException("Missing required configuration key: '" + path + "'.");
    }
    if (value.isBlank()) {
      throw new IllegalStateException("Invalid configuration: '" + path + "' must not be blank.");
    }
    return value;
  }

  private static String requireNonNullString(FileConfiguration fileConfiguration, String path) {
    String value = fileConfiguration.getString(path, null);
    if (value == null) {
      throw new IllegalStateException("Missing required configuration key: '" + path + "'.");
    }
    return value;
  }

  private static int requirePositiveInt(FileConfiguration fileConfiguration, String path) {
    int value = fileConfiguration.getInt(path, Integer.MIN_VALUE);
    if (value == Integer.MIN_VALUE) {
      throw new IllegalStateException("Missing required integer configuration key: '" + path + "'.");
    }
    if (value <= 0) {
      throw new IllegalStateException("Invalid configuration: '" + path + "' must be > 0, but was " + value + ".");
    }
    return value;
  }

  private static int requireNonNegativeInt(FileConfiguration fileConfiguration, String path) {
    int value = fileConfiguration.getInt(path, Integer.MIN_VALUE);
    if (value == Integer.MIN_VALUE) {
      throw new IllegalStateException("Missing required integer configuration key: '" + path + "'.");
    }
    if (value < 0) {
      throw new IllegalStateException("Invalid configuration: '" + path + "' must be >= 0, but was " + value + ".");
    }
    return value;
  }

  private static long requirePositiveLong(FileConfiguration fileConfiguration, String path) {
    Objects.requireNonNull(fileConfiguration, "fileConfiguration");
    Objects.requireNonNull(path, "path");

    Object rawValue = fileConfiguration.node().node((Object[]) path.split("\\.")).raw();
    if (rawValue == null) {
      throw new IllegalStateException("Missing required long configuration key: '" + path + "'.");
    }

    long value = parseLongOrThrow(path, rawValue);

    if (value <= 0L) {
      throw new IllegalStateException("Invalid configuration: '" + path + "' must be > 0, but was " + value + ".");
    }
    return value;
  }

  private static long parseLongOrThrow(String path, Object rawValue) {
    if (rawValue instanceof Long longValue) {
      return longValue;
    }
    if (rawValue instanceof Integer integerValue) {
      return integerValue.longValue();
    }
    if (rawValue instanceof Double doubleValue) {
      return (long) Math.floor(doubleValue);
    }
    if (rawValue instanceof String stringValue) {
      String normalized = stringValue.trim();
      if (normalized.isEmpty()) {
        throw new IllegalStateException("Invalid configuration: '" + path + "' must not be blank.");
      }
      try {
        return Long.parseLong(normalized);
      } catch (NumberFormatException exception) {
        throw new IllegalStateException("Invalid configuration: '" + path + "' must be a number, but was '" + stringValue + "'.");
      }
    }

    throw new IllegalStateException(
        "Invalid configuration: '" + path + "' must be a number, but was " + rawValue.getClass().getName() + "."
    );
  }

  private static Map<String, String> readStringMap(FileConfiguration fileConfiguration) {
    if (fileConfiguration.node().node((Object[]) "hibernate.additionalHibernateProperties".split("\\.")).virtual()) {
      return Map.of();
    }

    var node = fileConfiguration.node().node((Object[]) "hibernate.additionalHibernateProperties".split("\\."));
    if (!node.isMap()) {
      throw new IllegalStateException("Invalid configuration: '" + "hibernate.additionalHibernateProperties" + "' must be a map of string->string.");
    }

    Map<String, String> out = new LinkedHashMap<>();
    for (var entry : node.childrenMap().entrySet()) {
      String key = String.valueOf(entry.getKey());
      Object rawValue = entry.getValue().raw();
      if (rawValue == null) {
        continue;
      }
      if (!(rawValue instanceof String stringValue)) {
        throw new IllegalStateException(
            "Invalid configuration: '" + "hibernate.additionalHibernateProperties" + "." + key + "' must be a string, but was " + rawValue.getClass().getName() + "."
        );
      }
      out.put(key, stringValue);
    }
    return Map.copyOf(out);
  }
}