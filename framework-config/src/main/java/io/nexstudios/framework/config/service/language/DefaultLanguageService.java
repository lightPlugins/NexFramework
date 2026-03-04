package io.nexstudios.framework.config.service.language;

import io.nexstudios.framework.config.FileConfiguration;
import io.nexstudios.framework.config.service.multireader.MultiFileReaderService;
import io.nexstudios.framework.config.service.singlereader.FileReaderService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.framework.core.service.resource.ResourceService;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Dependencies({
    MultiFileReaderService.class,
    FileReaderService.class,
    ResourceService.class
})
public final class DefaultLanguageService implements LanguageService {

  private static final Path LANGUAGES_DIRECTORY = Path.of("languages");

  private final MultiFileReaderService multiFileReaderService;
  private final FileReaderService fileReaderService;
  private final ResourceService resourceService;

  private final ConcurrentHashMap<UUID, Locale> userLocalesByIdentifier = new ConcurrentHashMap<>();

  private volatile Locale defaultLocale = Locale.ENGLISH;

  private volatile Map<Locale, Map<String, String>> translationsByLocale = Map.of();

  public DefaultLanguageService(ServiceAccessor serviceAccessor) {
    Objects.requireNonNull(serviceAccessor, "serviceAccessor");

    this.multiFileReaderService = serviceAccessor.getService(MultiFileReaderService.class);
    this.fileReaderService = serviceAccessor.getService(FileReaderService.class);
    this.resourceService = serviceAccessor.getService(ResourceService.class);

    ensureLanguageFilesExistFromResources();

    this.multiFileReaderService.loadAll(LANGUAGES_DIRECTORY);
    rebuildTranslationsCache();
  }

  @Override
  public String getTranslation(String translationKey) {
    return getTranslation(defaultLocale, translationKey);
  }

  @Override
  public String getTranslation(Locale locale, String translationKey) {
    Objects.requireNonNull(locale, "locale");
    Objects.requireNonNull(translationKey, "translationKey");

    String normalizedTranslationKey = translationKey.trim();
    if (normalizedTranslationKey.isEmpty()) {
      return translationKey;
    }

    Map<Locale, Map<String, String>> translationsSnapshot = this.translationsByLocale;

    String direct = getTranslationFromSnapshot(translationsSnapshot, locale, normalizedTranslationKey);
    if (direct != null) {
      return direct;
    }

    Locale configuredDefaultLocale = this.defaultLocale;
    if (!configuredDefaultLocale.equals(locale)) {
      String fallbackToDefault = getTranslationFromSnapshot(translationsSnapshot, configuredDefaultLocale, normalizedTranslationKey);
      if (fallbackToDefault != null) {
        return fallbackToDefault;
      }
    }

    String fallbackToEnglish = getTranslationFromSnapshot(translationsSnapshot, Locale.ENGLISH, normalizedTranslationKey);
    if (fallbackToEnglish != null) {
      return fallbackToEnglish;
    }

    return normalizedTranslationKey;
  }

  @Override
  public String getTranslation(UUID userIdentifier, String translationKey) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Locale locale = getUserLocale(userIdentifier);
    return getTranslation(locale, translationKey);
  }

  @Override
  public void setDefaultLocale(Locale locale) {
    this.defaultLocale = Objects.requireNonNull(locale, "locale");
  }

  @Override
  public Locale getDefaultLocale() {
    return defaultLocale;
  }

  @Override
  public void setUserLocale(UUID userIdentifier, Locale locale) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(locale, "locale");
    userLocalesByIdentifier.put(userIdentifier, locale);
  }

  @Override
  public Locale getUserLocale(UUID userIdentifier) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    return userLocalesByIdentifier.getOrDefault(userIdentifier, defaultLocale);
  }

  @Override
  public Set<Locale> getAvailableLocales() {
    return Collections.unmodifiableSet(translationsByLocale.keySet());
  }

  @Override
  public void reload() {
    ensureLanguageFilesExistFromResources();
    multiFileReaderService.reload();
    rebuildTranslationsCache();
  }

  private void ensureLanguageFilesExistFromResources() {
    ClassLoader classLoader = resourceService.classLoader();

    URL languagesRootUrl;
    try {
      languagesRootUrl = classLoader.getResource("languages");
    } catch (Exception exception) {
      return;
    }

    if (languagesRootUrl == null) {
      return;
    }

    String protocol = languagesRootUrl.getProtocol();

    if ("file".equalsIgnoreCase(protocol)) {
      ensureLanguageFilesExistFromFileUrl(languagesRootUrl);
      return;
    }

    if ("jar".equalsIgnoreCase(protocol)) {
      ensureLanguageFilesExistFromJarUrl(languagesRootUrl);
      return;
    }
  }

  private void ensureLanguageFilesExistFromFileUrl(URL languagesRootUrl) {
    try {
      Path languagesRootPath = Path.of(languagesRootUrl.toURI());

      try (Stream<Path> walkedPaths = Files.walk(languagesRootPath)) {
        walkedPaths
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(".yml"))
            .filter(path -> !path.getFileName().toString().startsWith("_"))
            .forEach(path -> {
              Path relativeToLanguagesRoot = languagesRootPath.relativize(path);
              String resourcePath = "languages/" + relativeToLanguagesRoot.toString().replace('\\', '/');
              Path targetRelativePath = LANGUAGES_DIRECTORY.resolve(relativeToLanguagesRoot);

              fileReaderService.load(targetRelativePath, resourcePath, true);
            });
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to enumerate language resources from file URL: " + languagesRootUrl, exception);
    }
  }

  private void ensureLanguageFilesExistFromJarUrl(URL languagesRootUrl) {
    try {
      URI languagesRootUri = languagesRootUrl.toURI();

      URI jarFileSystemUri = toJarFileSystemRootUri(languagesRootUri);

      try (FileSystem fileSystem = openOrCreateFileSystem(jarFileSystemUri)) {
        Path languagesRootPath = fileSystem.getPath("/languages");

        if (Files.notExists(languagesRootPath)) {
          return;
        }

        try (Stream<Path> walkedPaths = Files.walk(languagesRootPath)) {
          walkedPaths
              .filter(Files::isRegularFile)
              .filter(path -> path.getFileName().toString().endsWith(".yml"))
              .filter(path -> !path.getFileName().toString().startsWith("_"))
              .forEach(path -> {
                Path relativeToLanguagesRoot = languagesRootPath.relativize(path);
                String resourcePath = "languages/" + relativeToLanguagesRoot.toString().replace('\\', '/');
                Path targetRelativePath = LANGUAGES_DIRECTORY.resolve(relativeToLanguagesRoot.toString().replace('\\', '/'));

                fileReaderService.load(targetRelativePath, resourcePath, true);
              });
        }
      }
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to enumerate language resources from jar URL: " + languagesRootUrl, exception);
    }
  }

  private static URI toJarFileSystemRootUri(URI languagesRootUri) {
    String raw = languagesRootUri.toString();
    int separatorIndex = raw.indexOf("!/");
    if (separatorIndex == -1) {
      return languagesRootUri;
    }
    return URI.create(raw.substring(0, separatorIndex + 2));
  }

  private static FileSystem openOrCreateFileSystem(URI uri) {
    try {
      return FileSystems.newFileSystem(uri, Map.of());
    } catch (Exception alreadyExistsOrUnsupported) {
      return FileSystems.getFileSystem(uri);
    }
  }

  private static String getTranslationFromSnapshot(
      Map<Locale, Map<String, String>> translationsSnapshot,
      Locale locale,
      String translationKey
  ) {
    Map<String, String> translations = translationsSnapshot.get(locale);
    if (translations == null) {
      return null;
    }
    return translations.get(translationKey);
  }

  private void rebuildTranslationsCache() {
    Map<Path, FileConfiguration> filesByRelativePath = multiFileReaderService.cache();

    Map<Locale, Map<String, String>> nextTranslationsByLocale = new LinkedHashMap<>();

    for (Map.Entry<Path, FileConfiguration> entry : filesByRelativePath.entrySet()) {
      Path relativePath = entry.getKey();
      FileConfiguration fileConfiguration = entry.getValue();

      Locale locale = parseLocaleFromLanguageFileName(relativePath.getFileName().toString());
      Map<String, String> flatTranslations = flattenStringValues(fileConfiguration);

      nextTranslationsByLocale.put(locale, Map.copyOf(flatTranslations));
    }

    translationsByLocale = Map.copyOf(nextTranslationsByLocale);
  }

  private static Locale parseLocaleFromLanguageFileName(String fileName) {
    String normalizedFileName = Objects.requireNonNull(fileName, "fileName").trim();

    if (!normalizedFileName.endsWith(".yml")) {
      throw new IllegalStateException("Language file must end with .yml: " + fileName);
    }

    String localeToken = normalizedFileName.substring(0, normalizedFileName.length() - ".yml".length()).trim();
    if (localeToken.isEmpty()) {
      throw new IllegalStateException("Language file name is missing locale token: " + fileName);
    }

    String languageTag = localeToken.replace('_', '-');
    Locale locale = Locale.forLanguageTag(languageTag);

    if (locale.getLanguage().isBlank()) {
      throw new IllegalStateException("Invalid locale token in language file: " + fileName);
    }

    return locale;
  }

  private static Map<String, String> flattenStringValues(FileConfiguration fileConfiguration) {
    Map<String, Object> valuesByPath = fileConfiguration.getValues(true);

    Map<String, String> flatTranslations = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : valuesByPath.entrySet()) {
      String path = entry.getKey();
      Object rawValue = entry.getValue();

      if (rawValue instanceof String stringValue) {
        flatTranslations.put(path, stringValue);
      }
    }

    return flatTranslations;
  }
}