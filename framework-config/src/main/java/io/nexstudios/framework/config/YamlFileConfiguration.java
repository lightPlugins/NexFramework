package io.nexstudios.framework.config;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader.Builder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * YAML file configuration backed by Configurate.
 *
 * Comments are stored on nodes (CommentedConfigurationNode). Header preservation is enabled.
 */
public final class YamlFileConfiguration extends FileConfiguration {

  private final Path path;
  private final YamlConfigurationLoader loader;
  private final boolean readOnly;

  public YamlFileConfiguration(Path path, boolean readOnly) {
    super(CommentedConfigurationNode.root());
    this.path = Objects.requireNonNull(path, "path");
    this.readOnly = readOnly;

    Builder b = YamlConfigurationLoader.builder()
        .path(path)
        .nodeStyle(NodeStyle.BLOCK)
        .indent(2)
        .headerMode(HeaderMode.PRESERVE);

    this.loader = b.build();
    reload();
  }

  @Override
  public Path path() {
    return path;
  }

  @Override
  public void reload() {
    try {
      if (Files.notExists(path)) {
        this.node = CommentedConfigurationNode.root();
        return;
      }
      this.node = loader.load();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load YAML configuration: " + path, e);
    }
  }

  @Override
  public void save() {
    if (readOnly) {
      throw new IllegalStateException("This configuration is read-only and cannot be saved: " + path);
    }
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      loader.save(node);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to save YAML configuration: " + path, e);
    }
  }
}