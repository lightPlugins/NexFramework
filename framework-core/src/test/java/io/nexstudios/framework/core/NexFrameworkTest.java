package io.nexstudios.framework.core;

import io.nexstudios.serviceregistry.di.ServiceAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class NexFrameworkTest {

  @Test
  void boot_calls_configure_then_start_in_order() {
    class TestFramework extends NexFramework {
      boolean configured = false;
      boolean started = false;

      @Override
      public String name() {
        return "test";
      }

      @Override
      protected void configureServices(ServiceAccessor services) {
        assertFalse(started);
        configured = true;
      }

      @Override
      protected void start() {
        assertTrue(configured);
        started = true;
      }
    }

    var fw = new TestFramework();
    fw.boot();
    assertTrue(fw.isBooted());
  }

  @Test
  void shutdown_is_idempotent() {
    class TestFramework extends NexFramework {
      @Override public String name() { return "test"; }
    }

    var fw = new TestFramework();
    fw.shutdown();
    fw.boot();
    fw.shutdown();
    fw.shutdown();
    assertFalse(fw.isBooted());
  }

  @Test
  void services_accessor_is_available() {
    class TestFramework extends NexFramework {
      @Override public String name() { return "test"; }
    }

    var fw = new TestFramework();
    assertNotNull(fw.services());
    assertNotNull(fw.registry());
  }
}