package io.nexstudios.framework.paper.services.locale.events;

import io.nexstudios.framework.core.service.database.DatabaseAsyncService;
import io.nexstudios.framework.core.service.language.LanguageService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Dependencies({
    PaperPluginService.class,
    LanguageService.class,
    UserLocaleStore.class,
    DatabaseAsyncService.class
})
public final class PaperPlayerLocaleListener implements ServiceListener {

  private final PaperPluginService paperPluginService;
  private final LanguageService languageService;
  private final UserLocaleStore userLocaleStore;
  private final DatabaseAsyncService databaseAsyncService;

  public PaperPlayerLocaleListener(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");
    this.paperPluginService = services.getService(PaperPluginService.class);
    this.languageService = services.getService(LanguageService.class);
    this.userLocaleStore = services.getService(UserLocaleStore.class);
    this.databaseAsyncService = services.getService(DatabaseAsyncService.class);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    Locale defaultLocale = languageService.getDefaultLocale();

    // First, set the default locale immediately
    languageService.setUserLocale(uuid, defaultLocale);

    // Now load language from DB asynchronously
    databaseAsyncService.executeAsyncInTransaction(em -> {
      userLocaleStore.ensureExists(uuid, defaultLocale);
      return userLocaleStore.find(uuid).orElse(defaultLocale);
    }).thenAccept(loadedLocale -> {
      // back to main thread
      Bukkit.getScheduler().runTask(
          paperPluginService.plugin(),
          () -> languageService.setUserLocale(uuid, loadedLocale)
      );
    });
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    languageService.clearUserLocale(event.getPlayer().getUniqueId());
  }
}