package io.nexstudios.framework.paper.di;

import io.nexstudios.framework.config.service.language.DefaultLanguageService;
import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.core.service.component.ComponentService;
import io.nexstudios.framework.core.service.component.DefaultComponentService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.framework.paper.services.commands.CommandService;
import io.nexstudios.framework.paper.services.commands.DefaultCommandService;
import io.nexstudios.framework.paper.services.thirdparty.DefaultHookService;
import io.nexstudios.framework.paper.services.thirdparty.HookService;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;

import java.util.Objects;

/**
 * Installiert alle "Paper internen" Feature-/Service-Registrierungen in einem Schritt.
 */
public final class PaperInternalServicesModule implements ServiceModule {

  @Override
  public void install(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");

    services.register(CommandService.class, DefaultCommandService.class);
    services.register(FileReaderService.class, DefaultFileReaderService.class);
    services.register(MultiFileReaderService.class, DefaultMultiFileReaderService.class);
    services.register(LanguageService.class, DefaultLanguageService.class);
    services.register(ComponentService.class, DefaultComponentService.class);
    services.register(HookService.class, DefaultHookService.class);
  }
}