package io.nexstudios.framework.velocity.di;

import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.core.service.resource.DefaultResourceService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Binds platform-specific data folder and class loader for Velocity services.
 *
 * <p>Installs and binds DataFolderService and ResourceService to enable platform-neutral access.
 */
public final class VelocityPlatformBindingsModule implements ServiceModule {

  private final Path dataFolder;
  private final ClassLoader classLoader;

  /**
   * Initializes a module that binds a data folder and class loader for Velocity services.
   *
   * @param dataFolder the path to the data folder, must not be null.
   * @param classLoader the class loader to bind, must not be null.
   */
  public VelocityPlatformBindingsModule(Path dataFolder, ClassLoader classLoader) {
    this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
  }

  @Override
  public void install(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");

    services.register(DataFolderService.class, DefaultDataFolderService.class);
    services.register(ResourceService.class, DefaultResourceService.class);

    DataFolderService dataFolderService = services.getService(DataFolderService.class);
    dataFolderService.bind(dataFolder);

    ResourceService resourceService = services.getService(ResourceService.class);
    resourceService.bind(classLoader);
  }
}