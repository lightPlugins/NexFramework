package io.nexstudios.framework.paper;

import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class NexPaperPlugin extends JavaPlugin {

  private final NexFramework core = new NexFramework() {
    @Override
    public String name() {
      return NexPaperPlugin.this.getDescription().getName();
    }

    @Override
    protected void configureServices(ServiceAccessor services) {
      NexPaperPlugin.this.configureServices(services);
    }

    @Override
    protected void start() {
      NexPaperPlugin.this.start();
    }

    @Override
    protected void stop() {
      NexPaperPlugin.this.stop();
    }
  };

  protected void configureServices(ServiceAccessor services) {
    // default no-op
  }

  protected void start() {
    // default no-op
  }

  protected void stop() {
    // default no-op
  }

  @Override
  public final void onEnable() {
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
}