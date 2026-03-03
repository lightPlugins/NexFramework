package io.nexstudios.framework.velocity;

import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

import java.util.Objects;

public abstract class NexVelocityBootstrap {

  private final NexFramework core = new NexFramework() {
    @Override
    public String name() {
      return Objects.requireNonNull(NexVelocityBootstrap.this.name(), "name() darf nicht null sein");
    }

    @Override
    protected void configureServices(ServiceAccessor services) {
      NexVelocityBootstrap.this.configureServices(services);
    }

    @Override
    protected void start() {
      NexVelocityBootstrap.this.start();
    }

    @Override
    protected void stop() {
      NexVelocityBootstrap.this.stop();
    }
  };

  public abstract String name();

  protected void configureServices(ServiceAccessor services) {
    // default no-op
  }

  protected void start() {
    // default no-op
  }

  protected void stop() {
    // default no-op
  }

  public final void onProxyInitialize() {
    core.boot();
  }

  public final void onProxyShutdown() {
    core.shutdown();
  }

  public final ServiceAccessor services() {
    return core.services();
  }

  public final NexFramework framework() {
    return core;
  }
}