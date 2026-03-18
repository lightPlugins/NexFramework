package io.nexstudios.framework.paper.di;

import io.nexstudios.framework.paper.services.thirdparty.DefaultHookService;
import io.nexstudios.framework.paper.services.thirdparty.HookService;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;

import java.util.Objects;

/**
 * Configures and registers default internal services for the application.
 *
 * <p>Ensures required services are bound to their respective implementations.
 */
public final class PaperInternalServicesModule implements ServiceModule {

  @Override
  public void install(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");

    services.register(HookService.class, DefaultHookService.class);
  }
}