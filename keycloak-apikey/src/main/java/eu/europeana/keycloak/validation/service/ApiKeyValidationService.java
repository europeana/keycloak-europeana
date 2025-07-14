package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.SessionTracker;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import eu.europeana.keycloak.validation.util.Constants;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;
import org.infinispan.Cache;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

/**
 * Provides operations for apikey validation.
 */
public class ApiKeyValidationService {

  private static final Logger LOG  = Logger.getLogger(ApiKeyValidationService.class);
  private final KeycloakSession session;
  private final RealmModel realm;

  /**
   * Initialize the ApiKeyValidationService using keycloak session
   * @param session Keycloak session
   */
  public ApiKeyValidationService(KeycloakSession session) {
    this.session = session;
    this.realm =  session.getContext().getRealm();
  }

  /** Method authenticates the bearer token input using the keycloack auth manager
   * @param tokenString  in form 'Bearer <TOKEN_VALUE>'
   * @param grantType type of the token,it can be for client_credentials or password based
   * @return ValidationResult
   */
  public ValidationResult authorizeToken(String tokenString,String grantType) {
      //As we want to send different message when the token is inactive , first get the token verified without the ifActive check.
      AuthResult authResult=  AuthenticationManager.verifyIdentityToken(
          this.session, this.realm, session.getContext().getUri(),
          session.getContext().getConnection(), false, true,null, false,
          tokenString, session.getContext().getRequestHeaders());

      if (authResult == null || authResult.getClient() == null) {
        return new ValidationResult( Status.UNAUTHORIZED,ErrorMessage.TOKEN_INVALID_401);
      }
      if(!authResult.getToken().isActive()) {
        return new ValidationResult(Status.UNAUTHORIZED,ErrorMessage.TOKEN_EXPIRED_401);
      }
      return validateClientScope(authResult,grantType);
  }

  private ValidationResult validateClientScope(AuthResult authResult,String grantType) {
    ClientModel client =authResult.getClient();
    Map<String, ClientScopeModel> clientScopes = client.getClientScopes(true);
    if (clientScopes == null || !clientScopes.containsKey(Constants.CLIENT_SCOPE_APIKEYS)) {
      LOG.error("Client ID " + client.getClientId() + " is missing scope- apikeys");
      return new ValidationResult( Status.FORBIDDEN, ErrorMessage.SCOPE_MISSING_403);
    }
    return validateTokenGrant(authResult, grantType);
  }

  private static ValidationResult validateTokenGrant(AuthResult authResult, String grantType) {
    if(!isValidGrantType(authResult, grantType)){
      return new ValidationResult(Status.FORBIDDEN, ErrorMessage.USER_MISSING_403);
    }
    ValidationResult result = new ValidationResult(Status.OK,null);
    result.setUser(authResult.getUser());
    return result;
  }

  /** We get the client_id on token object  in case the token was issued with the grant_type 'client_credentials'
   * this is used to verify against the requested grant_type of the controller method.
   * e.g. for disabling the apikeys, we allow access with tokens who have grant_type 'password'
   * If not specific gran_type check is requested then method return true.
   * @param authResult result
   * @param grantType client_credentials or password
   * @return boolean
   */
  private static boolean isValidGrantType(AuthResult authResult, String grantType) {
   if (Constants.GRANT_TYPE_PASSWORD.equals(grantType) ){
    return authResult.getToken().getOtherClaims().get("client_id") == null;
   }
   if (Constants.GRANT_TYPE_CLIENT_CRED.equals(grantType) ){
     return authResult.getToken().getOtherClaims().get("client_id") != null;
   }
   return true;
  }

  /**
   * Legacy method to validate apikey
   * @param apikey string
   * @return boolean true if valid
   */
  public boolean validateApikeyLegacy(String apikey){
     //validate if key exists . The clientID we receive in request parameter is actually the apikey.
    ClientProvider clientProvider = session.clients();
    ClientModel client = clientProvider.getClientByClientId(realm, apikey);
    if(client ==null ){
      LOG.error(String.format(Constants.APIKEY_NOT_REGISTERED, apikey));
      return false;
    }
    //check if key not deprecated and currently active
    if(!client.isEnabled()){
      LOG.error(String.format(Constants.APIKEY_NOT_ACTIVE, client.getClientId()));
      return false;
     }
    return true;
  }

  /** Validates the input apikey
   * @param apikey string value
   * @return validation result object
   */
  public ValidationResult validateApikey(String apikey){
    //validate if key exists . The clientID we receive in request parameter is actually the apikey.
    ClientModel client = session.clients().getClientByClientId(realm, apikey);
    return validateClient(client);
  }

  private ValidationResult validateClient(ClientModel client) {
    if(client ==null){
      return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.KEY_INVALID_401);
    }
    //check if key not deprecated and currently active
    if(!client.isEnabled()){
      return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.KEY_DISABLED_401);
    }
    // check the rate limit
    InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
    Cache<String, SessionTracker> sessionTrackerCache = provider.getCache("sessionTrackerCache");
    SessionTracker sessionTracker =  sessionTrackerCache.get(client.getClientId());
    String rateLimitDuration =System.getenv("SESSION_DURATION_FOR_RATE_LIMITING");
    //If the client id reflects a personal key check against the personal key limit and respond with a HTTP 429 error code “429_limit_personal”
    if(sessionTracker!=null && client.getRole(Constants.CLIENT_OWNER)!=null){
     String personalKeyLimit =  System.getenv("PERSONAL_KEY_RATE_LIMIT");
     if(StringUtils.isNotBlank(personalKeyLimit) ) {
       int sessionCount = Integer.parseInt(sessionTracker.getSessionCount());
       if(sessionCount >Integer.parseInt(personalKeyLimit)) {
         return new ValidationResult(Status.TOO_MANY_REQUESTS,ErrorMessage.LIMIT_PERSONAL_KEYS_429.formatError(personalKeyLimit,rateLimitDuration));
       }
     }
    }

    if(sessionTracker!=null && client.getRole(Constants.SHARED_OWNER)!=null){
      String projectKeyLimit =  System.getenv("PROJECT_KEY_RATE_LIMIT");
      if(StringUtils.isNotBlank(projectKeyLimit) ) {
        int sessionCount = Integer.parseInt(sessionTracker.getSessionCount());
        if(sessionCount >Integer.parseInt(projectKeyLimit)) {
          return new ValidationResult(Status.TOO_MANY_REQUESTS,ErrorMessage.LIMIT_PROJECT_KEYS_429.formatError(projectKeyLimit,rateLimitDuration));
        }
      }
    }

    return new ValidationResult(Status.OK,null);
  }


  /**
   * Fetch the apikey from request header
   * @param httpRequest request
   * @return apikey string
   */
  public String extractApikeyFromAuthorizationHeader(HttpRequest httpRequest){
    HttpHeaders httpHeaders = httpRequest.getHttpHeaders();
    String authorization = httpHeaders!=null?httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION):null;
    if (authorization != null) {
      try {
        Pattern pattern = Pattern.compile(Constants.APIKEY_PATTERN);
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




  /**Check if the HttpRequest has the Authorization header and validate its value.
   * @param grantType type of grant used for issuing token. can be password or client_credentials
   * @return ValidationResult
   */
  public ValidationResult validateAuthToken(String grantType) {
    HttpHeaders headers = session.getContext().getHttpRequest().getHttpHeaders();
    String authHeader = AppAuthManager.extractAuthorizationHeaderToken(headers);
    if(StringUtils.isEmpty(authHeader)) {
     return new ValidationResult( Status.UNAUTHORIZED,ErrorMessage.TOKEN_MISSING_401);
    }
    return authorizeToken(authHeader,grantType);
  }

  /**
   * Validates the input IP address string
   * @param ip IPv4 address string
   * @return Validation result
   */
  public ValidationResult validateIp(String ip) {
    if(StringUtils.isBlank(ip)) {
      return new ValidationResult(Status.BAD_REQUEST,ErrorMessage.IP_MISSING_400);
    }
    if(!isValidIpAddress(ip)){
      return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.IP_INVALID_400);
    }
    return null;
  }

  /** Method checks IPV4 address input and checks if it is matching the xxx.xxx.xxx.xxx
   * where xxx  ranges from 0-255 and validated using regex.
   * Regex explanation:
   * (\d{1,2}  -> one or two-digit number
   * |(0|1)\d{2}-> or first number can be 0 or 1 followed by any 2-digit number
   * |2[0-4]\d -> or  first number is 2 middle number can be between 0 and 4 and last number can be any single digit
   * |25[0-5]) -> or  first number is 2 middle number is 5 and last number can be between 0 and 5
   * @param ip value as string
   * @return boolean indication if ip format is valid or not
   */
  private boolean isValidIpAddress(String ip) {
    String regexFrag = "(\\d{1,2}|([0-1])\\d{2}|2[0-4]\\d|25[0-5])";
    String regex =regexFrag+"\\."+regexFrag+"\\."+regexFrag+"\\."+regexFrag;
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(ip);
    return  matcher.matches();
  }



}