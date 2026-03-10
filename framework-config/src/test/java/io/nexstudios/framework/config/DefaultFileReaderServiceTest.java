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
    public ClassLoader classLoader() {
      return getClass().getClassLoader();
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

  // ... existing code ...
}