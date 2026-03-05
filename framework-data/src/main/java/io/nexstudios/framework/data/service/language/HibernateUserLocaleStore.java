package io.nexstudios.framework.data.service.language;

import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.data.service.language.entity.PlayerLocaleEntity;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import jakarta.persistence.EntityManager;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Dependencies({
    DatabaseService.class
})
public final class HibernateUserLocaleStore implements UserLocaleStore {

  private final DatabaseService databaseService;

  public HibernateUserLocaleStore(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");
    this.databaseService = services.getService(DatabaseService.class);
  }

  @Override
  public void ensureExists(UUID userIdentifier, Locale defaultLocale) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(defaultLocale, "defaultLocale");

    databaseService.executeInTransaction(em -> {
      ensureExists(em, userIdentifier, defaultLocale);
      return null;
    });
  }

  @Override
  public Optional<Locale> find(UUID userIdentifier) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");

    return databaseService.executeInTransaction(em -> {
      return find(em, userIdentifier);
    });
  }

  @Override
  public void upsert(UUID userIdentifier, Locale locale) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(locale, "locale");

    databaseService.executeInTransaction(em -> {
      upsert(em, userIdentifier, locale);
      return null;
    });
  }

  @Override
  public void ensureExists(EntityManager em, UUID userIdentifier, Locale defaultLocale) {
    Objects.requireNonNull(em, "em");
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(defaultLocale, "defaultLocale");

    String uuid = userIdentifier.toString();
    String tag = defaultLocale.toLanguageTag();

    PlayerLocaleEntity existing = em.find(PlayerLocaleEntity.class, uuid);
    if (existing != null) {
      return;
    }
    em.persist(new PlayerLocaleEntity(uuid, tag));
  }

  @Override
  public Optional<Locale> find(EntityManager em, UUID userIdentifier) {
    Objects.requireNonNull(em, "em");
    Objects.requireNonNull(userIdentifier, "userIdentifier");

    String uuid = userIdentifier.toString();

    PlayerLocaleEntity e = em.find(PlayerLocaleEntity.class, uuid);
    if (e == null) {
      return Optional.empty();
    }

    String tag = e.getLocaleTag();
    if (tag == null || tag.isBlank()) {
      return Optional.empty();
    }

    Locale locale = Locale.forLanguageTag(tag.trim());
    if (locale.getLanguage().isBlank()) {
      return Optional.empty();
    }

    return Optional.of(locale);
  }

  @Override
  public void upsert(EntityManager em, UUID userIdentifier, Locale locale) {
    Objects.requireNonNull(em, "em");
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(locale, "locale");

    String uuid = userIdentifier.toString();
    String tag = locale.toLanguageTag();

    PlayerLocaleEntity e = em.find(PlayerLocaleEntity.class, uuid);
    if (e == null) {
      em.persist(new PlayerLocaleEntity(uuid, tag));
      return;
    }
    e.setLocaleTag(tag);
  }

  @Override
  public Locale findOrCreate(EntityManager em, UUID userIdentifier, Locale defaultLocale) {
    Objects.requireNonNull(em, "em");
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(defaultLocale, "defaultLocale");

    String uuid = userIdentifier.toString();

    PlayerLocaleEntity e = em.find(PlayerLocaleEntity.class, uuid);
    if (e == null) {
      Locale def = defaultLocale;
      em.persist(new PlayerLocaleEntity(uuid, def.toLanguageTag()));
      return def;
    }

    String tag = e.getLocaleTag();
    if (tag == null || tag.isBlank()) {
      return defaultLocale;
    }

    Locale parsed = Locale.forLanguageTag(tag.trim());
    return parsed.getLanguage().isBlank() ? defaultLocale : parsed;
  }
}