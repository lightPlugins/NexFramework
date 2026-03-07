package io.nexstudios.framework.paper.key;

import io.nexstudios.framework.core.key.NexKey;
import org.bukkit.NamespacedKey;

import java.util.Objects;

/**
 * Utility class for converting between {@link NexKey} and Bukkit's {@link NamespacedKey}.
 *
 * This class provides static methods to:
 * - Convert a {@link NexKey} to a {@link NamespacedKey}.
 * - Convert a {@link NamespacedKey} to a {@link NexKey}.
 *
 * The class cannot be instantiated and serves only as a utility.
 */
public final class BukkitNexKeys {

  private BukkitNexKeys() {}

  public static NamespacedKey toBukkit(NexKey nexKey) {
    Objects.requireNonNull(nexKey, "nexKey");
    return new NamespacedKey(nexKey.namespace(), nexKey.key());
  }

  public static NexKey fromBukkit(NamespacedKey bukkitKey) {
    Objects.requireNonNull(bukkitKey, "bukkitKey");
    return NexKey.of(bukkitKey.getNamespace(), bukkitKey.getKey());
  }
}