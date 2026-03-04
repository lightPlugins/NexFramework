package io.nexstudios.framework.config.service.multireader;

import io.nexstudios.framework.config.FileConfiguration;
import io.nexstudios.serviceregistry.di.Service;

import java.nio.file.Path;
import java.util.Map;

/**
 * Reads all .yml files recursively from a directory.
 *
 * - skips files whose name starts with "_" (e.g. _example.yml)
 * - never writes or copies defaults
 * - caches loaded configurations and supports reload
 */
public interface MultiFileReaderService extends Service {

  Map<Path, FileConfiguration> loadAll(Path relativeDirectory);

  Map<Path, FileConfiguration> cache();

  void reload();
}