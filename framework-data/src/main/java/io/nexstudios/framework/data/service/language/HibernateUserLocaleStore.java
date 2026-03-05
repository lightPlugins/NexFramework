package io.nexstudios.framework.data.service.language;

import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.data.service.language.entity.PlayerLocaleEntity;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

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

    String uuid = userIdentifier.toString();
    String tag = defaultLocale.toLanguageTag();

    databaseService.executeInTransaction(em -> {
      PlayerLocaleEntity existing = em.find(PlayerLocaleEntity.class, uuid);
      if (existing != null) {
        return null;
      }
      em.persist(new PlayerLocaleEntity(uuid, tag));
      return null;
    });
  }

  @Override
  public Optional<Locale> find(UUID userIdentifier) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");

    String uuid = userIdentifier.toString();

    return databaseService.executeInTransaction(em -> {
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
    });
  }

  @Override
  public void upsert(UUID userIdentifier, Locale locale) {
    Objects.requireNonNull(userIdentifier, "userIdentifier");
    Objects.requireNonNull(locale, "locale");

    String uuid = userIdentifier.toString();
    String tag = locale.toLanguageTag();

    databaseService.executeInTransaction(em -> {
      PlayerLocaleEntity e = em.find(PlayerLocaleEntity.class, uuid);
      if (e == null) {
        em.persist(new PlayerLocaleEntity(uuid, tag));
        return null;
      }
      e.setLocaleTag(tag);
      return null;
    });
  }
}