package io.nexstudios.framework.paper.services.thirdparty.mythicmobs;

import io.lumine.mythic.core.mobs.ActiveMob;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.serviceregistry.di.Service;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

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
   * Spawns a MythicMob at the specified location and level.
   *
   * @param mobTypeId the ID of the MythicMob type to spawn
   * @param location the location to spawn the mob
   * @param level the level of the spawned mob
   * @return the spawned entity, or null if the mob type does not exist
   */
  Entity spawn(@NotNull String mobTypeId, @NotNull Location location, int level);

  /**
   * Checks if the given entity is a MythicMob.
   *
   * @param entity the entity to check
   * @return true if the entity is a MythicMob
   */
  boolean isMythicMob(Entity entity);

  /**
   * Retrieves the ActiveMob associated with the given entity, if available.
   *
   * @param entity the entity to query
   * @return an Optional containing the ActiveMob if the entity is a MythicMob, otherwise empty
   */
  Optional<ActiveMob> getActiveMob(Entity entity);

  /**
   * Retrieves all active MythicMobs with the specified name.
   *
   * @param name the name of the MythicMob type to filter
   * @return a collection of matching active MythicMobs
   */
  Collection<ActiveMob> activeMobsByName(String name);

  /**
   * Registers a service listener for event handling.
   *
   * @param listener the service listener to register
   * @return the registered listener
   * @throws NullPointerException if the listener is null
   */
  <T extends ServiceListener> T registerListener(T listener);

  /**
   * Unregisters a previously registered service listener.
   *
   * @param listener the service listener to unregister
   */
  void unregisterListener(ServiceListener listener);
}