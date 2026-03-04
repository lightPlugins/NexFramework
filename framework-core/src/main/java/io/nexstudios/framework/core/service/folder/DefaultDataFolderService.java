package io.nexstudios.framework.core.service.folder;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Default implementation storing the data folder in memory.
 *
 * This service must be bound by the platform (Paper/Velocity) before use.
 */
public final class DefaultDataFolderService implements DataFolderService {

  private volatile Path dataFolder;

  @Override
  public Path getDataFolder() {
    Path local = dataFolder;
    if (local == null) {
      throw new IllegalStateException(
          "DataFolderService is not bound. The platform must call bind(Path) before accessing the data folder."
      );
    }
    return local;
  }

  @Override
  public void bind(Path dataFolder) {
    this.dataFolder = Objects.requireNonNull(dataFolder, "dataFolder");
  }
}