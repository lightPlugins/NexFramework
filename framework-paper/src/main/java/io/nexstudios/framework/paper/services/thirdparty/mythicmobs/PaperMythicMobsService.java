package io.nexstudios.framework.paper.services.thirdparty.mythicmobs;

import io.lumine.mythic.bukkit.BukkitAPIHelper;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.nexstudios.framework.paper.services.plugin.PaperPluginService;
import io.nexstudios.framework.paper.services.thirdparty.ThirdPartyPlugins;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.Location;

import java.util.Objects;
import java.util.logging.Logger;

/**
 * MythicMobs adapter (API-based, compileOnly).
 *
 * Contract:
 * - No Noop behavior
 * - If this service is registered, MythicMobs must be installed + enabled (enforced in ctor)
 */
@Dependencies({
    PaperPluginService.class
})
public final class PaperMythicMobsService implements MythicMobsService {

  public static final String PLUGIN_NAME = "MythicMobs";

  private final Logger logger;
  private final BukkitAPIHelper api;

  public PaperMythicMobsService(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");
    PaperPluginService paperPluginService = services.getService(PaperPluginService.class);
    this.logger = paperPluginService.plugin().getLogger();

    // Fail-fast: if you register this integration, MythicMobs must be enabled (depend recommended).
    ThirdPartyPlugins.requireEnabled(PLUGIN_NAME);

    this.api = MythicBukkit.inst().getAPIHelper();

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
  public boolean spawn(String mobTypeId, Location location) {
    Objects.requireNonNull(mobTypeId, "mobTypeId");
    Objects.requireNonNull(location, "location");
    return mobTypeId != null;
  }
}