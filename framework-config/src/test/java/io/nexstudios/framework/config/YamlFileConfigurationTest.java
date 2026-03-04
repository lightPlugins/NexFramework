package io.nexstudios.framework.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class YamlFileConfigurationTest {

  @TempDir
  Path tmp;

  @Test
  void save_and_reload_persists_values() {
    Path file = tmp.resolve("cfg.yml");

    YamlFileConfiguration cfg = new YamlFileConfiguration(file, false);
    cfg.set("a.b", 123);
    cfg.save();

    YamlFileConfiguration cfg2 = new YamlFileConfiguration(file, false);
    assertEquals(123, cfg2.getInt("a.b", -1));
  }

  @Test
  void reload_missingFile_yieldsEmptyRoot() throws Exception {
    Path file = tmp.resolve("missing.yml");
    assertFalse(Files.exists(file));

    YamlFileConfiguration cfg = new YamlFileConfiguration(file, false);
    assertFalse(cfg.contains("any"));
    assertEquals("x", cfg.getString("any", "x"));
  }

  @Test
  void save_readOnly_throws() {
    Path file = tmp.resolve("ro.yml");

    YamlFileConfiguration cfg = new YamlFileConfiguration(file, true);
    cfg.set("a", 1);

    assertThrows(IllegalStateException.class, cfg::save);
  }
}