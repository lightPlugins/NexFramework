package io.nexstudios.framework.velocity.di;

import io.nexstudios.framework.config.service.language.DefaultLanguageService;
import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.core.service.component.ComponentService;
import io.nexstudios.framework.core.service.component.DefaultComponentService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;

import java.util.Objects;

/**
 * Configures and registers internal services for the Velocity application.
 *
 * <p>Registers services related to file reading, languages, components,
 * database operations, and user locale storage.
 */
public final class VelocityInternalServicesModule implements ServiceModule {

  /**
   * Configures internal services for Velocity.
   * <p>
   * Registers file handling, language, component, database, and user locale services.
   */
  public VelocityInternalServicesModule() {
  }

  @Override
  public void install(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");

    services.register(FileReaderService.class, DefaultFileReaderService.class);
    services.register(MultiFileReaderService.class, DefaultMultiFileReaderService.class);

    services.register(LanguageService.class, DefaultLanguageService.class);
    services.register(ComponentService.class, DefaultComponentService.class);
  }
}