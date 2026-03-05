package io.nexstudios.framework.core.service.language;

import io.nexstudios.serviceregistry.di.Service;
import jakarta.persistence.EntityManager;

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

  // ... existing code ...

  /**
   * EntityManager-aware variants (to participate in an existing transaction).
   * Default implementations delegate to the legacy methods.
   *
   * Implementations should override these to avoid opening nested transactions.
   */
  default void ensureExists(EntityManager em, UUID userIdentifier, Locale defaultLocale) {
    ensureExists(userIdentifier, defaultLocale);
  }

  default Optional<Locale> find(EntityManager em, UUID userIdentifier) {
    return find(userIdentifier);
  }

  default void upsert(EntityManager em, UUID userIdentifier, Locale locale) {
    upsert(userIdentifier, locale);
  }

  /**
   * Convenience: load locale or insert default (should be implemented efficiently by the backend).
   */
  default Locale findOrCreate(EntityManager em, UUID userIdentifier, Locale defaultLocale) {
    ensureExists(em, userIdentifier, defaultLocale);
    return find(em, userIdentifier).orElse(defaultLocale);
  }
}