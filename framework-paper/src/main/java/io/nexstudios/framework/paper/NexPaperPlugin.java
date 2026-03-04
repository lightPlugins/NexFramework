package io.nexstudios.framework.paper;

import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.folder.DefaultDataFolderService;
import io.nexstudios.framework.core.service.resource.DefaultResourceService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.framework.paper.services.commands.CommandService;
import io.nexstudios.framework.paper.services.commands.DefaultCommandService;
import io.nexstudios.framework.paper.services.plugin.DefaultPaperPluginService;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.event.Listener;
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
      NexPaperPlugin.this.registerPaperInternalServices(services);

      DefaultPaperPluginService pluginService = (DefaultPaperPluginService) services.getService(PaperPluginService.class);
      pluginService.bind(NexPaperPlugin.this);

      DataFolderService dataFolderService = services.getService(DataFolderService.class);
      dataFolderService.bind(NexPaperPlugin.this.getDataFolder().toPath());

      ResourceService resourceService = services.getService(ResourceService.class);
      resourceService.bind(NexPaperPlugin.this.getClassLoader());

      NexPaperPlugin.this.configureServices(services);
    }

    @Override
    protected void start() {
      NexPaperPlugin.this.startInternal();
    }

    @Override
    protected void stop() {
      NexPaperPlugin.this.stop();
    }
  };

  protected void registerPaperInternalServices(ServiceAccessor services) {
    services.register(CommandService.class, DefaultCommandService.class);
    services.register(DataFolderService.class, DefaultDataFolderService.class);
    services.register(ResourceService.class, DefaultResourceService.class);
    services.register(FileReaderService.class, DefaultFileReaderService.class);
    services.register(MultiFileReaderService.class, DefaultMultiFileReaderService.class);
    services.register(PaperPluginService.class, DefaultPaperPluginService.class);
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
    start();
  }
}