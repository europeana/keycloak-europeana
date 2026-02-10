package eu.europeana.keycloak.timer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.scheduled.ClusterAwareScheduledTaskRunner;

/**
 * Scheduler for running the tasks which are aligned to clock minute which is important for
 * executions on the clustered environment to avoid task being run at different times on different pods. *
 */

public class FixedRateTaskScheduler {
  private static final Logger LOG = Logger.getLogger(FixedRateTaskScheduler.class);
  public static final long MILLISECONDS_IN_A_MINUTE = 60000L;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final AbstractCustomScheduledTask task;
  private final long intervalMinutes;
  private final long initialdelay;

  /**
   * Initialize FixedRateTaskScheduler with task and interval
   * @param task to be scheduled
   * @param intervalMinutes between 2 consecutive task executions
   */
  public FixedRateTaskScheduler(AbstractCustomScheduledTask task,long initialdelay,int intervalMinutes) {
    this.task = task;
    this.initialdelay = initialdelay;
    this.intervalMinutes = intervalMinutes;
  }

  /**
   * Schedule the task with delay.
   * @param keycloakSessionFactory required by {@code ClusterAwareScheduledTaskRunner}
   */
  public void scheduleTask(KeycloakSessionFactory keycloakSessionFactory){
    LOG.info("Scheduling the task '"+ task.getTaskName() +"' with interval of "+intervalMinutes +" minutes and initial delay of "+ initialdelay +" milli seconds");
    scheduler.scheduleAtFixedRate(
            new ClusterAwareScheduledTaskRunner(keycloakSessionFactory, task, intervalMinutes * MILLISECONDS_IN_A_MINUTE),
            initialdelay,
            intervalMinutes * MILLISECONDS_IN_A_MINUTE,
            TimeUnit.MILLISECONDS);
  }

}