package io.nexstudios.framework.core.service.resource;

import io.nexstudios.serviceregistry.di.Service;

import java.io.InputStream;
import java.util.Optional;

/**
 * Platform-neutral access to classpath resources (e.g. default config files).
 *
 * The platform module (Paper/Velocity) should bind a suitable ClassLoader.
 */
public interface ResourceService extends Service {

  /**
   * Returns a resource stream if the resource exists.
   *
   * @param resourcePath path within the classpath (e.g. "config.yml" or "configs/example.yml")
   * @return optional input stream
   */
  Optional<InputStream> openResource(String resourcePath);

  /**
   * Exposes the bound class loader.
   *
   * @return bound class loader
   * @throws IllegalStateException if not bound
   */
  ClassLoader classLoader();

  /**
   * Binds the class loader that should be used to resolve resources.
   *
   * @param classLoader class loader to use
   */
  void bind(ClassLoader classLoader);
}