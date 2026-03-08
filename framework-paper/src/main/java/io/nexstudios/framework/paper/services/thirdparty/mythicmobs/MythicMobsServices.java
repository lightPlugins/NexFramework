package io.nexstudios.framework.paper.services.thirdparty.mythicmobs;

import io.nexstudios.framework.paper.services.thirdparty.ThirdPartyPlugins;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

/**
 * Single entry-point to register the MythicMobs integration.
 */
public final class MythicMobsServices {

  private MythicMobsServices() {}

  public static void register(ServiceAccessor services) {
    // Fail-fast: if you call register(), MythicMobs must be enabled.
    ThirdPartyPlugins.requireEnabled(DefaultMythicMobsService.PLUGIN_NAME);
    // MythicMobs API / Service
    services.register(MythicMobsService.class, DefaultMythicMobsService.class);
  }
}