package eu.europeana.keycloak.validation.provider;


import eu.europeana.keycloak.timer.FixedRateTaskScheduler;
import eu.europeana.keycloak.KeycloakUtils;
import eu.europeana.keycloak.validation.service.ClearSessionTrackingCacheTask;
import eu.europeana.keycloak.utils.Constants;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Factory for creating instances of {@link CustomAdminResourceProvider}.
 * The provider ID is defined as "admin", indicating that this factory
 * will manage resources accessible via a path like "/realms/{realm}/admin/..."
 * It implements the {@link RealmResourceProviderFactory} interface, for extending realm-level REST APIs.
 */

public class CustomAdminResourceProviderFactory implements RealmResourceProviderFactory {

  public static final String PROVIDER_ID="admin";

  public static final int MINUTES_IN_HOUR = 60;
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
    FixedRateTaskScheduler scheduler = new FixedRateTaskScheduler(new ClearSessionTrackingCacheTask(),calculateInitialDelayInMillis(intervalMinutes),intervalMinutes);
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

  /**
   * The task execution time needs to be aligned such that it aligned with clock minute, in
   * clustered environment this is essential to avoid the task being registered at different times on each pod ,
   * thereby being run at different times by each pod.
   *  For Interval of 15 minutes aligned to clock time, the expectation is that task executed at each 15th clock minute e.g.3.15 am,3.30 am etc.
   *  e.g.
   *  Interval is 15 minutes and current time is 3.12 am , delay will be 3 minutes(180000 millis) and the next execution will be at 3.15 am.
   *  Interval is 10 minutes and current time is 3.10 am , delay will be 10 minutes(600000 millis) and the next execution will be at 3.20 am.
   *  For intervals which are more than 60 minutes the initial delay will be calculated considering the minutes of the interval duration.
   *  e.g. If interval is 72 minutes (1hr 12 minutes) and  current time is 3.10 am ,delay will be 2 minutes and next execution will be at 3.12 am
   * @param intervalInMinutes interval between 2 tasks in minutes
   * @return initial delay in milliseconds
   */
  public long calculateInitialDelayInMillis(int intervalInMinutes) {

    int interval = intervalInMinutes;
    //check valid interval value provided
    if (intervalInMinutes <= 0)
      return 0;
    //Consider the remaining minutes if the interval is more than minutes in an hour
    if (intervalInMinutes > MINUTES_IN_HOUR) {
      intervalInMinutes = (intervalInMinutes % MINUTES_IN_HOUR);
    }

    LocalDateTime now = getLocalTime();
    //Find start of current hour
    LocalDateTime startingHour = now.truncatedTo(ChronoUnit.HOURS);
    long minutesElapsed = Duration.between(startingHour, now).toMinutes();
    long intervalsElapsed = (minutesElapsed / intervalInMinutes) ;

    LocalDateTime nextScheduleTime = startingHour
            .plusMinutes((intervalsElapsed+1) * intervalInMinutes)
            .truncatedTo(ChronoUnit.MINUTES);

    long initialDelay = Duration.between(now,nextScheduleTime).toMillis();
    LOG.info((String.format("Current Time : %s Interval : %s Minutes. Initial delay : %s Millis. Scheduled execution time: %s"
            , now,interval,initialDelay,now.plusNanos(initialDelay * 1_000_000))));
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