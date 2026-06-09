package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.RateLimit;
import eu.europeana.keycloak.validation.datamodel.RateLimitPolicy;
import eu.europeana.keycloak.validation.datamodel.SessionTracker;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

import static eu.europeana.keycloak.utils.Constants.*;

/**
 * Provides operations for apikey validation.
 */
public class ApiKeyValidationService {
    private static final Logger LOG = Logger.getLogger(ApiKeyValidationService.class);
    private final KeycloakSession session;
    private final RealmModel realm;

    /**
     * Initialize the ApiKeyValidationService using keycloak session
     *
     * @param session Keycloak session
     */
    public ApiKeyValidationService(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    /**
     * Method authenticates the bearer token input using the keycloack auth manager
     *
     * @param tokenString in form 'Bearer <TOKEN_VALUE>'
     * @param grantType   type of the token,it can be for client_credentials or password based
     * @return ValidationResult
     */
    public ValidationResult authorizeToken(String tokenString, String grantType) {
        //As we want to send different message when the token is inactive , first get the token verified without the ifActive check.
        AuthResult authResult = AuthenticationManager.verifyIdentityToken(
                this.session, this.realm, session.getContext().getUri(),
                session.getContext().getConnection(), false, true, null, false,
                tokenString, session.getContext().getRequestHeaders());

        if (authResult == null || authResult.getClient() == null) {
            return new ValidationResult(Status.UNAUTHORIZED, ErrorMessage.TOKEN_INVALID_401);
        }
        if (!authResult.getToken().isActive()) {
            return new ValidationResult(Status.UNAUTHORIZED, ErrorMessage.TOKEN_EXPIRED_401);
        }
        return validateClientScope(authResult, grantType);
    }

    private ValidationResult validateClientScope(AuthResult authResult, String grantType) {
        ClientModel client = authResult.getClient();
        Map<String, ClientScopeModel> clientScopes = client.getClientScopes(true);
        if (clientScopes == null || !clientScopes.containsKey(CLIENT_SCOPE_APIKEYS)) {
            LOG.error("Client ID " + client.getClientId() + " is missing scope- apikeys");
            return new ValidationResult(Status.FORBIDDEN, ErrorMessage.SCOPE_MISSING_403);
        }
        return validateTokenGrant(authResult, grantType);
    }

    private static ValidationResult validateTokenGrant(AuthResult authResult, String grantType) {
        if (!isValidGrantType(authResult, grantType)) {
            return new ValidationResult(Status.FORBIDDEN, ErrorMessage.USER_MISSING_403);
        }
        ValidationResult result = new ValidationResult(Status.OK, null);
        result.setUser(authResult.getUser());
        return result;
    }

    /**
     * We get the client_id on token object  in case the token was issued with the grant_type 'client_credentials'
     * this is used to verify against the requested grant_type of the controller method.
     * e.g. for disabling the apikeys, we allow access with tokens who have grant_type 'password'
     * If not specific gran_type check is requested then method return true.
     *
     * @param authResult result
     * @param grantType  client_credentials or password
     * @return boolean
     */
    private static boolean isValidGrantType(AuthResult authResult, String grantType) {
        if (GRANT_TYPE_PASSWORD.equals(grantType)) {
            return authResult.getToken().getOtherClaims().get("client_id") == null;
        }
        if (GRANT_TYPE_CLIENT_CRED.equals(grantType)) {
            return authResult.getToken().getOtherClaims().get("client_id") != null;
        }
        return true;
    }

    /**
     * Legacy method to validate apikey
     *
     * @param apikey string
     * @return boolean true if valid
     */
    public boolean validateApikeyLegacy(String apikey) {
        //validate if key exists . The clientID we receive in request parameter is actually the apikey.
        ClientProvider clientProvider = session.clients();
        ClientModel client = clientProvider.getClientByClientId(realm, apikey);
        if (client == null) {
            LOG.error(String.format(APIKEY_NOT_REGISTERED, apikey));
            return false;
        }
        //check if key not deprecated and currently active
        if (!client.isEnabled()) {
            LOG.error(String.format(APIKEY_NOT_ACTIVE, client.getClientId()));
            return false;
        }
        return true;
    }

    /**
     * Validates the input kyecloak client (i.e. apikey)
     *
     * @param client  ClientModel representing the apikey
     * @param keyType indicating type of apikey e.g. 'PersonalKey','ProjectKey' or empty
     * @return validation result object
     */
    public ValidationResult validateClient(ClientModel client, String keyType) {
        //check if key is either personal or project key
        if (client == null || StringUtils.isEmpty(keyType)) {
            return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.KEY_INVALID_401);
        }
        //check if key not deprecated and currently active
        if (!client.isEnabled()) {
            return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.KEY_DISABLED_401);
        }
        return new ValidationResult(Status.OK, null);
    }

    /**
     * Performs the rate limit check for the input client.
     *
     *
     * @param clientId  Keycloak client Id
     * @param keyType   indicating type of apikey e.g. 'PersonalKey','ProjectKey' or empty
     * @param rateLimitPolicy indicates max rateLimitPolicy quota for the key.
     * @return validation result object
     */
    public ValidationResult performRateLimitCheck(String clientId, String keyType, RateLimitPolicy rateLimitPolicy) {

        InfinispanConnectionProvider provider = session.getProvider(
                InfinispanConnectionProvider.class);
        Cache<String, SessionTracker> sessionTrackerCache = provider.getCache(SESSION_TRACKER_CACHE);
        if (sessionTrackerCache == null) {
            LOG.error("Infinispan cache " + SESSION_TRACKER_CACHE
                    + " not found. Cannot perform rate limit check");
            return new ValidationResult(Status.OK, null);
        }

        //Update the sessionTracking cache
        LocalDateTime now = LocalDateTime.now();
        String currentDateTimeString = FORMATTER.format(now);
        SessionTrackerUpdater updater = new SessionTrackerUpdater(currentDateTimeString, rateLimitPolicy);
        SessionTracker updatedTracker = sessionTrackerCache.compute(clientId, updater);

        //Generate remaining rate limit details for the requested apikey
        RateLimit rateLimit = new RateLimit(rateLimitPolicy.getVendorIdentifier(),
            updatedTracker.getSessionCount(),
            getRemainingTimeUtilReset(now));

        // If the rateLimit is zero by default or is already exhausted , send 429
        if (ifDefaultRateLimitIsZero(rateLimitPolicy) ||
            isRateLimitExhaustedBeforeCurrentRequest(currentDateTimeString,
                updatedTracker.getLastRateLimitReachingTime(),
                updatedTracker.getSessionCount())) {
            //create result with error message for 429 status
            return new ValidationResult(Status.TOO_MANY_REQUESTS,
                generateMessageForQuotaExhaustion(keyType, rateLimitPolicy.getQ()),
                rateLimit);
        }

        return new ValidationResult(Status.OK, null ,rateLimit);
    }

    private boolean ifDefaultRateLimitIsZero(RateLimitPolicy rateLimitPolicy) {
        return (rateLimitPolicy!=null && rateLimitPolicy.getQ() == 0 );
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

    private ErrorMessage generateMessageForQuotaExhaustion(String keyType, int limit) {
        return (PROJECT.equals(keyType) ?
              projectKeyLimitReachedMessage(limit)
            : personalKeyLimitReachedMessage(limit)
        );
    }

    private static ErrorMessage personalKeyLimitReachedMessage(int limit) {
        return ErrorMessage.LIMIT_PERSONAL_KEYS_429.formatError(
                String.valueOf(limit),
                String.valueOf(RATE_LIMIT_DURATION))
            .formatErrorMessage(String.valueOf(limit),
                String.valueOf(RATE_LIMIT_DURATION));
    }

    private static ErrorMessage projectKeyLimitReachedMessage(int limit) {
        return ErrorMessage.LIMIT_PROJECT_KEYS_429.formatError(String.valueOf(limit),
            String.valueOf(RATE_LIMIT_DURATION));
    }

    /**
     * Determines if rate Limit was already exhausted before current request. <br>
     * Logic:<br>
     *  1. Verifies if session count is 0 and a "last limit reached time" is recorded.<br>
     *  2. Compares the two timestamps - "limit reached time" and "current request time".<br>
     *  3. Returns true only if the limit was reached at a time strictly before the current request.<br>
     *
     * @param currentRequestTimeStr - Current request time
     * @param quotaDepletedTimeStr - String indicating last time rate limit was reached/exhausted
     * @param availableSessionCount updated session count from cache
     * @return boolean true if the 'quotaDepletedTime' isBefore 'currentRequestTime'.
     */
    public boolean isRateLimitExhaustedBeforeCurrentRequest(String currentRequestTimeStr,
        String quotaDepletedTimeStr, int availableSessionCount) {

        if (availableSessionCount == 0 && !StringUtils.isBlank(quotaDepletedTimeStr)) {

            LocalDateTime currentRequestTime =LocalDateTime.parse(
                currentRequestTimeStr, FORMATTER);
            LocalDateTime quotaDepletedTime = LocalDateTime.parse(
                quotaDepletedTimeStr, FORMATTER);

            return quotaDepletedTime.isBefore(currentRequestTime);
        }
        return false;
    } 
 
    /**
     * Calculate the type of apikey based on specific roles associated to the client.
     *
     * @param role representing an apikey
     * @return key Type
     */
    public String getKeyType(RoleModel role) {
        if (role != null) {
            if (SHARED_OWNER.equals(role.getName())) {
                return PROJECT;
            }
            if (CLIENT_OWNER.equals(role.getName())) {
                return PERSONAL;
            }
        }
        return "";
    }


    /**
     * Fetch the apikey from request header
     *
     * @param httpRequest request
     * @return apikey string
     */
    public String extractApikeyFromAuthorizationHeader(HttpRequest httpRequest) {
        HttpHeaders httpHeaders = httpRequest.getHttpHeaders();
        String authorization = httpHeaders != null ? httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION) : null;
        if (authorization != null) {
            try {
                Pattern pattern = Pattern.compile(APIKEY_PATTERN);
                Matcher matcher = pattern.matcher(authorization);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            } catch (RuntimeException e) {
                LOG.error("Regex problem while parsing authorization header", e);
            }
        }
        LOG.error("No Apikey found in request header!");
        return null;
    }


    /**
     * Check if the HttpRequest has the Authorization header and validate its value.
     *
     * @param grantType type of grant used for issuing token. can be password or client_credentials
     * @return ValidationResult
     */
    public ValidationResult validateAuthToken(String grantType) {
        HttpHeaders headers = session.getContext().getHttpRequest().getHttpHeaders();
        String authHeader = AppAuthManager.extractAuthorizationHeaderToken(headers);
        if (StringUtils.isEmpty(authHeader)) {
            return new ValidationResult(Status.UNAUTHORIZED, ErrorMessage.TOKEN_MISSING_401);
        }
        return authorizeToken(authHeader, grantType);
    }

    /**
     * Will fetch the Rate Limit policy for the given key
     *
     * @return rate limit policy
     */
    public RateLimitPolicy getRateLimitPolicy(String keyType, RoleModel clientRole) {
        //Check and  get if any specific rate limit configured for that key.
        Integer clientSpecificRateLimit = getRateLimitSpecificToKey(clientRole);
        boolean isSpecificLimit = clientSpecificRateLimit != null;

        String rateLimitQuotaType = isSpecificLimit ? CUSTOM : keyType;
        int rateLimitQuota = isSpecificLimit ? clientSpecificRateLimit : getSessionKeyLimit(keyType);

        return new RateLimitPolicy(rateLimitQuotaType, rateLimitQuota, RATE_LIMIT_DURATION * 60L);
    }

    private int getSessionKeyLimit(String keyType) {
        return switch (keyType) {
            case PERSONAL -> PERSONAL_KEY_LIMIT;
            case PROJECT -> PROJECT_KEY_LIMIT;
            default -> 0;
        };
    }

    /**
     * Retrieves the specific rate limit value configured for the current key.
     * This value is sourced from the 'rateLimit' attribute defined on a designated client role
     * (e.g., 'client_owner' or 'shared_owner') associated with the client.If value not set method returns null,
     * indicating that system-wide default limits to be calculated and applied.
     *
     * @param role Keycloak client role
     * @return rate Limit from client role
     */
    private Integer getRateLimitSpecificToKey(RoleModel role) {
        if (role == null) {
            return null;
        }
        try {
            String rateLimitFromAttribute = role.getFirstAttribute(ROLE_ATTRIBUTE_RATE_LIMIT);
            return StringUtils.isNotEmpty(rateLimitFromAttribute) ? Integer.valueOf(rateLimitFromAttribute) : null;
        } catch (NumberFormatException e) {
            LOG.error("Incorrect rate limit specified in role attribute " + ROLE_ATTRIBUTE_RATE_LIMIT);
            return null;
        }
    }

    /**
     * Retrieves the designated roles for key ownership from the given client.
     * Valid roles are 'shared_owner' and 'client_owner'
     *
     * @param client keycloak client
     * @return roleModel object designated for representing apikey related roles else null
     */
    public RoleModel getClientRoleForKeyOwnership(ClientModel client) {
        if (client != null) {
            if (client.getRole(SHARED_OWNER) != null) {
                return client.getRole(SHARED_OWNER);
            }
            if (client.getRole(CLIENT_OWNER) != null) {
                return client.getRole(CLIENT_OWNER);
            }
        }
        return null;
    }

}
