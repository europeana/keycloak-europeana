package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.RateLimit;
import eu.europeana.keycloak.validation.datamodel.RateLimitPolicy;
import eu.europeana.keycloak.validation.datamodel.SessionTracker;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static eu.europeana.keycloak.utils.Constants.*;

/**
 * Class to track sessions for clients/keys
 *
 * @author shweta
 */
public class SessionTrackerUpdater implements BiFunction<String, SessionTracker, SessionTracker>, Serializable {

    AtomicReference<ErrorMessage> resultReference = new AtomicReference<>();
    AtomicReference<RateLimit> rateLimitReference = new AtomicReference<>();

    private final String lastAccessDate;
    private final String keyType;
    private final RateLimitPolicy rateLimitPolicy;

    public SessionTrackerUpdater(String lastAccessDate, String keyType, RateLimitPolicy rateLimitPolicy) {
        this.lastAccessDate = lastAccessDate;
        this.keyType = keyType;
        this.rateLimitPolicy = rateLimitPolicy;
    }

    /**
     * Method interacts with custom distributed cache 'sessionTrackerCache'
     * The number of sessions per time limit are validated , updated if required for the client.
     *
     * @param key             keycloak Client object
     * @param existingTracker Sessiontracker object for key
     * @return updated SessionTracker object
     */
    @Override
    public SessionTracker apply(String key, SessionTracker existingTracker) {
        //Null check - for handling during keycloak pod rebalancing
        Integer quota = (rateLimitPolicy!=null) ? rateLimitPolicy.getQ():0;
        String vendorIdentifier = (rateLimitPolicy!=null)? rateLimitPolicy.getVendorIdentifier():CUSTOM;


        SessionTracker tracker = (existingTracker != null) ? existingTracker :
                new SessionTracker(key, quota, lastAccessDate);
        //check the limits for allowed number of sessions for apikey (keycloak client) and get the validation result

        resultReference.set(validateAndUpdateSessionTracker(tracker, vendorIdentifier,quota));
        return tracker;
    }


    /**
     * Updates the session tracker object with the session counts
     * NOTE : the best would be to change it in the infinispan instead of counting up,
     * to decrease from the limit so that the limit could be initialized every hour
     * with either a predefined limit or a custom limit defined for that client.
     *
     * @param tracker object to update
     * @return error message if any
     */
    private ErrorMessage validateAndUpdateSessionTracker(SessionTracker tracker,String policyType,int rateLimit) {
        int updatedCount = tracker.getSessionCount() - 1;
        LocalDateTime lastAccessDate = LocalDateTime.now();

        // check if the client has exhausted the limit
        if (updatedCount == -1) {
            rateLimitReference.set(new RateLimit(policyType, 0, getRemainingTimeUtilReset(lastAccessDate)));
            return PROJECT.equals(keyType) ? projectKeyLimitReachedMessage(rateLimit) : personalKeyLimitReachedMessage(rateLimit);
        }
        // if the key is NOT exhausted, update tracker and rate limit
        updateSessionTracker(tracker, updatedCount, lastAccessDate);
        rateLimitReference.set(new RateLimit(policyType, updatedCount, getRemainingTimeUtilReset(lastAccessDate)));

        return null;
    }


    /**
     * Update the session tracker with the session count, lastAccessDate and
     * if the limit is reached (usage is exhausted) updates the LastRateLimitReachingTime
     *
     * @param tracker      existing tracker
     * @param updatedCount updated count
     */
    private void updateSessionTracker(SessionTracker tracker, int updatedCount, LocalDateTime lastAccessDate) {
        tracker.setSessionCount(updatedCount);
        tracker.setLastAccessDate(FORMATTER.format(lastAccessDate));
        if (updatedCount == 0 && StringUtils.isEmpty(tracker.getLastRateLimitReachingTime())) {
            tracker.setLastRateLimitReachingTime(tracker.getLastAccessDate());
        }
    }

    private ErrorMessage projectKeyLimitReachedMessage(int limit) {
        return ErrorMessage.LIMIT_PROJECT_KEYS_429.formatError(String.valueOf(limit), String.valueOf(RATE_LIMIT_DURATION));
    }

    private ErrorMessage personalKeyLimitReachedMessage(int limit) {
        return ErrorMessage.LIMIT_PERSONAL_KEYS_429.formatError(String.valueOf(limit),
                        String.valueOf(RATE_LIMIT_DURATION))
                .formatErrorMessage(String.valueOf(limit), String.valueOf(RATE_LIMIT_DURATION));
    }

    /**
     * Calculates remaining time in second
     *
     * @param dateTime last access time
     * @return time reamining till the new quota is assigned
     */
    private long getRemainingTimeUtilReset(LocalDateTime dateTime) {
        int minutesElapsed = dateTime.getMinute() % RATE_LIMIT_DURATION;
        if (minutesElapsed == 0) {
            return ((RATE_LIMIT_DURATION * 60L) - dateTime.getSecond());  // totalSeconds - seconds elapsed (for the new time window slot)
        } else {
            return (((RATE_LIMIT_DURATION - minutesElapsed) * 60L) - dateTime.getSecond()); // remaining seconds - seconds elapsed
        }
    }
}