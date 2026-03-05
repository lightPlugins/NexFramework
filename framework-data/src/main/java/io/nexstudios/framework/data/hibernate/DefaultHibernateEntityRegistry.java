package io.nexstudios.framework.data.hibernate;

import io.nexstudios.framework.core.service.database.HibernateEntityRegistryService;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class DefaultHibernateEntityRegistry implements HibernateEntityRegistryService {

  private final Set<Class<?>> entities = Collections.synchronizedSet(new LinkedHashSet<>());

  @Override
  public void register(Class<?> entityClass) {
    entities.add(Objects.requireNonNull(entityClass, "entityClass"));
  }

  @Override
  public Set<Class<?>> entities() {
    synchronized (entities) {
      return Set.copyOf(entities);
    }
  }
}