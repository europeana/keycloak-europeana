package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.timer.FixedRateTaskScheduler;
import eu.europeana.keycloak.KeycloakUtils;
import eu.europeana.keycloak.validation.service.ClearSessionTrackingCacheTask;
import eu.europeana.keycloak.validation.util.Constants;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for creating instances of {@link CustomAdminResourceProvider}.
 * The provider ID is defined as "admin", indicating that this factory
 * will manage resources accessible via a path like "/realms/{realm}/admin/..."
 * It implements the {@link RealmResourceProviderFactory} interface, for extending realm-level REST APIs.
 */

public class CustomAdminResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="admin";
  private static final Logger LOG = Logger.getLogger(CustomAdminResourceProviderFactory.class);
  @Override
  public RealmResourceProvider create(KeycloakSession keycloakSession) {
    return new CustomAdminResourceProvider(keycloakSession);
  }

  @Override
  public void init(Scope scope) {
    //specific init actions not required e.g. reading,parsing configuration parameters,establishing initial connections to external resources
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    int intervalMinutes = KeycloakUtils.getEnvInt(Constants.SESSION_DURATION_FOR_RATE_LIMITING,
        Constants.DEFAULT_SESSION_DURATION_RATE_LIMIT);

    LOG.info("Configured Session Duration - "+ intervalMinutes);
    if(intervalMinutes < 0 || intervalMinutes > Constants.DEFAULT_SESSION_DURATION_RATE_LIMIT) {
      LOG.info("Session duration not valid, defaulting to " +  Constants.DEFAULT_SESSION_DURATION_RATE_LIMIT + " minutes");
    }

    //schedule the cache cleanup task
    FixedRateTaskScheduler scheduler = new FixedRateTaskScheduler(new ClearSessionTrackingCacheTask(),intervalMinutes);
    scheduler.scheduleTask(keycloakSessionFactory);
  }

  @Override
  public void close() {
    //specific implementation not required . e.g resource or connection cleanup
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

}