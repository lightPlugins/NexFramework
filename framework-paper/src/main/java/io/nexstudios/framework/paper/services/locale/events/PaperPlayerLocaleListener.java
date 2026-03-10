package io.nexstudios.framework.paper.services.locale.events;

import io.nexstudios.framework.core.service.database.DatabaseAsyncService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.framework.paper.services.thirdparty.HookService;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Dependencies({
    PaperPluginService.class,
    LanguageService.class,
    HookService.class
})
public final class PaperPlayerLocaleListener implements ServiceListener {

  private static final String PDC_KEY = "selected_language";

  private final PaperPluginService paperPluginService;
  private final LanguageService languageService;
  private final HookService hookService;

  private final NamespacedKey localeKey;

  public PaperPlayerLocaleListener(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");
    this.paperPluginService = services.getService(PaperPluginService.class);
    this.languageService = services.getService(LanguageService.class);
    this.hookService = services.getService(HookService.class);

    this.localeKey = new NamespacedKey(paperPluginService.plugin(), PDC_KEY);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    Locale defaultLocale = languageService.getDefaultLocale();

    // default language on loading
    languageService.setUserLocale(uuid, defaultLocale);

    Optional<UserLocaleStore> storeOpt = hookService.findService(UserLocaleStore.class);
    Optional<DatabaseAsyncService> asyncOpt = hookService.findService(DatabaseAsyncService.class);

    if (storeOpt.isPresent() && asyncOpt.isPresent()) {
      UserLocaleStore store = storeOpt.get();
      DatabaseAsyncService async = asyncOpt.get();

      async.executeAsyncInTransaction(em -> {
        return store.findOrCreate(em, uuid, defaultLocale);
      }).thenAccept(loadedLocale -> Bukkit.getScheduler().runTask(
          paperPluginService.plugin(),
          () -> languageService.setUserLocale(uuid, loadedLocale)
      ));

      return;
    }

    // Kein DB-Service aktiv -> PDC-Fallback
    Locale fromPdc = readLocaleFromPdc(player).orElse(defaultLocale);
    languageService.setUserLocale(uuid, fromPdc);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();

    Optional<UserLocaleStore> storeOpt = hookService.findService(UserLocaleStore.class);
    Optional<DatabaseAsyncService> asyncOpt = hookService.findService(DatabaseAsyncService.class);

    // If the DB is active: same as before, only clear the cache (DB persistence may be handled elsewhere by command/setter).
    if (storeOpt.isPresent() && asyncOpt.isPresent()) {
      languageService.clearUserLocale(uuid);
      return;
    }

    // safe local in player data container
    Locale locale = languageService.getUserLocale(uuid);
    writeLocaleToPdc(player, locale);

    languageService.clearUserLocale(uuid);
  }

  private Optional<Locale> readLocaleFromPdc(Player player) {
    PersistentDataContainer pdc = player.getPersistentDataContainer();
    String tag = pdc.get(localeKey, PersistentDataType.STRING);
    if (tag == null || tag.isBlank()) {
      return Optional.empty();
    }

    Locale parsed = Locale.forLanguageTag(tag.trim());
    if (parsed.getLanguage().isBlank()) {
      return Optional.empty();
    }
    return Optional.of(parsed);
  }

  private void writeLocaleToPdc(Player player, Locale locale) {
    Objects.requireNonNull(locale, "locale");
    player.getPersistentDataContainer().set(localeKey, PersistentDataType.STRING, locale.toLanguageTag());
  }
}