package io.nexstudios.framework.data.service;

import io.nexstudios.framework.core.service.database.DatabaseAsyncService;
import io.nexstudios.framework.core.service.database.DatabaseService;
import io.nexstudios.serviceregistry.di.Dependencies;
import io.nexstudios.serviceregistry.di.ServiceAccessor;

import jakarta.persistence.EntityManager;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

@Dependencies({
    DatabaseService.class
})
public final class DefaultDatabaseAsyncService implements DatabaseAsyncService {

  private static final Logger logger = Logger.getLogger(DefaultDatabaseAsyncService.class.getName());

  private static final Duration DEFAULT_SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

  private final DatabaseService databaseService;

  private final Object lifecycleLock = new Object();

  private volatile ExecutorService databaseExecutorService;

  public DefaultDatabaseAsyncService(ServiceAccessor serviceAccessor) {
    Objects.requireNonNull(serviceAccessor, "serviceAccessor");
    this.databaseService = serviceAccessor.getService(DatabaseService.class);
  }

  @Override
  public void start() {
    ExecutorService local = databaseExecutorService;
    if (local != null) {
      return;
    }

    synchronized (lifecycleLock) {
      if (databaseExecutorService != null) {
        return;
      }

      databaseExecutorService = Executors.newVirtualThreadPerTaskExecutor();
    }
  }

  /**
   * Graceful shutdown with timeout:
   * 1) stop accepting new tasks
   * 2) wait up to DEFAULT_SHUTDOWN_TIMEOUT for running tasks to complete
   * 3) if still running, interrupt/cancel via shutdownNow()
   */
  @Override
  public void shutdown() {
    shutdown(DEFAULT_SHUTDOWN_TIMEOUT);
  }

  /**
   * Same as {@link #shutdown()} but with explicit timeout.
   */
  public void shutdown(Duration shutdownTimeout) {
    Objects.requireNonNull(shutdownTimeout, "shutdownTimeout");

    ExecutorService localExecutorService;
    synchronized (lifecycleLock) {
      localExecutorService = databaseExecutorService;
      databaseExecutorService = null;
    }

    if (localExecutorService == null) {
      return;
    }

    localExecutorService.shutdown();

    boolean terminated = false;
    try {
      long timeoutMillis = Math.max(0L, shutdownTimeout.toMillis());
      terminated = localExecutorService.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException interruptedException) {
      Thread.currentThread().interrupt();
    }

    if (!terminated) {
      logger.log(
          Level.WARNING,
          "Database async executor did not terminate within {0}ms. Forcing shutdownNow().",
          Math.max(0L, shutdownTimeout.toMillis())
      );

      localExecutorService.shutdownNow();

      boolean terminatedAfterForce = false;
      try {
        terminatedAfterForce = localExecutorService.awaitTermination(250L, TimeUnit.MILLISECONDS);
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      }

      if (!terminatedAfterForce) {
        logger.log(
            Level.WARNING,
            "Database async executor still did not terminate after shutdownNow(). " +
                "Some tasks may be stuck in driver/network code and may prevent a clean shutdown."
        );
      }
    }
  }

  @Override
  public Executor executor() {
    ExecutorService local = databaseExecutorService;
    if (local == null) {
      throw new IllegalStateException(
          "DatabaseAsyncService is not started yet. Ensure DatabaseAsyncService.start() is called during platform startup."
      );
    }
    return local;
  }

  @Override
  public <T> CompletableFuture<T> executeAsyncInTransaction(Function<EntityManager, T> work) {
    Objects.requireNonNull(work, "work");
    return CompletableFuture.supplyAsync(
        () -> databaseService.executeInTransaction(work),
        executor()
    );
  }

  @Override
  public CompletableFuture<Void> executeAsyncInTransaction(Consumer<EntityManager> work) {
    Objects.requireNonNull(work, "work");
    return CompletableFuture.runAsync(
        () -> databaseService.executeInTransaction(work),
        executor()
    );
  }
}