package io.nexstudios.framework.paper.services.commands.factory;

import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.paper.services.commands.annotations.Command;
import io.nexstudios.framework.paper.services.commands.annotations.CommandRoot;
import io.nexstudios.framework.paper.services.commands.annotations.Greedy;
import io.nexstudios.framework.paper.services.commands.annotations.Suggest;
import io.nexstudios.framework.paper.services.commands.factory.args.ArgParsing;
import io.nexstudios.framework.paper.services.commands.factory.model.ArgSpec;
import io.nexstudios.framework.paper.services.commands.factory.model.CmdNode;
import io.nexstudios.framework.paper.services.commands.factory.model.Exec;
import io.nexstudios.framework.paper.services.commands.factory.suggest.SuggestionProvider;
import io.nexstudios.framework.paper.services.commands.factory.util.CommandUtils;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

public final class CommandModelBuilder {

  private CommandModelBuilder() {}

  public static CommandModel build(NexFramework core, Object handler) {
    CommandRoot rootAnn = handler.getClass().getAnnotation(CommandRoot.class);
    if (rootAnn == null) {
      throw new IllegalStateException("Missing @CommandRoot on " + handler.getClass().getName());
    }

    List<Method> commandMethods = CommandMethodScanner.findCommandMethods(handler.getClass());

    CmdNode root = CmdNode.root(rootAnn.name());
    root.permission = CommandUtils.normalizePerm(rootAnn.permission());
    root.playerOnly = rootAnn.playerOnly();

    for (Method m : commandMethods) {
      Command ann = m.getAnnotation(Command.class);
      String path = ann.value().trim();

      if (path.isEmpty()) {
        root.exec = new Exec(handler, m, CommandUtils.normalizePerm(ann.permission()), ann.playerOnly());
        continue;
      }

      String[] parts = path.split("\\s+");
      CmdNode current = root;

      for (int i = 0; i < parts.length; i++) {
        String token = parts[i];

        if (ArgParsing.isArgToken(token)) {
          ArgParsing.ArgToken arg = ArgParsing.parseArgToken(token);
          ArgParsing.assertGreedyLast(arg, i, parts.length, m);

          current = current.childArg(arg.name(), resolveArgSpec(m, arg.name(), arg.greedy()));
          continue;
        }

        current = current.childLiteral(token);
      }

      current.exec = new Exec(handler, m, CommandUtils.normalizePerm(ann.permission()), ann.playerOnly());
    }

    return new CommandModel(rootAnn, root);
  }

  private static ArgSpec resolveArgSpec(Method method, String argName, boolean greedyFromPath) {
    Parameter param = ArgParsing.findArgParameter(method, argName);
    Class<?> type = param.getType();

    boolean greedy = greedyFromPath || param.isAnnotationPresent(Greedy.class);

    Suggest suggest = param.getAnnotation(Suggest.class);
    Class<? extends SuggestionProvider> suggestClass = (suggest == null) ? null : suggest.value();

    return new ArgSpec(type, greedy, suggestClass);
  }
}