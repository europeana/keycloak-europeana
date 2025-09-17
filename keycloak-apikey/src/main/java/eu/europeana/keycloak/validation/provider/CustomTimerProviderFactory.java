package eu.europeana.keycloak.validation.provider;

import eu.europeana.keycloak.validation.service.CustomScheduledTask;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;


public class CustomTimerProviderFactory implements TimerProviderFactory {

  public static final long INTERVAL_MILLIS = 180000L;
  public static final String TASK_NAME = "clearSessionTrackingCache";

  private final Logger LOG = Logger.getLogger(CustomTimerProviderFactory.class);
  @Override
  public TimerProvider create(KeycloakSession keycloakSession) {
    return new CustomTimerProvider(keycloakSession);
  }
  private final String PROVIDER_ID="CustomTimerProvider" ;

  @Override
  public void init(Scope scope) {
  //NA
  }

  @Override
  public void postInit(KeycloakSessionFactory keycloakSessionFactory) {
    try(KeycloakSession session = keycloakSessionFactory.create()) {
      TimerProvider provider = session.getProvider(TimerProvider.class);
      LOG.info("Scheduling the cron");

      if (provider != null) {
        LOG.info("Found Provider " + provider.getClass().getName());
        provider.scheduleTask(new CustomScheduledTask(),INTERVAL_MILLIS, TASK_NAME);
      }
    }
  }

  @Override
  public void close() {
    //NA
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }
}