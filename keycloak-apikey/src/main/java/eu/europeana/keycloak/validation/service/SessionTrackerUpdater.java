package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.RateLimitPolicy;
import eu.europeana.keycloak.validation.datamodel.SessionTracker;
import java.io.Serializable;
import java.util.function.BiFunction;
import org.apache.commons.lang3.StringUtils;

/**
 * Class to track sessions for clients/keys
 *
 * @author shweta
 */
public class SessionTrackerUpdater implements BiFunction<String, SessionTracker, SessionTracker>, Serializable {
    private final String currentDate;
    private final RateLimitPolicy rateLimitPolicy;

    public SessionTrackerUpdater(String currentDate, RateLimitPolicy rateLimitPolicy) {
        this.currentDate = currentDate;
        this.rateLimitPolicy = rateLimitPolicy;
    }

    /**
     * Method interacts with custom distributed cache 'sessionTrackerCache'
     * The number of sessions per time limit are validated , updated if required for the client.
     *
     * @param key             keycloak Client object
     * @param existingTracker Session tracker object for key
     * @return updated SessionTracker object
     */
    @Override
    public SessionTracker apply(String key, SessionTracker existingTracker) {
        SessionTracker tracker = (existingTracker != null) ? existingTracker :
                new SessionTracker(key, rateLimitPolicy.getQ(), currentDate);
        updateSessionTracker(tracker);
        return tracker;
    }

    /**
     * Decrements the session count by 1 and updates the access dates.<br>
     * Below is Behavior based on the new count for rate Limit - <br>
     * <ul>
     * <li>If greater than 0:  Updates count and last access date.</li>
     * <li>If exactly 0 (limit just exhausted):   Updates count, last access date, and sets the rate limit reaching time.</li>
     * <li>If less than 0 (limit already exhausted): sets the rate limit reaching time if not set for case when rate limit quota itself was 0 </li>
     * </ul>
     * @param tracker  session tracker object to update
     */
    private void updateSessionTracker(SessionTracker tracker){

        //Reduce Session Count by 1
        int updatedCount = tracker.getSessionCount() - 1;

        //If session count is 0 or more , update details on tracker object
        if( updatedCount > -1 ) {
            tracker.setSessionCount(updatedCount);
            tracker.setLastAccessDate(this.currentDate);
        }
        //If rateLimit gets exhausted, update  LastRateLimitReachingTime with lastAccessDate if not already.
        if (updatedCount <= 0 && StringUtils.isEmpty(tracker.getLastRateLimitReachingTime())) {
            tracker.setLastRateLimitReachingTime(tracker.getLastAccessDate());
        }
    }


}
