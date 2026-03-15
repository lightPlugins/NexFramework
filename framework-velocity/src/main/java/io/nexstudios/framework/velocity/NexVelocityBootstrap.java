package io.nexstudios.framework.velocity;

import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.velocity.di.VelocityInternalServicesModule;
import io.nexstudios.framework.velocity.di.VelocityPlatformBindingsModule;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Base class for initializing and managing a Velocity plugin using NexFramework.
 */
public abstract class NexVelocityBootstrap {

  /**
   * Creates a new bootstrap instance.
   *
   * <p>The constructor is {@code protected} because this class is intended to be subclassed.</p>
   */
  protected NexVelocityBootstrap() {
  }

  private final NexFramework core = new NexFramework() {
    /**
     * Retrieves the name of the framework or bootstrap implementation.
     *
     * @return a non-null name string.
     */
    @Override
    public String name() {
      return Objects.requireNonNull(NexVelocityBootstrap.this.name(), "name() must not be null");
    }

    /**
     * Configures and registers services necessary for the application.
     *
     * @param services the service accessor for managing and registering dependencies.
     */
    @Override
    protected void configureServices(ServiceAccessor services) {
      // Bind-first: register + bind platform-bound services before any dependent services are registered/instantiated
      services.install(new VelocityPlatformBindingsModule(
          Objects.requireNonNull(NexVelocityBootstrap.this.dataFolder(), "dataFolder() must not be null"),
          Objects.requireNonNull(NexVelocityBootstrap.this.classLoader(), "classLoader() must not be null")
      ));

      // Now it is safe to register services that depend on ResourceService/DataFolderService
      services.install(new VelocityInternalServicesModule());

      NexVelocityBootstrap.this.configureServices(services);
    }

    /**
     * Starts database services and initializes the application.
     */
    @Override
    protected void start() {
      NexVelocityBootstrap.this.start();
    }

    /**
     * Stops the plugin and shuts down database services.
     * <p>
     * Ensures both async and sync database services are properly shut down.
     * Logs exceptions during shutdown if they occur.
     */
    @Override
    protected void stop() {
      NexVelocityBootstrap.this.stop();
    }
  };

  /**
   * Retrieves the name associated with the implementation.
   *
   * @return a non-null name string.
   */
  public abstract String name();

  /**
   * Provides the path to the data folder.
   *
   * @return the path to the data folder, must not be null.
   */
  protected abstract Path dataFolder();

  /**
   * Provides the ClassLoader for the plugin.
   *
   * @return the ClassLoader instance, must not be null.
   */
  protected abstract ClassLoader classLoader();

  /**
   * Allows customization of service registration or configuration.
   *
   * @param services the service accessor for managing services.
   */
  protected void configureServices(ServiceAccessor services) {
    // default no-op
  }

  /**
   * Starts the application or service.
   * <p>
   * Intended for optional setup or initialization during startup.
   */
  protected void start() {
    // default no-op
  }

  /**
   * Stops the application or service.
   */
  protected void stop() {
    // default no-op
  }

  /**
   * Initializes and starts the core framework for the proxy.
   *
   * @throws IllegalStateException if the framework is already booted.
   */
  public final void onProxyInitialize() {
    core.boot();
  }

  /**
   * Handles necessary operations during proxy shutdown.
   */
  public final void onProxyShutdown() {
    core.shutdown();
  }

  /**
   * Provides access to registered services.
   *
   * @return the service accessor instance, never null.
   */
  public final ServiceAccessor services() {
    return core.services();
  }

  /**
   * Returns the core framework instance.
   *
   * @return the framework instance, never null.
   */
  public final NexFramework framework() {
    return core;
  }
}