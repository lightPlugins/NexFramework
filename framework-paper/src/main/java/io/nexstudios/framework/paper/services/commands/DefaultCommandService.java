package io.nexstudios.framework.paper.services.commands;

import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.paper.services.commands.factory.BrigadierCommandRegistrar;
import io.nexstudios.framework.paper.services.commands.factory.CommandModelBuilder;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultCommandService implements CommandService {

  private final NexFramework core;
  private final ServiceAccessor services;

  private volatile Commands commands;
  private final AtomicLong bindEpoch = new AtomicLong(0L);

  private final List<Object> handlers = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<Object, Long> lastRegisteredEpoch = new ConcurrentHashMap<>();

  public DefaultCommandService(NexFramework core) {
    this.core = Objects.requireNonNull(core, "core");
    this.services = core.services();
  }

  @Override
  public void bind(Commands commands) {
    this.commands = Objects.requireNonNull(commands, "commands");
    long epoch = bindEpoch.incrementAndGet();

    for (Object handler : handlers) {
      registerHandlerIfNeeded(handler, epoch);
    }
  }

  @Override
  public <T extends Service> T register(Class<T> handlerType) {
    Objects.requireNonNull(handlerType, "handlerType");
    T handler = services.create(handlerType);
    handlers.add(handler);

    Commands local = this.commands;
    if (local != null) {
      registerHandlerIfNeeded(handler, bindEpoch.get());
    }
    return handler;
  }

  private void registerHandlerIfNeeded(Object handler, long epoch) {
    Long prev = lastRegisteredEpoch.put(handler, epoch);
    if (prev != null && prev == epoch) {
      return; // already registered in this bind() progress
    }
    registerHandler(handler);
  }

  private void registerHandler(Object handler) {
    var model = CommandModelBuilder.build(core, handler);
    BrigadierCommandRegistrar.register(core, commands, model);
  }
}