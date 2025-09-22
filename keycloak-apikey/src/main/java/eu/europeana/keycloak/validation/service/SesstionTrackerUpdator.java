package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.KeycloakUtils;
import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.SessionTracker;
import eu.europeana.keycloak.validation.util.Constants;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

public class SesstionTrackerUpdator implements BiFunction<String, SessionTracker, SessionTracker>,
    Serializable {
  AtomicReference<ErrorMessage> resultReference = new AtomicReference<>();

  public static final int PERSONAL_KEY_LIMIT = KeycloakUtils.getEnvInt(Constants.PERSONAL_KEY_RATE_LIMIT, Constants.DEFAULT_PERSONAL_KEY_RATE_LIMIT);
  public static final int PROJECT_KEY_LIMIT = KeycloakUtils.getEnvInt(Constants.PROJECT_KEY_RATE_LIMIT, Constants.DEFAULT_PROJECT_KEY_RATE_LIMIT);
  public static final int RATE_LIMIT_DURATION = KeycloakUtils.getEnvInt(Constants.SESSION_DURATION_FOR_RATE_LIMITING,
      Constants.DEFAULT_SESSION_DURATION_RATE_LIMIT);

  private String lastAccessDate;
  private String keyType;

  public SesstionTrackerUpdator(String lastAccessDate,String keyType) {
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
    SessionTracker tracker = (existingTracker != null) ? existingTracker : new SessionTracker(key, 0,
            lastAccessDate);
    //check the limits for allowed number of sessions for apikey (keycloak client) and get the validation result
    resultReference.set(validateAndUpdateSessionTracker(tracker));
    return tracker;
  }



  private ErrorMessage validateAndUpdateSessionTracker(SessionTracker tracker) {
    int updatedCount = tracker.getSessionCount() + 1;
    //check personal key limit
    if (Constants.PERSONAL_KEY.equals(keyType)) {
      if (updatedCount > PERSONAL_KEY_LIMIT) {
        return personalKeyLimitReachedMessage();
      }
      updateSessionTracker(tracker, PERSONAL_KEY_LIMIT, updatedCount);
    }
    //check project key limit
    if (Constants.PROJECT_KEY.equals(keyType)) {
      if (updatedCount > PROJECT_KEY_LIMIT) {
        projectKeyLimitReachedMessage();
      }
      updateSessionTracker(tracker, PROJECT_KEY_LIMIT, updatedCount);
    }
    return null;
  }


  private  void updateSessionTracker(SessionTracker tracker, int maxKeyLimit,
      int updatedCount) {
    tracker.setSessionCount(updatedCount);
    tracker.setLastAccessDate(Constants.FORMATTER.format(LocalDateTime.now()));
    if(updatedCount == maxKeyLimit && tracker.getLastRateLimitReachingTime() == null){
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

}