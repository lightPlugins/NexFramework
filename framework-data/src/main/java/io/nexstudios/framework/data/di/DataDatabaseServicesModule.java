package io.nexstudios.framework.data.di;

import io.nexstudios.framework.core.service.database.DatabaseAsyncService;
import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.framework.core.service.database.HibernateEntityRegistryService;
import io.nexstudios.framework.core.service.language.UserLocaleStore;
import io.nexstudios.framework.data.hibernate.DefaultHibernateEntityRegistry;
import io.nexstudios.framework.data.service.DefaultDatabaseAsyncService;
import io.nexstudios.framework.data.service.DefaultDatabaseService;
import io.nexstudios.framework.data.service.language.HibernateUserLocaleStore;
import io.nexstudios.framework.data.service.language.entity.PlayerLocaleEntity;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;

import java.util.Objects;

/**
 * The {@code DataDatabaseServicesModule} class is responsible for registering database-related services
 * and configuring the entity registry for the application.
 * <p>
 * This module integrates various database services, including both synchronous and asynchronous database access,
 * as well as user locale storage handling through Hibernate-backed implementations. Additionally, it registers
 * entities to be managed via the Hibernate entity registry.
 * <p>
 * This module is implemented as final and uses the {@link ServiceModule} interface to define its behavior
 * within a service-oriented architecture. It ensures proper initialization and registration of services
 * required for database operations.
 */
public final class DataDatabaseServicesModule implements ServiceModule {

  @Override
  public void install(ServiceAccessor services) {
    Objects.requireNonNull(services, "services");

    services.register(HibernateEntityRegistryService.class, DefaultHibernateEntityRegistry.class);
    services.getService(HibernateEntityRegistryService.class).register(PlayerLocaleEntity.class);

    services.register(DatabaseService.class, DefaultDatabaseService.class);
    services.register(DatabaseAsyncService.class, DefaultDatabaseAsyncService.class);

    services.register(UserLocaleStore.class, HibernateUserLocaleStore.class);
  }
}