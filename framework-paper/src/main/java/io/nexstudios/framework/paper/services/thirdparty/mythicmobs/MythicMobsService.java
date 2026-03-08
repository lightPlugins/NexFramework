package io.nexstudios.framework.paper.services.thirdparty.mythicmobs;

import io.nexstudios.serviceregistry.di.Service;
import org.bukkit.Location;

/**
 * Paper-facing MythicMobs integration API.
 *
 * Keep it minimal at first; expand as you actually need features.
 */
public interface MythicMobsService extends Service {

  /**
   * @return true if MythicMobs is available and hook initialized.
   */
  boolean isAvailable();

  /**
   * Checks whether a Mythic mob type/id exists.
   */
  boolean mobTypeExists(String mobTypeId);

  /**
   * Spawns a MythicMob at the given location.
   *
   * @return true if spawn succeeded
   */
  boolean spawn(String mobTypeId, Location location);
}