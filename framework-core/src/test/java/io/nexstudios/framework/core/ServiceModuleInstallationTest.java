package io.nexstudios.framework.core;

import io.nexstudios.serviceregistry.DefaultServiceRegistry;
import io.nexstudios.serviceregistry.di.Service;
import io.nexstudios.serviceregistry.di.ServiceAccessor;
import io.nexstudios.serviceregistry.di.ServiceModule;
import io.nexstudios.serviceregistry.di.ServiceOwner;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

final class ServiceModuleInstallationTest {

  interface UserService extends Service {
    String userName();
  }

  interface BillingService extends Service {
    int balance();
  }

  static final class DefaultUserService implements UserService {
    @Override public String userName() { return "alice"; }
  }

  static final class DefaultBillingService implements BillingService {
    @Override public int balance() { return 42; }
  }

  static final class UserModule implements ServiceModule {
    @Override
    public void install(ServiceAccessor services) {
      Objects.requireNonNull(services, "services");
      services.register(UserService.class, DefaultUserService.class);
      services.register(BillingService.class, DefaultBillingService.class);
    }
  }

  @Test
  void install_module_registers_multiple_services_and_resolves_them() {
    ServiceOwner owner = () -> "test-owner";
    ServiceAccessor services = new ServiceAccessor(new DefaultServiceRegistry(), owner);

    services.install(new UserModule());

    UserService user = services.getService(UserService.class);
    BillingService billing = services.getService(BillingService.class);

    assertEquals("alice", user.userName());
    assertEquals(42, billing.balance());
  }
}