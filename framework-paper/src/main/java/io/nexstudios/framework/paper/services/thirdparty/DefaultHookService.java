package io.nexstudios.framework.paper.services.thirdparty;

import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.Optional;

public final class DefaultHookService implements HookService {

  private final ServiceAccessor services;

  public DefaultHookService(ServiceAccessor services) {
    this.services = Objects.requireNonNull(services, "services");
  }

  @Override
  public boolean isPluginEnabled(String pluginName) {
    Objects.requireNonNull(pluginName, "pluginName");
    return Bukkit.getPluginManager().isPluginEnabled(pluginName);
  }

  @Override
  public <T extends Service> boolean isServiceAvailable(Class<T> serviceType) {
    return findService(serviceType).isPresent();
  }

  @Override
  public <T extends Service> Optional<T> findService(Class<T> serviceType) {
    Objects.requireNonNull(serviceType, "serviceType");
    try {
      return Optional.ofNullable(services.getService(serviceType));
    } catch (RuntimeException notRegisteredOrNotResolvable) {
      return Optional.empty();
    }
  }
}