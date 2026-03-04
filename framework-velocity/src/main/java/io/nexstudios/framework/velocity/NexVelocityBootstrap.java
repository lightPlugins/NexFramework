package io.nexstudios.framework.velocity;

import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.core.*;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.core.service.resource.DefaultResourceService;
import io.nexstudios.framework.core.service.resource.ResourceService;
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
      services.register(DataFolderService.class, DefaultDataFolderService.class);
      services.register(ResourceService.class, DefaultResourceService.class);

      services.register(FileReaderService.class, DefaultFileReaderService.class);
      services.register(MultiFileReaderService.class, DefaultMultiFileReaderService.class);

      DataFolderService dataFolderService = services.getService(DataFolderService.class);
      dataFolderService.bind(Objects.requireNonNull(NexVelocityBootstrap.this.dataFolder(), "dataFolder() must not be null"));

      ResourceService resourceService = services.getService(ResourceService.class);
      resourceService.bind(Objects.requireNonNull(NexVelocityBootstrap.this.classLoader(), "classLoader() must not be null"));

      NexVelocityBootstrap.this.configureServices(services);
    }

    @Override
    protected void start() {
      NexVelocityBootstrap.this.start();
    }

    @Override
    protected void stop() {
      NexVelocityBootstrap.this.stop();
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