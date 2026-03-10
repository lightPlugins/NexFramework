package io.nexstudios.framework.paper.di;

import io.nexstudios.framework.paper.NexPaperPlugin;
import io.nexstudios.framework.paper.services.plugin.DefaultPaperPluginService;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.core.service.resource.DefaultResourceService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;

import java.util.Objects;

/**
 * Registriert und bindet alle platform-bound Services für Paper (Plugin, DataFolder, Resources).
 */
public final class PaperPlatformBindingsModule implements ServiceModule {

  private final NexPaperPlugin plugin;

  /**
   * @param plugin Paper-Plugin Instanz; darf nicht {@code null} sein.
   */
  public PaperPlatformBindingsModule(NexPaperPlugin plugin) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
  }

  @Override
  public void install(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");

    services.register(PaperPluginService.class, DefaultPaperPluginService.class);
    services.register(DataFolderService.class, DefaultDataFolderService.class);
    services.register(ResourceService.class, DefaultResourceService.class);

    DefaultPaperPluginService pluginService = (DefaultPaperPluginService) services.getService(PaperPluginService.class);
    pluginService.bind(plugin);

    DataFolderService dataFolderService = services.getService(DataFolderService.class);
    dataFolderService.bind(plugin.getDataFolder().toPath());

    ResourceService resourceService = services.getService(ResourceService.class);
    resourceService.bind(plugin.getClass().getClassLoader());
  }
}