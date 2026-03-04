package io.nexstudios.framework.paper.services;

import io.nexstudios.serviceregistry.di.Service;
import org.bukkit.event.Listener;

/**
 * Represents a specialized type of listener that combines the behaviors of
 * a generic event listener (via the Listener interface) and a service (via
 * the Service interface).
 *
 * This interface is primarily used in plugin frameworks or service-oriented
 * architectures where event listeners may also need to act as services.
 * Implementing this interface allows a class to be registered and managed
 * both as an event listener and as a service.
 *
 * Common usage involves:
 * - Registration of the service listener with a service registry or manager.
 * - Management of lifecycle hooks such as initialization or shutdown.
 * - Listening to events and handling them as part of system services.
 */
public interface ServiceListener extends Listener, Service {
}
