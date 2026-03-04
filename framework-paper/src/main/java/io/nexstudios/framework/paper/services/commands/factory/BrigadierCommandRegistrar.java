package io.nexstudios.framework.paper.services.commands.factory;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.paper.services.commands.factory.args.ArgTypeSupport;
import io.nexstudios.framework.paper.services.commands.factory.model.CmdNode;
import io.nexstudios.framework.paper.services.commands.factory.model.NodeKind;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

import static io.papermc.paper.command.brigadier.Commands.literal;

public final class BrigadierCommandRegistrar {

  private BrigadierCommandRegistrar() {}

  public static void register(NexFramework core, Commands commands, CommandModel model) {
    CmdNode root = model.root();

    LiteralArgumentBuilder<CommandSourceStack> rootBuilder = literal(root.name);
    CommandRequires.apply(rootBuilder, root.permission, root.playerOnly);

    resolve(core, root, rootBuilder);

    commands.register(
        rootBuilder.build(),
        model.rootAnn().description(),
        List.of(model.rootAnn().aliases())
    );
  }

  private static ArgumentBuilder<CommandSourceStack, ?> buildBuilder(NexFramework core, CmdNode node) {
    ArgumentBuilder<CommandSourceStack, ?> builder =
        (node.kind == NodeKind.LITERAL)
            ? literal(node.name)
            : ArgTypeSupport.buildArgumentBuilder(core, node);

    resolve(core, node, builder);

    return builder;
  }

  private static void resolve(NexFramework core, CmdNode node, ArgumentBuilder<CommandSourceStack, ?> builder) {
    if (node.exec != null) {
      CommandRequires.apply(builder, node.exec.permission, node.exec.playerOnly);
      builder.executes(ctx -> CommandInvoker.invoke(node.exec.handler, node.exec.method, ctx));
    }

    for (CmdNode child : node.children.values()) {
      builder.then(buildBuilder(core, child));
    }
  }
}