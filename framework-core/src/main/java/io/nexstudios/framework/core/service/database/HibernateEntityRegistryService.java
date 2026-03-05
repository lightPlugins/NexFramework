package io.nexstudios.framework.core.service.database;

import io.nexstudios.serviceregistry.di.Service;

import java.util.Set;

public interface HibernateEntityRegistryService extends Service {

  void register(Class<?> entityClass);

  Set<Class<?>> entities();
}