package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.datamodel.ErrorMessage;
import eu.europeana.keycloak.validation.datamodel.ValidationResult;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationManager.AuthResult;

public class ApiKeyValidationService {

  private static final Logger LOG  = Logger.getLogger(ApiKeyValidationService.class);
  public static final String CLIENT_SCOPE_APIKEYS = "apikeys";
  public static final String ROLE_ATTRIBUTE_CREATION_DATE = "creationDate";
  public static final String CLIENT_STATE_DISABLED = "Disabled";
  public static final String PERSONAL_KEY = "PersonalKey";
  public static final String PROJECT_KEY = "ProjectKey";

  private final KeycloakSession session;

  private final RealmModel realm;

  public static final String APIKEY_NOT_REGISTERED = "API key %s is not registered";
  public static final String APIKEY_NOT_ACTIVE = "API key %s is not active";
  public static final String APIKEY_PATTERN = "APIKEY\\s+([^\\s]+)";


  public static final String CLIENT_OWNER = "client_owner";
  public static final String SHARED_OWNER ="shared_owner";

  public ApiKeyValidationService(KeycloakSession session) {
    this.session = session;
    this.realm =  session.getContext().getRealm();
  }


  /** Method authenticates the bearer token input using the keycloack auth manager
   * @param tokenString  in form 'Bearer <TOKEN_VALUE>'
   * @return ValidationResult
   */
  public ValidationResult isAuthorized(String tokenString) {
      //As we want to send different message when the token is inactive , first get the token verified without the ifActive check.
      AuthResult authResult=  AuthenticationManager.verifyIdentityToken(
          this.session, this.realm, session.getContext().getUri(),
          session.getContext().getConnection(), false, true,null, false,
          tokenString, session.getContext().getRequestHeaders(), new TokenVerifier.Predicate[0]);
      if (authResult == null || authResult.getClient() == null) {
        return new ValidationResult( Status.UNAUTHORIZED,ErrorMessage.TOKEN_INVALID_401);
      }
      if(!authResult.getToken().isActive()) {
        return new ValidationResult(Status.UNAUTHORIZED,ErrorMessage.TOKEN_EXPIRED_401);
      }
      LOG.info("Token sub : "+authResult.getToken().getSubject() + "  email: "+authResult.getToken().getEmail());
      return validateClientScope(authResult);
  }

  private ValidationResult validateClientScope(AuthResult authResult) {
    ClientModel client =authResult.getClient();
    Map<String, ClientScopeModel> clientScopes = client.getClientScopes(true);
    if (clientScopes == null || !clientScopes.containsKey(CLIENT_SCOPE_APIKEYS)) {
      LOG.error("Client ID " + client.getClientId() + " is missing scope- apikeys");
      return new ValidationResult( Status.FORBIDDEN, ErrorMessage.SCOPE_MISSING_403);
    }
    ValidationResult result = new ValidationResult(Status.OK,null);
    result.setUser(authResult.getUser());
    return result;
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

  public ValidationResult validateApikey(String apikey){
    //validate if key exists . The clientID we receive in request parameter is actually the apikey.
    ClientModel client = session.clients().getClientByClientId(realm, apikey);
    return validateClient(client);
  }

  public ValidationResult validateClientById(String clientPublicId) {
    ClientModel client =session.clients().getClientById(realm,clientPublicId);
    return validateClient(client);
  }

  private static ValidationResult validateClient(ClientModel client) {
    if(client ==null){
      return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.KEY_INVALID_401);
    }
    //check if key not deprecated and currently active
    if(!client.isEnabled()){
      return new ValidationResult(Status.BAD_REQUEST, ErrorMessage.KEY_DISABLED_401);
    }
    return new ValidationResult(Status.OK,null);
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
     return new ValidationResult( Status.UNAUTHORIZED,ErrorMessage.TOKEN_MISSING_401);
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
    String regexFrag = "(\\d{1,2}|(0|1)\\d{2}|2[0-4]\\d|25[0-5])";
    String regex =regexFrag+"\\."+regexFrag+"\\."+regexFrag+"\\."+regexFrag;
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(ip);
    return  matcher.matches();
  }


  public List<Apikey> getPrivateAndProjectkeys(UserModel userModel) {
    List<RoleModel> rolesAssociatedToUser = userModel.getRoleMappingsStream().filter(
        roleModel -> (CLIENT_OWNER.equals(roleModel.getName()) || SHARED_OWNER.equals(
            roleModel.getName()))).toList();
    return gatherSortedApiKeyInfo(rolesAssociatedToUser);
  }

  /** Method fetches personal and project keys and then sorts them based on creation date.   *
   * The resultant list has 2 parts-> first is sorted personal keys and then sorted project keys.
   * If the creation date is not provided the such keys are appended at the end of respective list.
   * @param roleModelList roles Associated To User
   * @return Api key List
   */
  private static List<Apikey> gatherSortedApiKeyInfo(List<RoleModel> roleModelList) {
    List<Apikey> personalKeys = new ArrayList<>();
    List<Apikey> projectKeys = new ArrayList<>();

    gatherPersonalAndProjectKeys(roleModelList, personalKeys, projectKeys);

    personalKeys.sort(Comparator.comparing(Apikey::getCreationDate,Comparator.nullsLast(Comparator.naturalOrder())));
    projectKeys.sort(Comparator.comparing(Apikey::getCreationDate,Comparator.nullsLast(Comparator.naturalOrder())));

    personalKeys.addAll(projectKeys);

    return personalKeys;
  }

  private static void gatherPersonalAndProjectKeys(List<RoleModel> rolesAssociatedToUser, List<Apikey> personalKeys,
      List<Apikey> projectKeys) {
    for (RoleModel rolemodel : rolesAssociatedToUser) {
      if (rolemodel.isClientRole()) {
        Apikey apikey = getApikey(rolemodel);
        if(apikey !=null) {
          if(PERSONAL_KEY.equals(apikey.getType())){
            personalKeys.add(apikey);
          }
          if(PROJECT_KEY.equals(apikey.getType())){
            projectKeys.add(apikey);
          }
        }
      }
    }
  }

  /**Based on the role associated to use , get the client(i.e. apikey) and
   * generate the Apikey object for only personal & project keys associated to user.
   * The creation date of key is fetched based on attribute associated to user
   * @param rolemodel role associated to user
   * @return Apikey objec or else null
   */
  private static Apikey getApikey(RoleModel rolemodel) {
    ClientModel client = (ClientModel) rolemodel.getContainer();
    Date creationDate = getRoleCreationDate(rolemodel.getFirstAttribute(ROLE_ATTRIBUTE_CREATION_DATE));
    String clientState = client.isEnabled()?null: CLIENT_STATE_DISABLED;
    if (CLIENT_OWNER.equals(rolemodel.getName())) {
      return new Apikey(client.getId(), client.getClientId(), PERSONAL_KEY, creationDate, null,
          null, clientState);
    }
    if (SHARED_OWNER.equals(rolemodel.getName())) {
      return new Apikey(client.getId(), client.getClientId(), PROJECT_KEY, creationDate,
          client.getName(),
          client.getDescription(), clientState);
    }
    return null;
  }

  private static Date getRoleCreationDate(String creationDate) {
    if(StringUtils.isEmpty(creationDate)) return null;
    try {
      ZonedDateTime zonedDateTime = ZonedDateTime.parse(creationDate,DateTimeFormatter.ISO_INSTANT.withZone(
          ZoneOffset.UTC));
      return Date.from(zonedDateTime.toInstant());
    }
    catch (DateTimeParseException ex) {
      LOG.error("Exception occurred while parsing date : "+creationDate+" returning null date");
      return null;
    }
  }

}
