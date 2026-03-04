package io.nexstudios.framework.paper.services.commands.factory.util;

public final class CommandUtils {

  private CommandUtils() {}

  public static String normalizePerm(String permission) {
    return permission == null ? "" : permission.trim();
  }
}