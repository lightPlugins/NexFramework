package io.nexstudios.framework.data.service;

import io.nexstudios.framework.config.FileConfiguration;
import io.nexstudios.framework.config.YamlFileConfiguration;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.database.HibernateEntityRegistryService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.data.hibernate.DefaultHibernateEntityRegistry;
import io.nexstudios.framework.data.service.language.entity.PlayerLocaleEntity;
import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceOwner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.persistence.EntityManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultDatabaseServiceSqliteIT {

  @TempDir
  Path dataDir;

  /**
   * Minimal FileReaderService for tests:
   * writes a provided YAML into the plugin data folder and returns a YamlFileConfiguration.
   */
  public static final class TestFileReaderService implements FileReaderService {

    private final DataFolderService dataFolderService;
    private final String yaml;

    public TestFileReaderService(ServiceAccessor services) {
      Objects.requireNonNull(services, "services");
      this.dataFolderService = services.getService(DataFolderService.class);

      // A minimal config that still lets Hibernate boot for SQLite.
      // Important: Hibernate needs an explicit SQLite dialect (community dialects).
      this.yaml = """
          database:
            type: "sqlite"
            file: "database.sqlite"
            useSSL: false
            jdbcParameters: ""
            advanced:
              poolName: "it-hikari"
              maximumPoolSize: 2
              minimumIdle: 1
              connectionTimeoutMillis: 30000
              idleTimeoutMillis: 600000
              maxLifetimeMillis: 0
          hibernate:
            hbm2ddl:
              auto: "none"
            additionalHibernateProperties:
              hibernate.dialect: "org.hibernate.community.dialect.SQLiteDialect"
          """;
    }

    @Override
    public FileConfiguration load(Path relativePath, String resourcePath, boolean loadDefaults) {
      Objects.requireNonNull(relativePath, "relativePath");

      Path target = dataFolderService.getDataFolder().resolve(relativePath).normalize();
      try {
        if (target.getParent() != null) {
          Files.createDirectories(target.getParent());
        }
        Files.writeString(target, yaml, StandardCharsets.UTF_8);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to write test database.yml to " + target, e);
      }

      return new YamlFileConfiguration(target, false);
    }
  }

  private ServiceAccessor newServices() {
    ServiceOwner owner = new ServiceOwner() {
      @Override
      public String name() {
        return "DefaultDatabaseServiceSqliteIT";
      }
    };

    var registry = new DefaultServiceRegistry();
    var services = new ServiceAccessor(registry, owner);

    services.register(DataFolderService.class, DefaultDataFolderService.class);
    DataFolderService dfs = services.getService(DataFolderService.class);
    dfs.bind(dataDir);

    services.register(FileReaderService.class, TestFileReaderService.class);

    services.register(HibernateEntityRegistryService.class, DefaultHibernateEntityRegistry.class);
    HibernateEntityRegistryService reg = services.getService(HibernateEntityRegistryService.class);
    reg.register(PlayerLocaleEntity.class);

    services.register(DatabaseService.class, DefaultDatabaseService.class);

    return services;
  }

  @Test
  void start_bootstrapsSqlite_andEntityManagerFactoryWorks_andCreatesDbFile() {
    ServiceAccessor services = newServices();
    DatabaseService db = services.getService(DatabaseService.class);

    assertDoesNotThrow(db::start);

    assertNotNull(db.entityManagerFactory());

    try (EntityManager em = db.entityManagerFactory().createEntityManager()) {
      assertNotNull(em);
    }

    Path dbFile = dataDir.resolve("database.sqlite");
    assertTrue(Files.exists(dbFile), "Expected SQLite database file to exist: " + dbFile);

    assertDoesNotThrow(db::shutdown);
  }

  @Test
  void start_and_shutdown_are_idempotent() {
    ServiceAccessor services = newServices();
    DatabaseService db = services.getService(DatabaseService.class);

    db.start();
    db.start(); // idempotent

    db.shutdown();
    db.shutdown(); // idempotent

    assertThrows(IllegalStateException.class, db::entityManagerFactory);
  }
}