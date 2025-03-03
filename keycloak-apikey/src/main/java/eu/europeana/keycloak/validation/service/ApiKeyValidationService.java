package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

public class ApiKeyValidationService {

  private static final Logger LOG  = Logger.getLogger(ApiKeyValidationService.class);
  public static final String CLIENT_SCOPE_APIKEYS = "apikeys";

  private final KeycloakSession session;

  private final RealmModel realm;

  public static final String APIKEY_NOT_REGISTERED = "API key %s is not registered";
  public static final String APIKEY_NOT_ACTIVE = "API key %s is not active";
  public static final String APIKEY_PATTERN = "APIKEY\\s+([^\\s]+)";

  public ApiKeyValidationService(KeycloakSession session) {
    this.session = session;
    this.realm =  session.getContext().getRealm();
  }


  /** Method authenticates the bearer token input using the keycloack auth manager
   * @param tokenString  in form 'Bearer <TOKEN_VALUE>'
   * @return ValidationResult  or null
   */
  public ValidationResult isAuthorized(String tokenString) {

      //As we want to send different message when the token is inactive , first get the token verified without the ifActive check.
      AuthResult authResult=  AuthenticationManager.verifyIdentityToken(
          this.session, this.realm, session.getContext().getUri(),
          session.getContext().getConnection(), false, true,null, false,
          tokenString, session.getContext().getRequestHeaders(), new TokenVerifier.Predicate[0]);
      if (authResult == null || authResult.getClient() == null) {
        return new ValidationResult(Status.UNAUTHORIZED,ErrorMessage.TOKEN_INVALID_401);
      }
      if(!authResult.getToken().isActive()) {
        return new ValidationResult(Status.UNAUTHORIZED,ErrorMessage.TOKEN_EXPIRED_401);
      }
      return validateClientScope(authResult.getClient());
  }

  private ValidationResult validateClientScope(ClientModel client) {
    Map<String, ClientScopeModel> clientScopes = client.getClientScopes(true);
    if (clientScopes == null || !clientScopes.containsKey(CLIENT_SCOPE_APIKEYS)) {
      LOG.error("Client ID " + client.getClientId() + " is missing scope- apikeys");
      return new ValidationResult(Status.FORBIDDEN, ErrorMessage.SCOPE_MISSING_403);
    }
    return null;
  }

   public boolean validateApikeyLegacy(String apikey)
   {
    //validate if key exists . The clientID we receive in request parameter is actually the apikey.
    ClientProvider clientProvider = session.clients();
    ClientModel client = clientProvider.getClientByClientId(realm, apikey);
    if(client ==null ){
      LOG.error(String.format(APIKEY_NOT_REGISTERED, apikey));
      return false;
    }
    //check if key not deprecated and currently active
    if(!client.isEnabled()){
      LOG.error(String.format(APIKEY_NOT_ACTIVE, client.getClientId()));
      return false;
     }
    return true;
  }

  public ValidationResult validateApikey(String apikey)
  {
    //validate if key exists . The clientID we receive in request parameter is actually the apikey.
    ClientProvider clientProvider = session.clients();
    ClientModel client = clientProvider.getClientByClientId(realm, apikey);
    if(client ==null ){
      return new ValidationResult(Status.BAD_REQUEST,ErrorMessage.KEY_INVALID_401);
    }
    //check if key not deprecated and currently active
    if(!client.isEnabled()){
      return new ValidationResult(Status.BAD_REQUEST,ErrorMessage.KEY_DISABLED_401);
    }
    return null;
  }

  public String extractApikeyFromAuthorizationHeader(HttpRequest httpRequest){
    HttpHeaders httpHeaders = httpRequest.getHttpHeaders();
    String authorization = httpHeaders!=null?httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION):null;
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

  /**Check if the HttpRequest has the Authorization header and validate its value   *
   * @return ValidationResult or null
   */
  public ValidationResult validateAuthToken() {
    HttpHeaders headers = session.getContext().getHttpRequest().getHttpHeaders();
    String authHeader = AppAuthManager.extractAuthorizationHeaderToken(headers);
    if(StringUtils.isEmpty(authHeader)) {
     return new ValidationResult(Status.UNAUTHORIZED,ErrorMessage.TOKEN_MISSING_401);
    }
    return isAuthorized(authHeader);
  }

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
   * where xxx  ranges from 0-255
   * @param ip value as string
   * @return boolean indication if ip format is valid or not
   */
  private boolean isValidIpAddress(String ip) {
    String regexFrag = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
    String regex =regexFrag+"\\."+regexFrag+"\\."+regexFrag+"\\."+regexFrag;
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(ip);
    return  matcher.matches();
  }
}
