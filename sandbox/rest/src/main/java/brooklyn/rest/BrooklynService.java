package brooklyn.rest;

import brooklyn.rest.core.ApplicationManager;
import brooklyn.rest.core.LocationStore;
import brooklyn.rest.health.GeneralHealthCheck;
import brooklyn.rest.resources.ApplicationResource;
import brooklyn.rest.resources.EffectorResource;
import brooklyn.rest.resources.EntityResource;
import brooklyn.rest.resources.LocationResource;
import brooklyn.rest.resources.SensorResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Environment;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class BrooklynService extends Service<BrooklynConfiguration> {

  protected BrooklynService() {
    super("brooklyn-rest");
  }

  @Override
  protected void initialize(BrooklynConfiguration configuration, Environment environment)
      throws Exception {

    // Create managed components and wire them together

    LocationStore locationStore = new LocationStore(configuration);
    environment.manage(locationStore);

    // TODO configure executor from application config file
    ExecutorService managedExecutor = environment.managedExecutorService("brooklyn", 2, 16, 30, TimeUnit.SECONDS);
    ApplicationManager applicationManager = new ApplicationManager(configuration, locationStore, managedExecutor);
    environment.manage(applicationManager);

    // Setup REST endpoints

    environment.addResource(new LocationResource(locationStore));

    EntityResource entityResource = new EntityResource();
    environment.addResource(entityResource);

    environment.addResource(new ApplicationResource(applicationManager, locationStore, entityResource));

    environment.addResource(new SensorResource(applicationManager));
    environment.addResource(new EffectorResource(applicationManager, managedExecutor));

    environment.addHealthCheck(new GeneralHealthCheck());
  }

  public static void main(String[] args) throws Exception {
    new BrooklynService().run(args);
  }
}
