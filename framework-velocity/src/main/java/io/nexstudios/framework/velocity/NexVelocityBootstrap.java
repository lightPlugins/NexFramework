package io.nexstudios.framework.velocity;

import io.nexstudios.framework.config.service.language.DefaultLanguageService;
import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.core.service.component.ComponentService;
import io.nexstudios.framework.core.service.component.DefaultComponentService;
import io.nexstudios.framework.core.service.database.DatabaseAsyncService;
import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.database.HibernateEntityRegistryService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.core.service.resource.DefaultResourceService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import io.nexstudios.framework.data.hibernate.DefaultHibernateEntityRegistry;
import io.nexstudios.framework.data.service.DefaultDatabaseAsyncService;
import io.nexstudios.framework.data.service.DefaultDatabaseService;
import io.nexstudios.framework.data.service.language.HibernateUserLocaleStore;
import io.nexstudios.framework.data.service.language.entity.PlayerLocaleEntity;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

import java.nio.file.Path;
import java.util.Objects;

public abstract class NexVelocityBootstrap {

  private final NexFramework core = new NexFramework() {
    @Override
    public String name() {
      return Objects.requireNonNull(NexVelocityBootstrap.this.name(), "name() must not be null");
    }

    @Override
    protected void configureServices(ServiceAccessor services) {
      // Bind-first: register + bind platform-bound services before any dependent services are registered/instantiated
      services.register(DataFolderService.class, DefaultDataFolderService.class);
      services.register(ResourceService.class, DefaultResourceService.class);

      DataFolderService dataFolderService = services.getService(DataFolderService.class);
      dataFolderService.bind(Objects.requireNonNull(NexVelocityBootstrap.this.dataFolder(), "dataFolder() must not be null"));

      ResourceService resourceService = services.getService(ResourceService.class);
      resourceService.bind(Objects.requireNonNull(NexVelocityBootstrap.this.classLoader(), "classLoader() must not be null"));

      // Now it is safe to register services that depend on ResourceService/DataFolderService
      services.register(FileReaderService.class, DefaultFileReaderService.class);
      services.register(MultiFileReaderService.class, DefaultMultiFileReaderService.class);
      services.register(LanguageService.class, DefaultLanguageService.class);
      services.register(ComponentService.class, DefaultComponentService.class);
      services.register(HibernateEntityRegistryService.class, DefaultHibernateEntityRegistry.class);
      services.getService(HibernateEntityRegistryService.class).register(PlayerLocaleEntity.class);
      services.register(DatabaseService.class, DefaultDatabaseService.class);
      services.register(DatabaseAsyncService.class, DefaultDatabaseAsyncService.class);

      services.register(UserLocaleStore.class, HibernateUserLocaleStore.class);

      NexVelocityBootstrap.this.configureServices(services);
    }

    @Override
    protected void start() {
      DatabaseService databaseService = NexVelocityBootstrap.this.services().getService(DatabaseService.class);
      databaseService.start();

      DatabaseAsyncService databaseAsyncService = NexVelocityBootstrap.this.services().getService(DatabaseAsyncService.class);
      databaseAsyncService.start();

      NexVelocityBootstrap.this.start();
    }

    @Override
    protected void stop() {
      try {
        DatabaseAsyncService databaseAsyncService = NexVelocityBootstrap.this.services().getService(DatabaseAsyncService.class);
        databaseAsyncService.shutdown();

        // Plugin stop hook runs while DB is still available (important for final saves)
        NexVelocityBootstrap.this.stop();
      } finally {
        try {
          DatabaseService databaseService = NexVelocityBootstrap.this.services().getService(DatabaseService.class);
          databaseService.shutdown();
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      }
    }
  };

  public abstract String name();

  /**
   * Must be implemented by the Velocity platform module to provide the plugin data directory.
   */
  protected abstract Path dataFolder();

  protected abstract ClassLoader classLoader();

  protected void configureServices(ServiceAccessor services) {
    // default no-op
  }

  protected void start() {
    // default no-op
  }

  protected void stop() {
    // default no-op
  }

  public final void onProxyInitialize() {
    core.boot();
  }

  public final void onProxyShutdown() {
    core.shutdown();
  }

  public final ServiceAccessor services() {
    return core.services();
  }

  public final NexFramework framework() {
    return core;
  }
}