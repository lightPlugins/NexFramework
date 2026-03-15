package io.nexstudios.framework.paper.di;

import io.nexstudios.framework.paper.services.commands.CommandService;
import io.nexstudios.framework.paper.services.commands.DefaultCommandService;
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

    services.register(CommandService.class, DefaultCommandService.class);
    services.register(HookService.class, DefaultHookService.class);
  }
}