package io.nexstudios.framework.paper.key;

import io.nexstudios.framework.core.key.NexKey;
import io.nexstudios.framework.paper.NexPaperPlugin;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Paper-only convenience factories for creating NexKey values from Bukkit objects.
 */
public final class NexPaperKeys {

  private NexPaperKeys() {}

  public static NexKey ofPlugin(Plugin plugin, String key) {
    Objects.requireNonNull(plugin, "plugin");
    return NexKey.ofOwnerName(plugin.getName(), key);
  }

  public static NexKey ofPlugin(NexPaperPlugin plugin, String key) {
    Objects.requireNonNull(plugin, "plugin");
    return NexKey.ofOwnerName(plugin.getPluginMeta().getName(), key);
  }
}