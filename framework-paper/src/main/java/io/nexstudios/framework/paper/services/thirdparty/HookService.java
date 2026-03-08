package io.nexstudios.framework.paper.services.thirdparty;

import io.nexstudios.serviceregistry.di.Service;

import java.util.Optional;

public interface HookService extends Service {

  /**
   * Checks if a Bukkit/Paper plugin is present and currently enabled.
   */
  boolean isPluginEnabled(String pluginName);

  /**
   * Checks if a service can be resolved from the service registry.
   */
  <T extends Service> boolean isServiceAvailable(Class<T> serviceType);

  /**
   * Attempts to resolve a service from the registry.
   * Returns Optional.empty() if not registered / not resolvable.
   */
  <T extends Service> Optional<T> findService(Class<T> serviceType);
}