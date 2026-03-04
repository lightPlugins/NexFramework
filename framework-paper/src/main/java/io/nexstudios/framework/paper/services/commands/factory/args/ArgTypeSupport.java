package io.nexstudios.framework.paper.services.commands.factory.args;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.paper.services.commands.factory.model.ArgSpec;
import io.nexstudios.framework.paper.services.commands.factory.model.CmdNode;
import io.nexstudios.framework.paper.services.commands.factory.suggest.SuggestionProvider;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import static io.papermc.paper.command.brigadier.Commands.argument;

public final class ArgTypeSupport {

  private ArgTypeSupport() {}

  public static ArgumentBuilder<CommandSourceStack, ?> buildArgumentBuilder(NexFramework core, CmdNode node) {
    ArgSpec spec = java.util.Objects.requireNonNull(node.argSpec, "argSpec");

    SuggestionProvider provider = (spec.suggestClass == null) ? null : core.services().create(spec.suggestClass);

    if (spec.type == String.class) {
      RequiredArgumentBuilder<CommandSourceStack, String> a = argument(
          node.name,
          spec.greedy ? StringArgumentType.greedyString() : StringArgumentType.word()
      );
      if (provider != null) a = a.suggests(provider::suggest);
      return a;
    }

    if (spec.type == int.class || spec.type == Integer.class) {
      RequiredArgumentBuilder<CommandSourceStack, Integer> a = argument(node.name, IntegerArgumentType.integer());
      if (provider != null) a = a.suggests(provider::suggest);
      return a;
    }

    if (spec.type == double.class || spec.type == Double.class) {
      RequiredArgumentBuilder<CommandSourceStack, Double> a = argument(node.name, DoubleArgumentType.doubleArg());
      if (provider != null) a = a.suggests(provider::suggest);
      return a;
    }

    throw new IllegalStateException("Unsupported arg type: " + spec.type.getName());
  }
}