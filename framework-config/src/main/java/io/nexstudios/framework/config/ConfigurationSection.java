package io.nexstudios.framework.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;

/**
 * Bukkit-like configuration section backed by Configurate nodes.
 *
 * Paths use dot-notation: "a.b.c".
 * Lists are accessed via dedicated list methods.
 */
public class ConfigurationSection {

  protected CommentedConfigurationNode node;

  public ConfigurationSection(CommentedConfigurationNode node) {
    this.node = Objects.requireNonNull(node, "node");
  }

  public CommentedConfigurationNode node() {
    return node;
  }

  public boolean contains(String path) {
    return !getNode(path).virtual();
  }

  public ConfigurationSection getSection(String path) {
    CommentedConfigurationNode child = getNode(path);
    if (child.virtual()) {
      return null;
    }
    return new ConfigurationSection(child);
  }

  public List<ConfigurationSection> getSectionList(String path) {
    CommentedConfigurationNode child = getNode(path);
    if (child.virtual() || !child.isList()) {
      return List.of();
    }

    List<ConfigurationSection> out = new ArrayList<>();
    for (ConfigurationNode n : child.childrenList()) {
      if (n instanceof CommentedConfigurationNode c) {
        out.add(new ConfigurationSection(c));
      } else {
        out.add(new ConfigurationSection(CommentedConfigurationNode.root().raw(n.raw())));
      }
    }
    return List.copyOf(out);
  }

  public Set<String> getKeys(boolean deep) {
    if (node.virtual() || !node.isMap()) return Set.of();
    Set<String> out = new LinkedHashSet<>();
    collectKeys(node, deep, "", out);
    return Collections.unmodifiableSet(out);
  }

  private static void collectKeys(ConfigurationNode base, boolean deep, String prefix, Set<String> out) {
    for (Map.Entry<Object, ? extends ConfigurationNode> e : base.childrenMap().entrySet()) {
      String k = String.valueOf(e.getKey());
      String full = prefix.isEmpty() ? k : prefix + "." + k;
      out.add(full);
      if (deep) {
        ConfigurationNode n = e.getValue();
        if (n.isMap()) {
          collectKeys(n, true, full, out);
        }
      }
    }
  }

  public Map<String, Object> getValues(boolean deep) {
    if (node.virtual() || !node.isMap()) return Map.of();
    Map<String, Object> out = new LinkedHashMap<>();
    collectValues(node, deep, "", out);
    return Collections.unmodifiableMap(out);
  }

  private static void collectValues(ConfigurationNode base, boolean deep, String prefix, Map<String, Object> out) {
    for (Map.Entry<Object, ? extends ConfigurationNode> e : base.childrenMap().entrySet()) {
      String k = String.valueOf(e.getKey());
      String full = prefix.isEmpty() ? k : prefix + "." + k;
      ConfigurationNode n = e.getValue();

      out.put(full, n.raw());

      if (deep && n.isMap()) {
        collectValues(n, true, full, out);
      }
    }
  }

  public String getString(String path, String def) {
    ConfigurationNode n = getNode(path);
    String v = n.getString();
    return v != null ? v : def;
  }

  public int getInt(String path, int def) {
    ConfigurationNode n = getNode(path);
    int v = n.getInt(Integer.MIN_VALUE);
    return v != Integer.MIN_VALUE ? v : def;
  }

  public boolean getBoolean(String path, boolean def) {
    ConfigurationNode n = getNode(path);
    return n.getBoolean(def);
  }

  public double getDouble(String path, double def) {
    ConfigurationNode n = getNode(path);
    double v = n.getDouble(Double.NaN);
    return !Double.isNaN(v) ? v : def;
  }

  public List<String> getStringList(String path) {
    ConfigurationNode n = getNode(path);
    if (n.virtual() || !n.isList()) return List.of();
    List<String> out = new ArrayList<>();
    for (ConfigurationNode c : n.childrenList()) {
      Object raw = c.raw();
      if (raw instanceof String s) {
        out.add(s);
      }
    }
    return List.copyOf(out);
  }

  public String getComment(String path) {
    CommentedConfigurationNode n = getNode(path);
    return n.comment();
  }

  public void setComment(String path, String comment) {
    CommentedConfigurationNode n = getOrCreateNode(path);
    n.comment(comment);
  }

  public void set(String path, Object value) {
    CommentedConfigurationNode n = getOrCreateNode(path);
    try {
      n.set(value);
    } catch (SerializationException e) {
      throw new IllegalStateException("Failed to set value at path '" + path + "' to '" + value + "'", e);
    }
  }

  protected CommentedConfigurationNode getNode(String path) {
    Objects.requireNonNull(path, "path");
    if (path.isBlank()) return node;
    return node.node((Object[]) path.split("\\."));
  }

  protected CommentedConfigurationNode getOrCreateNode(String path) {
    Objects.requireNonNull(path, "path");
    if (path.isBlank()) return node;
    return node.node((Object[]) path.split("\\."));
  }
}