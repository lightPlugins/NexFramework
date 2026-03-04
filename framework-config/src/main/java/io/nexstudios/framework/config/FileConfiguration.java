package io.nexstudios.framework.config;

import org.spongepowered.configurate.CommentedConfigurationNode;

import java.nio.file.Path;

/**
 * Root configuration that can be loaded from and saved to a file.
 */
public abstract class FileConfiguration extends ConfigurationSection {

  protected FileConfiguration(CommentedConfigurationNode node) {
    super(node);
  }

  public abstract Path path();

  public abstract void reload();

  public abstract void save();
}