package io.nexstudios.framework.paper.services.plugin;

import io.nexstudios.framework.paper.NexPaperPlugin;

import java.util.Objects;

public final class DefaultPaperPluginService implements PaperPluginService {

  private volatile NexPaperPlugin plugin;

  public void bind(NexPaperPlugin plugin) {
    this.plugin = Objects.requireNonNull(plugin, "plugin");
  }

  @Override
  public NexPaperPlugin plugin() {
    NexPaperPlugin p = plugin;
    if (p == null) {
      throw new IllegalStateException("PaperPluginService is not bound yet.");
    }
    return p;
  }
}