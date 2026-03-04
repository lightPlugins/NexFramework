package io.nexstudios.framework.paper.services.commands.factory;

import com.mojang.brigadier.builder.ArgumentBuilder;
import io.nexstudios.framework.paper.services.commands.factory.util.CommandUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class CommandRequires {

  private CommandRequires() {}

  static <B extends ArgumentBuilder<CommandSourceStack, ?>> B apply(B node, String permission, boolean playerOnly) {
    String perm = CommandUtils.normalizePerm(permission);
    if (perm.isEmpty() && !playerOnly) return node;

    node.requires(src -> {
      CommandSender sender = src.getSender();
      if (playerOnly && !(sender instanceof Player)) return false;
      return perm.isEmpty() || sender.hasPermission(perm);
    });

    return node;
  }
}