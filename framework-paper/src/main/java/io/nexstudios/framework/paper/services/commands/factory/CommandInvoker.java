package io.nexstudios.framework.paper.services.commands.factory;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.nexstudios.framework.paper.services.commands.annotations.Arg;
import io.papermc.paper.command.brigadier.CommandSourceStack;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

final class CommandInvoker {

  private CommandInvoker() {}

  static int invoke(Object handler, Method method, CommandContext<CommandSourceStack> ctx) {
    try {
      Object[] args = buildInvokeArgs(method, ctx);
      Object r = method.invoke(handler, args);
      return (r instanceof Integer i) ? i : 1;
    } catch (Exception e) {
      throw new IllegalStateException("Command execution failed: " + method, e);
    }
  }

  private static Object[] buildInvokeArgs(Method method, CommandContext<CommandSourceStack> ctx) {
    Parameter[] params = method.getParameters();
    Object[] out = new Object[params.length];

    for (int i = 0; i < params.length; i++) {
      Parameter p = params[i];
      Class<?> t = p.getType();

      if (t.equals(CommandContext.class)) {
        out[i] = ctx;
        continue;
      }

      if (t.equals(CommandSourceStack.class)) {
        out[i] = ctx.getSource();
        continue;
      }

      Arg a = p.getAnnotation(Arg.class);
      if (a == null) {
        throw new IllegalStateException("Missing @Arg on parameter " + p.getName() + " in " + method);
      }

      String name = a.value();
      if (t.equals(String.class)) {
        out[i] = StringArgumentType.getString(ctx, name);
        continue;
      }
      if (t.equals(int.class) || t.equals(Integer.class)) {
        out[i] = IntegerArgumentType.getInteger(ctx, name);
        continue;
      }
      if (t.equals(double.class) || t.equals(Double.class)) {
        out[i] = DoubleArgumentType.getDouble(ctx, name);
        continue;
      }

      throw new IllegalStateException("Unsupported @Arg parameter type in " + method + ": " + t.getName());
    }

    return out;
  }
}