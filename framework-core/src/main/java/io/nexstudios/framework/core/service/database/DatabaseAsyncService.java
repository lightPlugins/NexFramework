package io.nexstudios.framework.core.service.database;

import io.nexstudios.serviceregistry.di.Service;
import jakarta.persistence.EntityManager;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DatabaseAsyncService extends Service {

  /**
   * Starts the async executor used for database work.
   * This method should be idempotent.
   */
  void start();

  /**
   * Shuts down the async executor used for database work.
   * This method should be idempotent.
   */
  void shutdown();

  /**
   * Executor used for async database work.
   * Must not run on platform main thread / event loop.
   */
  Executor executor();

  <T> CompletableFuture<T> executeAsyncInTransaction(Function<EntityManager, T> work);

  CompletableFuture<Void> executeAsyncInTransaction(Consumer<EntityManager> work);
}