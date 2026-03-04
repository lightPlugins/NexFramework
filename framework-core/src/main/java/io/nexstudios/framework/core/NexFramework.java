package io.nexstudios.framework.core;

import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.ServiceRegistry;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceOwner;

import java.util.Objects;

public abstract class NexFramework implements ServiceOwner {

  private final DefaultServiceRegistry registry;
  private final ServiceAccessor services;

  private boolean booted = false;

  protected NexFramework() {
    this.registry = new DefaultServiceRegistry();
    this.services = new ServiceAccessor(registry, this);
  }

  protected void registerInternalServices(ServiceAccessor services) { }
  protected void configureServices(ServiceAccessor services) { }
  protected void start() { }
  protected void stop() { }

  public final synchronized void boot() {
    if (booted) {
      throw new IllegalStateException("NexFramework " + name() + " is already booted!");
    }
    Objects.requireNonNull(name(), "ServiceOwner.name() must not be null!");

    registerInternalServices(services);
    configureServices(services);
    booted = true;
    start();
  }

  public final synchronized void shutdown() {
    if (!booted) {
      return;
    }
    try {
      stop();
    } finally {
      booted = false;
    }
  }

  public final ServiceAccessor services() {
    return services;
  }

  public final ServiceRegistry registry() {
    return registry;
  }

  public final boolean isBooted() {
    return booted;
  }
}