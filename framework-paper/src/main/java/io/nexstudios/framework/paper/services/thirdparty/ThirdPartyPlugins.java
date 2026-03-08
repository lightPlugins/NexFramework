package io.nexstudios.framework.paper.services.thirdparty;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.Objects;

/**
 * Small helpers for third-party plugin presence checks.
 *
 * Design goal:
 * - No Noop services
 * - Fail fast when a plugin tries to register an integration but the dependency isn't enabled
 */
public final class ThirdPartyPlugins {

  private ThirdPartyPlugins() {}

  public static Plugin requireEnabled(String pluginName) {
    return requireEnabled(Bukkit.getPluginManager(), pluginName);
  }

  public static Plugin requireEnabled(PluginManager pluginManager, String pluginName) {
    Objects.requireNonNull(pluginManager, "pluginManager");
    Objects.requireNonNull(pluginName, "pluginName");

    Plugin plugin = pluginManager.getPlugin(pluginName);
    if (plugin == null) {
      throw new IllegalStateException(
          "Required third-party plugin is missing: '" + pluginName + "'. " +
              "If you register this integration, add it as depend/softdepend in plugin.yml."
      );
    }

    if (!plugin.isEnabled()) {
      throw new IllegalStateException(
          "Required third-party plugin is present but not enabled yet: '" + pluginName + "'. " +
              "Register this integration only after the plugin is enabled (usually fine with 'depend')."
      );
    }

    return plugin;
  }
}