package io.nexstudios.framework.config;

import io.nexstudios.framework.config.service.multireader.DefaultMultiFileReaderService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultMultiFileReaderServiceTest {

  @TempDir
  Path dataDir;

  private static final class TestDataFolderService implements DataFolderService {
    private final Path dataFolder;
    private TestDataFolderService(Path dataFolder) { this.dataFolder = dataFolder; }
    @Override public Path getDataFolder() { return dataFolder; }
    @Override public void bind(Path dataFolder) { throw new UnsupportedOperationException("not used"); }
  }

  @Test
  void loadAll_loadsYamlRecursively_andSkipsUnderscoreFiles() throws Exception {
    Path dir = dataDir.resolve("configs");
    Files.createDirectories(dir.resolve("nested"));

    Files.writeString(dir.resolve("a.yml"), "x: 1\n", StandardCharsets.UTF_8);
    Files.writeString(dir.resolve("_example.yml"), "x: 2\n", StandardCharsets.UTF_8);
    Files.writeString(dir.resolve("nested").resolve("b.yml"), "x: 3\n", StandardCharsets.UTF_8);

    var svc = new DefaultMultiFileReaderService(new TestDataFolderService(dataDir));

    Map<Path, FileConfiguration> all = svc.loadAll(Path.of("configs"));

    assertTrue(all.containsKey(Path.of("a.yml")));
    assertTrue(all.containsKey(Path.of("nested").resolve("b.yml")));
    assertFalse(all.containsKey(Path.of("_example.yml")));

    assertEquals(1, all.get(Path.of("a.yml")).getInt("x", -1));
    assertEquals(3, all.get(Path.of("nested").resolve("b.yml")).getInt("x", -1));
  }

  @Test
  void reload_requiresLoadAllFirst() {
    var svc = new DefaultMultiFileReaderService(new TestDataFolderService(dataDir));
    assertThrows(IllegalStateException.class, svc::reload);
  }

  @Test
  void reload_refreshesCache() throws Exception {
    Path dir = dataDir.resolve("configs");
    Files.createDirectories(dir);

    Files.writeString(dir.resolve("a.yml"), "x: 1\n", StandardCharsets.UTF_8);

    var svc = new DefaultMultiFileReaderService(new TestDataFolderService(dataDir));
    svc.loadAll(Path.of("configs"));

    assertTrue(svc.cache().containsKey(Path.of("a.yml")));
    assertFalse(svc.cache().containsKey(Path.of("b.yml")));

    Files.writeString(dir.resolve("b.yml"), "x: 2\n", StandardCharsets.UTF_8);

    svc.reload();

    assertTrue(svc.cache().containsKey(Path.of("a.yml")));
    assertTrue(svc.cache().containsKey(Path.of("b.yml")));
  }
}