package io.nexstudios.framework.velocity.di;

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

  }
}