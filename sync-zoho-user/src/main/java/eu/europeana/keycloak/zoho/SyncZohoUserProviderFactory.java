package eu.europeana.keycloak.zoho;

import eu.europeana.keycloak.timer.FixedRateTaskScheduler;
import eu.europeana.keycloak.zoho.timer.ZohoSyncTask;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;
import java.time.*;
import java.time.format.DateTimeFormatter;


/**
 * Created by luthien on 14/11/2022.
 */
public class SyncZohoUserProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "zohosync";
    private static final Logger LOG = Logger.getLogger(SyncZohoUserProviderFactory.class);
    public static final String ZOHO_SYNC_TIME_OF_DAY = "ZOHO_SYNC_TIME_OF_DAY";
    public static final String DEFAULT_SYNC_TIME = "02:00";
    public static final int MINUTES_PER_DAY = 24 * 60;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new SyncZohoUserProvider(session);
    }

    @Override
    public void init(Scope config) {
        //No specific implementation required
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        //Schedule the zoho sync task based on the configured time
        LOG.info("Scheduling Zoho-sync task !!");
        FixedRateTaskScheduler scheduler = new FixedRateTaskScheduler(new ZohoSyncTask(),calculateInitialDelayInMillis(MINUTES_PER_DAY), MINUTES_PER_DAY);
        scheduler.scheduleTask(factory);
    }

    private long calculateInitialDelayInMillis(int intervalMinutes) {
        //fetch the configured time for running the job
        ZonedDateTime now = getCurrentTime(ZoneOffset.UTC);
        ZonedDateTime nextScheduleTime = now.with(getConfiguredSyncTime());
        if(now.isAfter(nextScheduleTime)){
            nextScheduleTime = nextScheduleTime.plusDays(1);
        }
        long initialDelay = Duration.between(now,nextScheduleTime).toMillis();
        LOG.info((String.format("Current Time : %s Interval : %s Minutes. Initial delay : %s Millis. Scheduled execution time: %s"
                , now,intervalMinutes,initialDelay,nextScheduleTime)));
        return initialDelay;
    }

    private LocalTime getConfiguredSyncTime() {
        String zohoSyncTimeOfDay = System.getenv(ZOHO_SYNC_TIME_OF_DAY);
        if(StringUtils.isBlank(zohoSyncTimeOfDay)){
            LOG.warn("No specific time configured for running zoho sync. Defaulting to "+ DEFAULT_SYNC_TIME);
            zohoSyncTimeOfDay = DEFAULT_SYNC_TIME;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return LocalTime.parse(zohoSyncTimeOfDay,formatter);
    }

    /** Separate method retrieving current dateTime
     *  to allow for unit tests to mock the different times.
     * @return current time
     */
    public ZonedDateTime getCurrentTime(ZoneOffset offset) {
        return ZonedDateTime.now(offset);
    }

    @Override
    public void close() {
        //No specific implementation required
    }

}