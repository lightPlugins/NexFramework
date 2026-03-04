package io.nexstudios.framework.paper.services.commands;

import io.nexstudios.serviceregistry.di.Service;
import io.papermc.paper.command.brigadier.Commands;

public interface CommandService extends Service {

  void bind(Commands commands);

  <T extends Service> T register(Class<T> handlerType);
}