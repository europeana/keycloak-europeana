package eu.europeana.keycloak.utils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to list constants used by keycloak apikey module
 */
public final class Constants {

  private Constants() {
  }

  public static final String CLIENT_SCOPE_APIKEYS                          = "apikeys";
  public static final String ROLE_ATTRIBUTE_CREATION_DATE                  = "created";
  public static final String ROLE_ATTRIBUTE_LAST_ACCESS_DATE               = "lastAccess";
  public static final String ROLE_ATTRIBUTE_SCOPE                          = "scope";
  public static final String ROLE_ATTRIBUTE_SCOPE_INTERNAL                 = "internal";

  public static final String ROLE_ATTRIBUTE_LAST_RATELIMIT_REACHING_DATE   = "rateLimitReached";
  public static final String CREATION_DATE_PATTERN                         = "yyyy-MM-dd'T'HH:mm:ss'Z'";
  public static final String CLIENT_STATE_DISABLED                         = "disabled";
  public static final String PERSONAL                                      = "personal";
  public static final String PROJECT                                       = "project";
  public static final String PERSONAL_KEY                                  = "PersonalKey";
  public static final String PROJECT_KEY                                   = "ProjectKey";

  public static final String GRANT_TYPE_PASSWORD                           = "password";
  public static final String GRANT_TYPE_CLIENT_CRED                        = "client_credentials";
  public static final String APIKEY_NOT_REGISTERED                         = "API key %s is not registered";
  public static final String APIKEY_NOT_ACTIVE                             = "API key %s is not active";
  public static final String APIKEY_PATTERN                                = "APIKEY\\s+([^\\s]+)";
  public static final String CLIENT_OWNER                                  = "client_owner";
  public static final String SHARED_OWNER                                  = "shared_owner";
  public static final String CLIENT_OWNER_ROLE_DESCRIPTION                 = "Ownership of this client";
  public static final String SHARED_OWNER_ROLE_DESCRIPTION                 = "Shared ownership for this client (project keys)";
  public static final String PRIVATE_KEY_DESCRIPTION                       = "Private key for individual use only";
  public static final String ADMIN_ROLE_NAME                               = "admin";

  public static final String SESSION_TRACKER_CACHE                         = "sessionTrackerCache";
  public static final String SESSION_DURATION_FOR_RATE_LIMITING            = "SESSION_DURATION_FOR_RATE_LIMITING";
  public static final String PERSONAL_KEY_RATE_LIMIT                       = "PERSONAL_KEY_RATE_LIMIT";
  public static final String PROJECT_KEY_RATE_LIMIT                        = "PROJECT_KEY_RATE_LIMIT";
  public static final int DEFAULT_PROJECT_KEY_RATE_LIMIT                   = 10000;
  public static final int DEFAULT_PERSONAL_KEY_RATE_LIMIT                  = 1000;
  public static final int DEFAULT_SESSION_DURATION_RATE_LIMIT              = 60;

  public static final String RATE_LIMIT_POLICY_HEADER                      = "RateLimit-Policy";
  public static final String RATE_LIMIT_HEADER                             = "RateLimit";


  public static final DateTimeFormatter FORMATTER                          = DateTimeFormatter
                                                                              .ofPattern(CREATION_DATE_PATTERN)
                                                                              .withZone(ZoneOffset.UTC);

}