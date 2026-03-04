package io.nexstudios.framework.config.service.singlereader;

import io.nexstudios.framework.config.CommentRoundtripPatcher;
import io.nexstudios.framework.config.FileConfiguration;
import io.nexstudios.framework.config.YamlFileConfiguration;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import org.spongepowered.configurate.CommentedConfigurationNode;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

public final class DefaultFileReaderService implements FileReaderService {

  private final DataFolderService dataFolderService;
  private final ResourceService resourceService;

  public DefaultFileReaderService(DataFolderService dataFolderService, ResourceService resourceService) {
    this.dataFolderService = Objects.requireNonNull(dataFolderService, "dataFolderService");
    this.resourceService = Objects.requireNonNull(resourceService, "resourceService");
  }

  @Override
  public FileConfiguration load(Path relativePath, String resourcePath, boolean loadDefaults) {
    Objects.requireNonNull(relativePath, "relativePath");
    Objects.requireNonNull(resourcePath, "resourcePath");

    if (!resourcePath.endsWith(".yml")) {
      throw new IllegalArgumentException("Only .yml resources are supported: " + resourcePath);
    }
    if (!relativePath.toString().endsWith(".yml")) {
      throw new IllegalArgumentException("Only .yml files are supported: " + relativePath);
    }

    Path target = dataFolderService.getDataFolder().resolve(relativePath);

    if (loadDefaults) {
      ensureCopiedIfMissing(target, resourcePath);
      mergeMissingScalarDefaults(target, resourcePath);
    }

    return new YamlFileConfiguration(target, false);
  }

  private void ensureCopiedIfMissing(Path target, String resourcePath) {
    if (Files.exists(target)) return;

    try (InputStream in = resourceService.openResource(resourcePath)
        .orElseThrow(() -> new IllegalStateException("Missing default resource: " + resourcePath))) {
      if (target.getParent() != null) {
        Files.createDirectories(target.getParent());
      }
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to copy default resource " + resourcePath + " to " + target, e);
    }
  }

  private boolean mergeMissingScalarDefaults(Path target, String resourcePath) {
    String originalText = readFileIfExists(target);
    String defaultsText = readResourceToString(resourcePath);

    CommentedConfigurationNode defaultsRoot = loadDefaultsNode(resourcePath);

    YamlFileConfiguration cfg = new YamlFileConfiguration(target, false);
    CommentedConfigurationNode targetRoot = cfg.node();

    boolean changed = mergeScalarLeaves(defaultsRoot, targetRoot);

    if (changed) {
      cfg.save();

      String generatedText = readFileIfExists(target);
      String patched = CommentRoundtripPatcher.patch(originalText, defaultsText, generatedText);

      if (!patched.equals(generatedText)) {
        writeUtf8(target, patched);
      }
    }

    return changed;
  }

  private String readResourceToString(String resourcePath) {
    try (InputStream in = resourceService.openResource(resourcePath)
        .orElseThrow(() -> new IllegalStateException("Missing default resource: " + resourcePath))) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read default resource as text: " + resourcePath, e);
    }
  }
  private String readFileIfExists(Path path) {
    try {
      if (Files.notExists(path)) return "";
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read file: " + path, e);
    }
  }

  private void writeUtf8(Path path, String text) {
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.writeString(path, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to write patched YAML: " + path, e);
    }
  }


  private CommentedConfigurationNode loadDefaultsNode(String resourcePath) {
    try (InputStream in = resourceService.openResource(resourcePath)
        .orElseThrow(() -> new IllegalStateException("Missing default resource: " + resourcePath))) {

      Path tmp = Files.createTempFile("nex-defaults-", ".yml");
      try {
        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        YamlFileConfiguration tmpCfg = new YamlFileConfiguration(tmp, true);
        return tmpCfg.node();
      } finally {
        try { Files.deleteIfExists(tmp); } catch (Exception ignored) { }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load default resource YAML: " + resourcePath, e);
    }
  }

  private boolean mergeScalarLeaves(CommentedConfigurationNode defaults, CommentedConfigurationNode target) {
    boolean[] changed = {false};
    mergeRecursive(defaults, target, changed);
    return changed[0];
  }

  private void mergeRecursive(CommentedConfigurationNode defaults, CommentedConfigurationNode target, boolean[] changed) {
    if (defaults.isMap()) {
      for (var e : defaults.childrenMap().entrySet()) {
        Object key = e.getKey();
        CommentedConfigurationNode defChild = e.getValue();
        CommentedConfigurationNode targetChild = target.node(key);
        mergeRecursive(defChild, targetChild, changed);
      }
      return;
    }

    if (defaults.isList()) {
      return; // intentionally ignored
    }

    if (target.virtual()) {
      target.raw(defaults.raw());

      String c = defaults.comment();
      if (c != null && !c.isBlank()) {
        target.comment(c);
      }
      changed[0] = true;
    }
  }
}