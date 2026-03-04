package io.nexstudios.framework.core.service.resource;

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * Default ResourceService implementation backed by a bound ClassLoader.
 */
public final class DefaultResourceService implements ResourceService {

  private volatile ClassLoader classLoader;

  @Override
  public Optional<InputStream> openResource(String resourcePath) {
    Objects.requireNonNull(resourcePath, "resourcePath");
    ClassLoader cl = classLoader;
    if (cl == null) {
      throw new IllegalStateException("ResourceService is not bound. The platform must call bind(ClassLoader) first.");
    }
    return Optional.ofNullable(cl.getResourceAsStream(resourcePath));
  }

  @Override
  public void bind(ClassLoader classLoader) {
    this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
  }
}