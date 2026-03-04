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

  @Test
  void boot_twice_throws() {
    class TestFramework extends NexFramework {
      @Override public String name() { return "test"; }
    }

    var fw = new TestFramework();
    fw.boot();

    assertThrows(IllegalStateException.class, fw::boot);
    assertTrue(fw.isBooted());
  }

  @Test
  void boot_requires_nonNull_name() {
    class TestFramework extends NexFramework {
      @Override public String name() { return null; }
    }

    var fw = new TestFramework();
    assertThrows(NullPointerException.class, fw::boot);
    assertFalse(fw.isBooted());
  }

  @Test
  void boot_calls_registerInternalServices_before_configureServices() {
    class TestFramework extends NexFramework {
      boolean registered = false;

      @Override public String name() { return "test"; }

      @Override
      protected void registerInternalServices(ServiceAccessor services) {
        registered = true;
      }

      @Override
      protected void configureServices(ServiceAccessor services) {
        assertTrue(registered, "registerInternalServices() must run before configureServices()");
      }
    }

    new TestFramework().boot();
  }

  @Test
  void shutdown_calls_stop_only_after_boot_and_only_once() {
    class TestFramework extends NexFramework {
      int stops = 0;

      @Override public String name() { return "test"; }

      @Override
      protected void stop() {
        stops++;
      }
    }

    var fw = new TestFramework();

    fw.shutdown();
    assertEquals(0, fw.stops);

    fw.boot();
    fw.shutdown();
    fw.shutdown();
    assertEquals(1, fw.stops);
  }
}