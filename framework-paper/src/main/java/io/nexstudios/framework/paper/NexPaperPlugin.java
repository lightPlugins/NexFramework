package io.nexstudios.framework.paper;

import io.nexstudios.framework.config.service.language.DefaultLanguageService;
import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.core.service.component.ComponentService;
import io.nexstudios.framework.core.service.component.DefaultComponentService;
import io.nexstudios.framework.core.service.database.DatabaseAsyncService;
import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.database.HibernateEntityRegistryService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.core.service.resource.DefaultResourceService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import io.nexstudios.framework.data.hibernate.DefaultHibernateEntityRegistry;
import io.nexstudios.framework.data.service.DefaultDatabaseAsyncService;
import io.nexstudios.framework.data.service.DefaultDatabaseService;
import io.nexstudios.framework.data.service.language.HibernateUserLocaleStore;
import io.nexstudios.framework.data.service.language.entity.PlayerLocaleEntity;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.framework.paper.services.commands.CommandService;
import io.nexstudios.framework.paper.services.commands.DefaultCommandService;
import io.nexstudios.framework.paper.services.locale.events.PaperPlayerLocaleListener;
import io.nexstudios.framework.paper.services.plugin.DefaultPaperPluginService;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class NexPaperPlugin extends JavaPlugin {

  private final List<Listener> pendingListenerInstances = new CopyOnWriteArrayList<>();
  private final List<Class<? extends ServiceListener>> pendingListenerTypes = new CopyOnWriteArrayList<>();
  private final List<Class<? extends Service>> pendingCommandHandlers = new CopyOnWriteArrayList<>();
  private volatile boolean commandsLifecycleHooked = false;

  private final NexFramework core = new NexFramework() {
    @Override
    public String name() {
      return NexPaperPlugin.this.getPluginMeta().getName();
    }

    @Override
    protected void configureServices(ServiceAccessor services) {
      // Bind-first: register + bind platform-bound services before any dependent services are registered/instantiated
      services.register(PaperPluginService.class, DefaultPaperPluginService.class);
      services.register(DataFolderService.class, DefaultDataFolderService.class);
      services.register(ResourceService.class, DefaultResourceService.class);

      DefaultPaperPluginService pluginService = (DefaultPaperPluginService) services.getService(PaperPluginService.class);
      pluginService.bind(NexPaperPlugin.this);

      DataFolderService dataFolderService = services.getService(DataFolderService.class);
      dataFolderService.bind(NexPaperPlugin.this.getDataFolder().toPath());

      ResourceService resourceService = services.getService(ResourceService.class);
      resourceService.bind(NexPaperPlugin.this.getClassLoader());

      // Now it is safe to register services that depend on ResourceService/DataFolderService
      NexPaperPlugin.this.registerPaperInternalServices(services);
      NexPaperPlugin.this.configureServices(services);
    }

    @Override
    protected void start() {
      NexPaperPlugin.this.startInternal();
    }

    @Override
    protected void stop() {
      try {
        DatabaseAsyncService databaseAsyncService = NexPaperPlugin.this.services().getService(DatabaseAsyncService.class);
        databaseAsyncService.shutdown();

        // Plugin stop hook runs while DB is still available (important for final saves)
        NexPaperPlugin.this.stop();
      } finally {
        try {
          DatabaseService databaseService = NexPaperPlugin.this.services().getService(DatabaseService.class);
          databaseService.shutdown();
        } catch (Exception ignored) { }
      }
    }
  };

  protected void registerPaperInternalServices(ServiceAccessor services) {
    services.register(CommandService.class, DefaultCommandService.class);
    services.register(FileReaderService.class, DefaultFileReaderService.class);
    services.register(MultiFileReaderService.class, DefaultMultiFileReaderService.class);
    services.register(LanguageService.class, DefaultLanguageService.class);
    services.register(ComponentService.class, DefaultComponentService.class);
    services.register(HibernateEntityRegistryService.class, DefaultHibernateEntityRegistry.class);
    services.getService(HibernateEntityRegistryService.class).register(PlayerLocaleEntity.class);
    services.register(DatabaseService.class, DefaultDatabaseService.class);
    services.register(DatabaseAsyncService.class, DefaultDatabaseAsyncService.class);
    services.register(UserLocaleStore.class, HibernateUserLocaleStore.class);
  }

  protected void configureServices(ServiceAccessor services) { }
  protected void load() { }
  protected void start() { }
  protected void stop() { }

  protected final void registerListeners(Listener... listeners) {
    var pm = getServer().getPluginManager();

    if (isEnabled()) {
      for (var listener : listeners) {
        pm.registerEvents(listener, this);
      }
      return;
    }

    pendingListenerInstances.addAll(List.of(listeners));
  }

  @SafeVarargs
  protected final void registerListenerTypes(Class<? extends ServiceListener>... listenerTypes) {
    if (isEnabled()) {
      var pm = getServer().getPluginManager();
      for (var type : listenerTypes) {
        Listener listener = services().create(type);
        pm.registerEvents(listener, this);
      }
      return;
    }

    pendingListenerTypes.addAll(List.of(listenerTypes));
  }

  protected List<Class<? extends ServiceListener>> listeners() {
    return List.of();
  }

  protected List<Class<? extends Service>> commands() {
    return List.of();
  }

  @SafeVarargs
  protected final void registerCommands(Class<? extends Service>... handlerTypes) {
    if (framework().isBooted()) {
      CommandService commandService = services().getService(CommandService.class);
      for (var t : handlerTypes) {
        commandService.register(t);
      }
      return;
    }

    pendingCommandHandlers.addAll(List.of(handlerTypes));
  }

  @Override
  public final void onLoad() {
    load();
  }

  @Override
  public final void onEnable() {
    hookCommandsLifecycleOnce();
    core.boot();
  }

  @Override
  public final void onDisable() {
    core.shutdown();
  }

  public final ServiceAccessor services() {
    return core.services();
  }

  public final NexFramework framework() {
    return core;
  }

  private void hookCommandsLifecycleOnce() {
    if (commandsLifecycleHooked) {
      return;
    }
    synchronized (this) {
      if (commandsLifecycleHooked) {
        return;
      }

      getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
        var commands = event.registrar();
        CommandService commandService = services().getService(CommandService.class);
        commandService.bind(commands);
      });

      commandsLifecycleHooked = true;
    }
  }

  private void startInternal() {
    var pm = getServer().getPluginManager();

    registerInternalListener(pm);

    for (var type : listeners()) {
      Listener listener = services().create(type);
      pm.registerEvents(listener, this);
    }

    for (var type : pendingListenerTypes) {
      Listener listener = services().create(type);
      pm.registerEvents(listener, this);
    }

    pendingListenerTypes.clear();

    for (var listener : pendingListenerInstances) {
      pm.registerEvents(listener, this);
    }

    pendingListenerInstances.clear();
    CommandService commandService = services().getService(CommandService.class);

    for (var t : commands()) {
      commandService.register(t);
    }

    for (var t : pendingCommandHandlers) {
      commandService.register(t);
    }

    pendingCommandHandlers.clear();

    DatabaseService databaseService = services().getService(DatabaseService.class);
    databaseService.start();

    DatabaseAsyncService databaseAsyncService = services().getService(DatabaseAsyncService.class);
    databaseAsyncService.start();

    start();
  }

  private void registerInternalListener(PluginManager pluginManager) {
    Listener internalLocaleListener = services().create(PaperPlayerLocaleListener.class);
    pluginManager.registerEvents(internalLocaleListener, this);
  }
}