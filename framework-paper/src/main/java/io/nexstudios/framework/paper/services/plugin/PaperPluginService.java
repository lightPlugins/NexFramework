package io.nexstudios.framework.paper.services.plugin;

import io.nexstudios.framework.paper.NexPaperPlugin;
import io.nexstudios.serviceregistry.di.Service;

public interface PaperPluginService extends Service {

  NexPaperPlugin plugin();
}