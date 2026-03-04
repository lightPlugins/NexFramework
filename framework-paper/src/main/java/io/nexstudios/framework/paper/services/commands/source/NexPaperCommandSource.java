package io.nexstudios.framework.paper.services.commands.source;

import io.nexstudios.serviceregistry.di.Service;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.jspecify.annotations.Nullable;

public interface NexPaperCommandSource extends Service {

  CommandSender sender();

  @Nullable Entity executor();

  Location location();

  /**
   * Optional: wenn du das Paper-Objekt doch brauchst.
   */
  CommandSourceStack paper();
}