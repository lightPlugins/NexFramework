package io.nexstudios.framework.config;

import io.nexstudios.framework.config.service.singlereader.DefaultFileReaderService;
import io.nexstudios.framework.core.service.folder.DataFolderService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultFileReaderServiceTest {

  @TempDir
  Path dataDir;

  private static final class TestDataFolderService implements DataFolderService {
    private final Path dataFolder;
    private TestDataFolderService(Path dataFolder) { this.dataFolder = dataFolder; }
    @Override public Path getDataFolder() { return dataFolder; }
    @Override public void bind(Path dataFolder) { throw new UnsupportedOperationException("not used"); }
  }

  private static final class MapResourceService implements ResourceService {
    private final Map<String, String> resources;
    private MapResourceService(Map<String, String> resources) { this.resources = resources; }

    @Override
    public Optional<InputStream> openResource(String resourcePath) {
      String s = resources.get(resourcePath);
      if (s == null) return Optional.empty();
      return Optional.of(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void bind(ClassLoader classLoader) { throw new UnsupportedOperationException("not used"); }
  }

  @Test
  void load_copiesDefaultsIfMissing_whenLoadDefaultsTrue() throws Exception {
    String defaults = """
        # header
        a: 1
        """;

    var dfs = new TestDataFolderService(dataDir);
    var rs = new MapResourceService(Map.of("config.yml", defaults));

    var svc = new DefaultFileReaderService(dfs, rs);

    Path rel = Path.of("config.yml");
    Path target = dataDir.resolve(rel);
    assertFalse(Files.exists(target));

    FileConfiguration cfg = svc.load(rel, "config.yml", true);

    assertTrue(Files.exists(target));
    assertEquals(1, cfg.getInt("a", -1));
    assertTrue(Files.readString(target).contains("a: 1"));
  }

  @Test
  void load_mergesMissingScalarLeaves_butNeverOverwritesExisting() throws Exception {
    String defaults = """
        root:
          # comment for a
          a: 1
          b: 2
          child:
            c: 3
          list:
            - 1
            - 2
          map:
            x: 9
        """;

    // existing has root.a already, plus list/map already (should not be overwritten/merged)
    String existing = """
        root:
          a: 999
          list:
            - 42
          map:
            x: 111
        """;

    var dfs = new TestDataFolderService(dataDir);
    var rs = new MapResourceService(Map.of("config.yml", defaults));
    var svc = new DefaultFileReaderService(dfs, rs);

    Path rel = Path.of("config.yml");
    Path target = dataDir.resolve(rel);
    Files.createDirectories(target.getParent());
    Files.writeString(target, existing, StandardCharsets.UTF_8);

    FileConfiguration cfg = svc.load(rel, "config.yml", true);

    // existing scalar not overwritten
    assertEquals(999, cfg.getInt("root.a", -1));

    // missing scalar inserted
    assertEquals(2, cfg.getInt("root.b", -1));
    assertEquals(3, cfg.getInt("root.child.c", -1));

    // list/map should remain as-is (no merge)
    var listNode = cfg.node().node("root", "list");
    assertTrue(listNode.isList());
    assertEquals(1, listNode.childrenList().size());
    assertEquals(42, listNode.childrenList().getFirst().raw());

    assertEquals(111, cfg.getInt("root.map.x", -1));
  }

  @Test
  void load_validatesYmlExtensions() {
    var dfs = new TestDataFolderService(dataDir);
    var rs = new MapResourceService(Map.of("config.yml", "a: 1"));
    var svc = new DefaultFileReaderService(dfs, rs);

    assertThrows(IllegalArgumentException.class, () -> svc.load(Path.of("config.txt"), "config.yml", true));
    assertThrows(IllegalArgumentException.class, () -> svc.load(Path.of("config.yml"), "config.txt", true));
  }

  @Test
  void load_defaultsMissing_throws() {
    var dfs = new TestDataFolderService(dataDir);
    var rs = new MapResourceService(Map.of()); // no resources
    var svc = new DefaultFileReaderService(dfs, rs);

    assertThrows(IllegalStateException.class, () -> svc.load(Path.of("config.yml"), "config.yml", true));
  }
}