package eu.europeana.keycloak;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

public class FixedRateTaskScheduler {
  private static final Logger LOG = Logger.getLogger(FixedRateTaskScheduler.class);
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final CustomScheduledTask task;
  private final int intervalMinutes;
  public FixedRateTaskScheduler(CustomScheduledTask task, int intervalMinutes) {
    this.task = task;
    this.intervalMinutes = intervalMinutes;
  }
  public void scheduleTask(KeycloakSessionFactory keycloakSessionFactory){
    LOG.info("Scheduling the task '"+task.getTaskName()+"' with interval "+intervalMinutes);
    scheduler.scheduleAtFixedRate(new ClusterAwareScheduledTaskRunner(keycloakSessionFactory,task,intervalMinutes*6000),
        calculateInitialDelay(intervalMinutes),intervalMinutes, TimeUnit.MINUTES);
  }
  private int calculateInitialDelay(int intervalMinutes) {
    LocalDateTime now = LocalDateTime.now();
    int currentMinute = now.getMinute();
    int minutesSinceLastInterval = currentMinute % intervalMinutes;
    int initialDelay = intervalMinutes - minutesSinceLastInterval;
    LOG.info("CurrentMinute- " + currentMinute+", MinutesSinceLastInterval- " + minutesSinceLastInterval+", InitialDelay- "+ initialDelay);
    return initialDelay;
  }

}