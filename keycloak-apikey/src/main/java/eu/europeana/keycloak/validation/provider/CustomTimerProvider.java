package eu.europeana.keycloak.validation.provider;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;

public class CustomTimerProvider implements TimerProvider {

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


  private final KeycloakSession session;

  private final Logger LOG = Logger.getLogger(CustomTimerProvider.class);

  public CustomTimerProvider(KeycloakSession keycloakSession) {
    this.session = keycloakSession;
  }


  @Override
  public void schedule(Runnable runnable, long intervalMillis, String taskName) {
    LOG.info("Executing eu.europeana.keycloak.validation.provider.CustomTimerProvider.schedule  method");
    scheduler.scheduleAtFixedRate(runnable, 0L,intervalMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public void scheduleTask(ScheduledTask scheduledTask, long intervalMillis, String taskName) {
    LOG.info("Executing eu.europeana.keycloak.validation.provider.CustomTimerProvider.scheduleTask  method");
    ClusterAwareScheduledTaskRunner runner = new ClusterAwareScheduledTaskRunner(this.session.getKeycloakSessionFactory(),scheduledTask,intervalMillis);
    schedule(runner,intervalMillis,taskName);
  }

  @Override
  public TimerTaskContext cancelTask(String taskName) {
    return null;
  }

  @Override
  public void close() {

  }
}