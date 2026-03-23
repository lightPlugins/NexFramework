package io.nexstudios.framework.core;

import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.ServiceRegistry;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceOwner;

import java.util.Objects;

public abstract class NexFramework implements ServiceOwner {

  private final DefaultServiceRegistry registry;
  private final ServiceAccessor services;

  private enum State {
    NEW,
    PRELOADED,
    BOOTED
  }

  private State state = State.NEW;

  protected NexFramework() {
    this.registry = new DefaultServiceRegistry();
    this.services = new ServiceAccessor(registry, this);
  }

  protected void registerInternalServices(ServiceAccessor services) { }
  protected void configureServices(ServiceAccessor services) { }
  protected void start() { }
  protected void stop() { }

  /**
   * Runs service registration + configuration without starting runtime logic.
   * Intended for early lifecycle stages (e.g. Paper onLoad()).
   */
  public final synchronized void preload() {
    if (state == State.BOOTED) {
      throw new IllegalStateException("NexFramework " + name() + " is already booted!");
    }
    if (state == State.PRELOADED) {
      return; // idempotent
    }

    Objects.requireNonNull(name(), "ServiceOwner.name() must not be null!");

    registerInternalServices(services);
    configureServices(services);
    state = State.PRELOADED;
  }

  public final synchronized void boot() {
    if (state == State.BOOTED) {
      throw new IllegalStateException("NexFramework " + name() + " is already booted!");
    }

    // Ensure services are available even if preload() was not called explicitly
    if (state == State.NEW) {
      preload();
    }

    state = State.BOOTED;
    start();
  }

  public final synchronized void shutdown() {
    if (state != State.BOOTED) {
      return;
    }
    try {
      stop();
    } finally {
      state = State.NEW;
    }
  }

  public final ServiceAccessor services() {
    return services;
  }

  public final ServiceRegistry registry() {
    return registry;
  }

  public final boolean isBooted() {
    return state == State.BOOTED;
  }

  public final boolean isPreloaded() {
    return state == State.PRELOADED;
  }
}