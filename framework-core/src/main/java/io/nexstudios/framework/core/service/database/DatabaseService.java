package io.nexstudios.framework.core.service.database;

import io.nexstudios.serviceregistry.di.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Platform-neutral database service (JPA-style public API).
 *
 * Lifecycle:
 * - call start() once (typically during platform start)
 * - call shutdown() on platform stop
 */
public interface DatabaseService extends Service {

  /**
   * Starts the underlying connection pool and JPA provider.
   *
   * This method should be idempotent.
   */
  void start();

  /**
   * Shuts down EntityManagerFactory/SessionFactory and HikariDataSource.
   *
   * This method should be idempotent.
   */
  void shutdown();

  /**
   * Public API: JPA EntityManagerFactory.
   */
  EntityManagerFactory entityManagerFactory();

  /**
   * Executes work inside a transaction using a fresh EntityManager.
   * The EntityManager is always closed after execution.
   */
  default <T> T executeInTransaction(Function<EntityManager, T> work) {
    Objects.requireNonNull(work, "work");

    EntityManagerFactory entityManagerFactory = entityManagerFactory();
    EntityManager entityManager = entityManagerFactory.createEntityManager();

    try {
      entityManager.getTransaction().begin();
      T result = work.apply(entityManager);
      entityManager.getTransaction().commit();
      return result;
    } catch (RuntimeException exception) {
      try {
        if (entityManager.getTransaction().isActive()) {
          entityManager.getTransaction().rollback();
        }
      } catch (RuntimeException rollbackException) {
        exception.addSuppressed(rollbackException);
      }
      throw exception;
    } finally {
      try {
        entityManager.close();
      } catch (RuntimeException closeException) {
        // best-effort close; do not mask the original exception
      }
    }
  }

  /**
   * Executes work inside a transaction using a fresh EntityManager.
   * The EntityManager is always closed after execution.
   */
  default void executeInTransaction(Consumer<EntityManager> work) {
    Objects.requireNonNull(work, "work");
    executeInTransaction(entityManager -> {
      work.accept(entityManager);
      return null;
    });
  }
}