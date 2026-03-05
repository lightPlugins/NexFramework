package io.nexstudios.framework.core.service.language;

import io.nexstudios.serviceregistry.di.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public interface UserLocaleStore extends Service {

  /**
   * Ensures a row exists for the user. If missing, inserts with the given default locale.
   * Must be idempotent.
   */
  void ensureExists(UUID userIdentifier, Locale defaultLocale);

  /**
   * Loads the stored locale if present.
   */
  Optional<Locale> find(UUID userIdentifier);

  /**
   * Inserts or updates the stored locale.
   */
  void upsert(UUID userIdentifier, Locale locale);
}