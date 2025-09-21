package eu.europeana.keycloak.timer;

import java.time.LocalDateTime;
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
  public static final int MINUTES_IN_HOUR = 60;
  public static final long MILLISECONDS_IN_A_MINUTE = 6000L;
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final CustomScheduledTask task;
  private final int intervalMinutes;

  /**
   * Initialize FixedRateTaskScheduler with task and interval
   * @param task to be scheduled
   * @param intervalMinutes between 2 consecutive task executions
   */
  public FixedRateTaskScheduler(CustomScheduledTask task, int intervalMinutes) {
    this.task = task;
    this.intervalMinutes = intervalMinutes;
  }

  /**
   * Schedule the task with delay.
   * @param keycloakSessionFactory required by {@code ClusterAwareScheduledTaskRunner}
   */
  public void scheduleTask(KeycloakSessionFactory keycloakSessionFactory){
    LOG.info("Scheduling the task '"+task.getTaskName()+"' with interval "+intervalMinutes);
    scheduler.scheduleAtFixedRate(new ClusterAwareScheduledTaskRunner(keycloakSessionFactory,task,intervalMinutes * MILLISECONDS_IN_A_MINUTE),
        calculateInitialDelay(intervalMinutes),intervalMinutes, TimeUnit.MINUTES);
  }

  /**
   * The task execution time needs to be aligned such that it aligned with clock minute, in
   * clustered environment this is essential to avoid the task being registered at different times on each pod ,
   * thereby being run at different times by each.
   *  For Interval of 15 minutes aligned to clock time, the expectation is that task executed at each 15th clock minute e.g.3.15 am,3.30 am etc.
   *  e.g. If interval is 15 minutes and current time is 3.12 am , delay will be 3 minutes and the next execution will be at 3.15 am.
   *  For intervals which are more than 60 minutes the initial delay will be calculated considering the minutes of the interval duration.
   *  e.g. If interval is 72 minutes (1hr 12 minutes) and  current time is 3.10 am ,delay will be 2 minutes and next execution will be at 3.12 am
   * @param intervalMinutes interval between 2 tasks in minutes
   * @return initial delay in minutes
   */
  public int calculateInitialDelay(int intervalMinutes) {
    if (intervalMinutes <= 0)
      return 0;
    if (intervalMinutes > MINUTES_IN_HOUR) {
      intervalMinutes = (intervalMinutes % MINUTES_IN_HOUR);
    }
    int currentMinute = getLocalTime().getMinute();
    int minutesSinceLastInterval = currentMinute % intervalMinutes;
    int initialDelay = intervalMinutes - minutesSinceLastInterval;
    LOG.info("CurrentMinute- " + currentMinute + ", MinutesSinceLastInterval- "
        + minutesSinceLastInterval + ", InitialDelay- " + initialDelay);
    return initialDelay;
  }

  /** Separate method retrieving current dateTime
   *  to allow for unit tests to mock the different times.
   * @return current time
   */
  public LocalDateTime getLocalTime() {
    return LocalDateTime.now();
  }
}