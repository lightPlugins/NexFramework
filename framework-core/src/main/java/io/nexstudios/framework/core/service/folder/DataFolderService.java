package io.nexstudios.framework.core.service.folder;

import io.nexstudios.serviceregistry.di.Service;

import java.nio.file.Path;

/**
 * Provides the platform-specific data directory (plugin data folder) in a platform-neutral way.
 *
 * The platform module (Paper/Velocity) must bind the data folder during startup.
 */
public interface DataFolderService extends Service {

  /**
   * Returns the bound data folder.
   *
   * @return the data folder path
   * @throws IllegalStateException if the service has not been bound yet
   */
  Path getDataFolder();

  /**
   * Binds the data folder path. Typically called once during platform startup.
   *
   * @param dataFolder the data folder path
   */
  void bind(Path dataFolder);
}