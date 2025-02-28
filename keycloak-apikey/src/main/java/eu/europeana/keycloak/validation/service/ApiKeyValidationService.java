package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.exception.ErrorResponse;
import jakarta.ws.rs.NotAuthorizedException;
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
import org.keycloak.services.managers.AppAuthManager.BearerTokenAuthenticator;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

public class ApiKeyValidationService {

  private static final Logger LOG  = Logger.getLogger(ApiKeyValidationService.class);
  public static final String CLIENT_SCOPE_APIKEYS = "apikeys";

  private final KeycloakSession session;

  private final RealmModel realm;

  public static final String APIKEY_NOT_REGISTERED                 = "API key %s is not registered";
  public static final String APIKEY_NOT_ACTIVE                 = "API key %s is not active";

  public static final String APIKEY_PATTERN                        = "APIKEY\\s+([^\\s]+)";

  public ApiKeyValidationService(KeycloakSession session) {
    this.session = session;
    this.realm =  session.getContext().getRealm();
  }
  public boolean isAuthorized() {
    try {
      BearerTokenAuthenticator authenticator = new BearerTokenAuthenticator(session);
      AuthResult authResult = authenticator.authenticate();
      if (authResult == null ) {
        LOG.error("AuthResult is null Token expired ");
        return false;
      }
      if(authResult.getToken()==null){ LOG.error("Token is null ");  return false;}

      if(authResult.getToken().isExpired()){LOG.error("Token is expired ");  return false;}

      if(authResult.getClient()==null){LOG.error("Token not associated to any client ");  return false;}

      return validateClientScope(authResult);
    }
    catch (NotAuthorizedException e){
      LOG.error("Exception during token authentication : "+ e.getMessage());
      return  false;
    }
  }

  private static boolean validateClientScope(AuthResult authResult) {
    ClientModel client = authResult.getClient();
    Map<String, ClientScopeModel> clientScopes = client.getClientScopes(true);
    if(clientScopes != null && !clientScopes.containsKey(CLIENT_SCOPE_APIKEYS)){
      LOG.error("Client with ID "+client.getClientId() + " does not have required scope - apikeys ");
      return false;
    }
    return true;
  }


  public boolean validateApikey(String apikey)
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

  public ErrorResponse validateAuthToken() {
    HttpHeaders headers = session.getContext().getHttpRequest().getHttpHeaders();
    String authHeader = AppAuthManager.extractAuthorizationHeaderToken(headers);
    if(StringUtils.isEmpty(authHeader)) {
      return new ErrorResponse(Status.UNAUTHORIZED.getStatusCode(),"Token is missing","Please issue a token and supply it within the Authorization header.",null);
    }




    return null;
  }
}
