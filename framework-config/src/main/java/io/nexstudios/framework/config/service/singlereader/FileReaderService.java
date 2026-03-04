package io.nexstudios.framework.config.service.singlereader;

import io.nexstudios.framework.config.FileConfiguration;
import io.nexstudios.serviceregistry.di.Service;

import java.nio.file.Path;

/**
 * Loads a YAML config file from the data folder.
 *
 * If loadDefaults is enabled:
 * - copies the resource file if the target file does not exist
 * - merges missing scalar keys from defaults into the existing file
 * - never overwrites existing values
 * - never merges or overwrites list/map values (but may create missing parent sections for scalar inserts)
 * - preserves comments (best effort via Configurate; optional patching can be added)
 */
public interface FileReaderService extends Service {

  FileConfiguration load(Path relativePath, String resourcePath, boolean loadDefaults);
}