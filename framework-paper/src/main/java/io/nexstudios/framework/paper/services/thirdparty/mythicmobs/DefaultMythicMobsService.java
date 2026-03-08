package io.nexstudios.framework.paper.services.thirdparty.mythicmobs;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.framework.paper.services.thirdparty.ThirdPartyPlugins;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Provides a Paper-facing implementation of MythicMobsService.
 */
@Dependencies({
    PaperPluginService.class
})
public final class DefaultMythicMobsService implements MythicMobsService {

  public static final String PLUGIN_NAME = "MythicMobs";

  private final Logger logger;
  private final Plugin ownerPlugin;
  private final BukkitAPIHelper api;
  private final MobExecutor mobExecutor;
  private final Set<ServiceListener> externallyRegisteredListeners = ConcurrentHashMap.newKeySet();

  public DefaultMythicMobsService(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");
    PaperPluginService paperPluginService = services.getService(PaperPluginService.class);
    this.logger = paperPluginService.plugin().getLogger();
    this.ownerPlugin = paperPluginService.plugin();

    // if you register this integration, MythicMobs must be enabled
    ThirdPartyPlugins.requireEnabled(PLUGIN_NAME);

    this.api = MythicBukkit.inst().getAPIHelper();
    this.mobExecutor = MythicBukkit.inst().getMobManager();

    logger.info("MythicMobs integration enabled.");
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  @Override
  public boolean mobTypeExists(String mobTypeId) {
    Objects.requireNonNull(mobTypeId, "mobTypeId");
    return api.getMythicMob(mobTypeId) != null;
  }

  @Override
  @Nullable
  public Entity spawn(@NotNull String mobTypeId, @NotNull Location location, int level) {
    MythicMob mob = mobExecutor.getMythicMob(mobTypeId).orElse(null);
    if(mob != null) {
      ActiveMob knight = mob.spawn(BukkitAdapter.adapt(location),level);
      return knight.getEntity().getBukkitEntity();
    }
    logger.warning("Could not spawn mob type: " + mobTypeId);
    return null;
  }

  @Override
  public boolean isMythicMob(Entity entity) {
    return mobExecutor.isMythicMob(entity);
  }

  @Override
  public Optional<ActiveMob> getActiveMob(Entity entity) {
    return mobExecutor.getActiveMob(entity.getUniqueId());
  }

  @Override
  public Collection<ActiveMob> activeMobsByName(String name) {
    return mobExecutor.getActiveMobs(am -> am.getMobType().equals(name));
  }

  @Override
  public <T extends ServiceListener> T registerListener(T listener) {
    Objects.requireNonNull(listener, "listener");

    // safety: if this service exists, MythicMobs must still be enabled
    ThirdPartyPlugins.requireEnabled(PLUGIN_NAME);

    Bukkit.getPluginManager().registerEvents(listener, ownerPlugin);
    externallyRegisteredListeners.add(listener);
    return listener;
  }

  @Override
  public void unregisterListener(ServiceListener listener) {
    Objects.requireNonNull(listener, "listener");

    HandlerList.unregisterAll(listener);
    externallyRegisteredListeners.remove(listener);
  }

}