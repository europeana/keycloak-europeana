package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.KeycloakUtils;
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
 * @author shweta
 */
public class SessionTrackerUpdater implements BiFunction<String, SessionTracker, SessionTracker>, Serializable {

  AtomicReference<ErrorMessage> resultReference    = new AtomicReference<>();
  AtomicReference<RateLimit>    rateLimitReference = new AtomicReference<>();

  public static final int PERSONAL_KEY_LIMIT = KeycloakUtils.getEnvInt(PERSONAL_KEY_RATE_LIMIT, DEFAULT_PERSONAL_KEY_RATE_LIMIT);
  public static final int PROJECT_KEY_LIMIT = KeycloakUtils.getEnvInt(PROJECT_KEY_RATE_LIMIT, DEFAULT_PROJECT_KEY_RATE_LIMIT);
  public static final int RATE_LIMIT_DURATION = KeycloakUtils.getEnvInt(SESSION_DURATION_FOR_RATE_LIMITING, DEFAULT_SESSION_DURATION_RATE_LIMIT);

  private String lastAccessDate;
  private String keyType;

  public SessionTrackerUpdater(String lastAccessDate, String keyType) {
    this.lastAccessDate = lastAccessDate;
    this.keyType =keyType;
  }


  /**
   * Method interact with custom distributed cache 'sessionTrackerCache'
   * The number of sessions per time limit are validated , updated if required for the client.
   * @param key keycloak Client object
   * @param existingTracker Sessiontracker object for key
   * @return  updated SessionTracker object
   */
  @Override
  public SessionTracker apply(String key, SessionTracker existingTracker) {
    SessionTracker tracker = (existingTracker != null) ? existingTracker :
            new SessionTracker(key, getSessionKeyLimit(keyType), lastAccessDate);
    //check the limits for allowed number of sessions for apikey (keycloak client) and get the validation result
    resultReference.set(validateAndUpdateSessionTracker(tracker));
    return tracker;
  }


  /**
   * Updates the session tracker object with the session counts
   * NOTE : the best would be to change the infinitespan to instead of counting up,
   *       to decrease from the limit so that the limit could be initialized every hour
   *       with either a predefined limit or a custom limit defined for that client
   * @param tracker
   * @return
   */
  private ErrorMessage validateAndUpdateSessionTracker(SessionTracker tracker) {
    int updatedCount = tracker.getSessionCount() - 1;
    LocalDateTime lastAccessDate = LocalDateTime.now();

    // check if the client has exhausted the limit
    if (updatedCount == -1) {
      rateLimitReference.set(new RateLimit(keyType, 0, getRemainingTimeUtilReset(lastAccessDate)));
      return PROJECT_KEY.equals(keyType) ? projectKeyLimitReachedMessage() : personalKeyLimitReachedMessage();
    }
    // if the key is NOT exhausted, update tracker and rate limit
    updateSessionTracker(tracker, updatedCount, lastAccessDate);
    rateLimitReference.set(new RateLimit(keyType, updatedCount, getRemainingTimeUtilReset(lastAccessDate)));

    return null;
  }


  /**
   * Update the session tracker with the session count, lastAccessDate and
   * if the limit is reached (usage is exhausted) updates the LastRateLimitReachingTime
   * @param tracker existing tracker
   * @param updatedCount updated count
   */
  private void updateSessionTracker(SessionTracker tracker, int updatedCount, LocalDateTime lastAccessDate) {
    tracker.setSessionCount(updatedCount);
    tracker.setLastAccessDate(FORMATTER.format(lastAccessDate));
    if (updatedCount == 0 && StringUtils.isEmpty(tracker.getLastRateLimitReachingTime())) {
      tracker.setLastRateLimitReachingTime(tracker.getLastAccessDate());
    }
  }

  private  ErrorMessage projectKeyLimitReachedMessage() {
    return ErrorMessage.LIMIT_PROJECT_KEYS_429.formatError(String.valueOf(PROJECT_KEY_LIMIT),
        String.valueOf(RATE_LIMIT_DURATION));
  }

  private  ErrorMessage personalKeyLimitReachedMessage() {
    return ErrorMessage.LIMIT_PERSONAL_KEYS_429.formatError(String.valueOf(PERSONAL_KEY_LIMIT),
            String.valueOf(RATE_LIMIT_DURATION))
        .formatErrorMessage(String.valueOf(PERSONAL_KEY_LIMIT),
            String.valueOf(RATE_LIMIT_DURATION));
  }


  /**
   * returns the rate limit policy for the key type
   * @param keyType type of the key
   * @return
   */
  public RateLimitPolicy getRateLimitPolicy(String keyType) {
    return new RateLimitPolicy(keyType, getSessionKeyLimit(keyType),RATE_LIMIT_DURATION * 60);
  }

  private int getSessionKeyLimit(String keyType) {
    switch (keyType) {
      case PERSONAL_KEY : return PERSONAL_KEY_LIMIT;
      case PROJECT_KEY  : return PROJECT_KEY_LIMIT;
      default: return 0;
    }
  }

  /**
   * Calculates remaining time in seconds
   * @param dateTime last access time
   * @return time reamining till the new quota is assigned
   */
  private long getRemainingTimeUtilReset(LocalDateTime dateTime) {
    return  (RATE_LIMIT_DURATION - dateTime.getMinute()) * 60 ;
  }

}