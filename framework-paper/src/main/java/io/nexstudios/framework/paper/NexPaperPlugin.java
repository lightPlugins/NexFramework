/*
 * Copyright (c) 2026 Nex Studios
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.nexstudios.framework.paper;

import io.nexstudios.framework.core.NexFramework;
import io.nexstudios.framework.core.key.NexKey;
import io.nexstudios.framework.paper.di.PaperInternalServicesModule;
import io.nexstudios.framework.paper.di.PaperPlatformBindingsModule;
import io.nexstudios.framework.paper.services.ServiceListener;
import io.nexstudios.framework.paper.services.thirdparty.HookService;
import io.nexstudios.framework.paper.services.thirdparty.mythicmobs.MythicMobsService;
import io.nexstudios.framework.paper.services.thirdparty.mythicmobs.MythicMobsServices;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The {@code NexPaperPlugin} class serves as the base implementation for a plugin
 * within the NexFramework environment. It provides essential methods and lifecycle
 * hooks to manage services, listeners, commands, and other core components required
 * for the plugin's operation. This class is designed to simplify the process of
 * plugin development by offering a structured framework and consistent behavior
 * across various lifecycle stages.
 *
 * <p>The class includes a variety of lifecycle-related methods such as loading,
 * enabling, disabling, and initialization of services and components. It also
 * offers utility methods for creating keys, accessing frameworks, and dynamically
 * managing listeners and commands.
 *
 * <p>Subclasses of {@code NexPaperPlugin} can override protected methods to provide
 * custom behavior or to extend the existing functionality while adhering to the
 * foundational requirements defined by the NexFramework.
 */
public abstract class NexPaperPlugin extends JavaPlugin {

  private final List<Listener> pendingListenerInstances = new CopyOnWriteArrayList<>();
  private final List<Class<? extends ServiceListener>> pendingListenerTypes = new CopyOnWriteArrayList<>();

  private final NexFramework core = new NexFramework() {

    @Override
    public String name() {
      return NexPaperPlugin.this.getPluginMeta().getName();
    }

    /**
     * Configures and registers services required for the plugin's execution.
     * This method initializes and binds essential services, ensuring their readiness
     * and proper interaction within the plugin's lifecycle. It also delegates additional
     * custom service configurations to the plugin's specific implementation.
     *
     * <p>This method is responsible for registering platform-bound services such as
     * {@code PaperPluginService}, {@code DataFolderService}, and {@code ResourceService}.
     * Binding ensures that these services can operate with the appropriate context,
     * such as a plugin instance, data folder path, or class loader.</p>
     *
     * <p>Once these foundational services are registered and bound, dependent services
     * and custom service configurations can be safely initiated to extend functionality.</p>
     *
     * @param services the service accessor to register, retrieve, and bind required services
     */
    @Override
    protected void configureServices(ServiceAccessor services) {
      // Bind-first: register + bind platform-bound services before any dependent services are registered/instantiated
      services.install(new PaperPlatformBindingsModule(NexPaperPlugin.this));

      // Now it is safe to register services that depend on ResourceService/DataFolderService
      services.install(new PaperInternalServicesModule());

      // plugin hook for additional registrations
      NexPaperPlugin.this.configureServices(services);
    }

    /**
     * Starts the plugin by initializing its internal components and performing
     * any necessary pre-runtime setup operations.
     *
     * <p>This method delegates to {@code NexPaperPlugin.this.startInternal()}, which contains
     * the core logic for starting the plugin. It is responsible for ensuring that
     * the plugin is ready to function properly within its intended environment.</p>
     *
     * <p>This method is typically invoked as part of the plugin lifecycle management and
     * should not be called directly by external code.</p>
     */
    @Override
    protected void start() {
      NexPaperPlugin.this.startInternal();
    }

    /**
     * Stops the plugin and performs necessary cleanup operations.
     *
     * <p>This method ensures that any running database-related services, such as
     * {@code DatabaseAsyncService} and {@code DatabaseService}, are shut down properly
     * before the plugin is fully stopped. It is essential to ensure that the plugin's
     * stop hooks run while the database is still accessible for handling final state
     * saves or necessary cleanup tasks.</p>
     *
     * <p>The method first attempts to shut down the {@code DatabaseAsyncService}, allowing
     * any pending async database operations to complete. After invoking the plugin's stop
     * hooks, the {@code DatabaseService} is shut down to finalize the database operations.</p>
     *
     * <p>Any exceptions occurring during the shutdown of {@code DatabaseService} are caught
     * and ignored to ensure the method execution continues without interruption.</p>
     */
    @Override
    protected void stop() {
      NexPaperPlugin.this.stop();
    }
  };

  /**
   * Registers the internal services required by the plugin. This method is responsible
   * for binding and configuring various service classes to their respective default
   * implementations, ensuring proper functionality during the plugin's runtime. It also
   * performs specific setup tasks such as registering entities with related services.
   *
   * <p>The services registered by this method include several core utilities like command
   * handling, file reading, language management, component handling, database services,
   * and user locale storage. Additionally, it handles the registration of the
   * {@code PlayerLocaleEntity} within the Hibernate entity registry.
   *
   * @param services the {@code ServiceAccessor} instance providing access to the plugin's
   *                 service registry, used to bind and retrieve service implementations
   */
  protected void registerPaperInternalServices(ServiceAccessor services) {
    services.install(new PaperInternalServicesModule());
  }

  /**
   * Configures and registers necessary services for the plugin's operation.
   * This method provides an opportunity to bind or set up any additional services
   * required by the plugin during initialization. It ensures all services are
   * properly configured, initialized, and made available through the provided
   * {@link ServiceAccessor}.
   *
   * <p>The implementation of this method should handle registration and binding
   * of any additional platform-specific or plugin-specific services that are
   * required. This may include setting up dependencies or preparing resources
   * required by the plugin's functionality.
   *
   * @param services the {@code ServiceAccessor} instance that provides access to
   *                 the service registry used by the plugin
   */
  protected void configureServices(ServiceAccessor services) { }

  /**
   * Executes the loading logic for the plugin. This method is invoked during the plugin's
   * initialization phase to prepare it for further lifecycle stages, such as enabling or
   * starting specific services, components, or event handling mechanisms.
   *
   * <p>The purpose of this method is to conduct necessary setup tasks that should occur
   * during the loading phase of the plugin but before its activation. These steps typically
   * include resource loading, configuration parsing, or setting up required state for
   * subsequent operations.
   *
   * <p>Subclasses may override this method to implement custom loading behavior as needed.
   * However, it is intended to be called only once during the plugin's lifecycle and before
   * {@code onEnable()} is triggered. Proper use of this method ensures a consistent and
   * predictable initialization sequence.
   */
  protected void load() { }

  /**
   * Initiates the execution or startup process for the implementing component.
   * <p>
   * This method is intended to be overridden by subclasses to define
   * specific startup logic. By default, this method does not perform any action.
   * </p>
   * <p>
   * Ensure that all necessary configurations or prerequisites are handled
   * before invoking this method, as it may trigger application-specific behaviors
   * or workflows.
   * </p>
   */
  protected void start() { }

  /**
   * Stops the current operation or process being executed by the implementing class.
   * <p>
   * This method is designed to perform any necessary cleanup or state changes to
   * halt the ongoing activity safely. Subclasses may override this method
   * to provide specific stop behavior.
   */
  protected void stop() { }

  /**
   * Registers one or more {@link Listener} objects for handling events in the plugin.
   * If the plugin is enabled at the time of this method call, the provided listeners
   * are immediately registered with the server's {@code PluginManager}.
   * If the plugin is not enabled, the listeners are added to a pending queue to be
   * registered later when the plugin is activated.
   *
   * <p>This method ensures that event listeners are managed consistently across
   * the plugin's lifecycle, providing support for both immediate and deferred
   * registration. This is particularly useful for managing listeners dynamically
   * or during plugin initialization.
   *
   * @param listeners an array of {@code Listener} objects to be registered;
   *                  must not be {@code null}
   */
  protected final void registerListeners(Listener... listeners) {
    var pm = getServer().getPluginManager();

    if (isEnabled()) {
      for (var listener : listeners) {
        pm.registerEvents(listener, this);
      }
      return;
    }

    pendingListenerInstances.addAll(List.of(listeners));
  }

  /**
   * Registers one or more listener types to be managed by the plugin. If the plugin is currently
   * enabled, the specified listener types are immediately instantiated using the plugin's
   * {@code ServiceAccessor} and registered with the server's {@code PluginManager}. If the plugin
   * is not enabled, the listener types are added to a queue to be processed later.
   *
   * <p>Each listener type must be a concrete class that implements the {@code ServiceListener}
   * interface. The listeners are dynamically created and registered to handle relevant events
   * during the plugin's lifecycle.
   *
   * @param listenerTypes the array of {@code Class} objects representing the listener types to be
   *                      registered, each extending {@code ServiceListener}; must not be null or empty
   */
  @SafeVarargs
  protected final void registerListenerTypes(Class<? extends ServiceListener>... listenerTypes) {
    if (isEnabled()) {
      var pm = getServer().getPluginManager();
      for (var type : listenerTypes) {
        Listener listener = services().create(type);
        pm.registerEvents(listener, this);
      }
      return;
    }

    pendingListenerTypes.addAll(List.of(listenerTypes));
  }

  /**
   * Retrieves a list of listener classes that implement the ServiceListener interface.
   * <p>
   * This method returns a collection of classes that may be used to handle
   * specific service events or perform custom logic when certain conditions
   * are met in the application.
   *
   * @return a list of classes extending the ServiceListener interface.
   */
  protected List<Class<? extends ServiceListener>> listeners() {
    return List.of();
  }

  @Override
  public final void onLoad() {
    load();
  }

  @Override
  public final void onEnable() {
    core.boot();
  }

  @Override
  public final void onDisable() {
    core.shutdown();
  }

  public final ServiceAccessor services() {
    return core.services();
  }

  public final NexFramework framework() {
    return core;
  }

  public final NexKey createKey(String key) {
    return NexKey.ofOwnerName(getPluginMeta().getName(), key);
  }

  private void startInternal() {
    var pm = getServer().getPluginManager();

    for (var type : listeners()) {
      Listener listener = services().create(type);
      pm.registerEvents(listener, this);
    }

    for (var type : pendingListenerTypes) {
      Listener listener = services().create(type);
      pm.registerEvents(listener, this);
    }

    pendingListenerTypes.clear();

    for (var listener : pendingListenerInstances) {
      pm.registerEvents(listener, this);
    }

    pendingListenerInstances.clear();

    // try registering third party services
    registerHookServices();

    start();
  }

  private void registerHookServices() {
    HookService hooks = services().getService(HookService.class);
    if (!hooks.isServiceAvailable(MythicMobsService.class) && hooks.isPluginEnabled("MythicMobs")) {
      MythicMobsServices.register(services());
    }
  }
}