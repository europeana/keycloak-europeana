package eu.europeana.keycloak.registration.service;

import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.CAPTCHA_MISSING;
import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.CAPTCHA_PATTERN;
import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.CAPTCHA_VERIFICATION_FAILED;

import eu.europeana.keycloak.registration.datamodel.RegistrationInput;
import eu.europeana.keycloak.registration.exception.CaptchaException;
import eu.europeana.keycloak.registration.util.PassGenerator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailException;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

public class RegistrationService {

  public static final String CLIENT_OWNER = "client_owner";
  private static final Logger LOG = Logger.getLogger(RegistrationService.class);
  public static final String ACCOUNT_NOT_FOUND_FOR_EMAIL = "An Europeana account was not found associated to the email address %s";

  private final KeycloakSession session;
  private final RealmModel realm;
  private final UserProvider userProvider;

  public RegistrationService(KeycloakSession session) {
    this.session =session;
    this.realm = session.getContext().getRealm();
    this.userProvider = session.users();
  }

  @Path("")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public Response registerWithCaptcha(RegistrationInput input){
    try {
      verifyCaptcha();
      if (input == null && StringUtils.isEmpty(input.getEmail())) {
        return Response.status(Status.BAD_REQUEST).entity("The email field is missing").build();
      }
      UserModel user = getUserBasedOnEmail(input.getEmail());
      if (user == null) {
        return Response.status(Status.BAD_REQUEST).entity(
            String.format(ACCOUNT_NOT_FOUND_FOR_EMAIL, input.getEmail())).build();
      }
      updateKeyAndNotifyUser(user);
      return Response.ok().build();
    }
    catch (CaptchaException ex){
      return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
    }
    catch (EmailException ex){
      return Response.status(Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).build();
    }
  }

  private void updateKeyAndNotifyUser(UserModel user) throws EmailException {
    String apikey = getOrCreateApikeyForUser(user);
    //send mail to accepted email id i.e. email of user
    MailService mailService = new MailService(session, user);
    mailService.sendEmailToUserWithApikey(apikey);
  }

  private String getOrCreateApikeyForUser(UserModel user) {
    var client = getOrCreateClientForUser(user);
    if(client !=null){
      RoleModel clientRole = getOrCeateNewRoleForClient(client);
      if(clientRole != null && !user.hasRole(clientRole)){
          user.grantRole(clientRole);
          LOG.info("Assigning new role : " + clientRole.getName() + " to User : " + user.getUsername());
        }
      }
    return client.getClientId();
  }

  private ClientModel getOrCreateClientForUser(UserModel user) {
    var client = Optional.ofNullable(getClientAssociatedToUser(user))
        .orElseGet(() -> {
          var newClient = createClientWithNewKey(user);
          LOG.info("New client : " + newClient.getClientId() + " is created for user : "+ user.getUsername());
          return newClient;
        });
    return client;
  }

  private void verifyCaptcha() {
    String captchaToken = getAuthorizationHeader(session.getContext().getHttpRequest(), CAPTCHA_PATTERN);
    if (captchaToken == null) {
      LOG.info(CAPTCHA_MISSING);
      throw new CaptchaException(CAPTCHA_MISSING);
    }
    CaptchaManager captchaManager = new CaptchaManager();
    if (!captchaManager.verifyCaptchaToken(captchaToken)) {
      LOG.info(CAPTCHA_VERIFICATION_FAILED);
      throw new CaptchaException(CAPTCHA_VERIFICATION_FAILED);
    }
  }

  private String getAuthorizationHeader(HttpRequest httpRequest, String captchaPattern) {
      String authorization = httpRequest.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
      if (authorization != null) {
        try {
          Pattern pattern = Pattern.compile(captchaPattern);
          Matcher matcher = pattern.matcher(authorization);
          if (matcher.find()) {
            return matcher.group(1);
          }
        } catch (RuntimeException e) {
          LOG.error("Regex problem while parsing authorization header", e);
        }
      }
      return null;
    }

  private RoleModel getOrCeateNewRoleForClient(ClientModel client) {
      RoleProvider roles = session.roles();
      if (client.getRole(CLIENT_OWNER) != null) {
       return client.getRole(CLIENT_OWNER);
      }
      LOG.info("New owner role will be added for client : " + client.getId());
      return roles.addClientRole(client, CLIENT_OWNER);
  }

  private ClientModel createClientWithNewKey(UserModel user) {
    String apiKey = createApiKey();
     //Create a new client with the API key as ID.
      KeyCloakClientCreationService service = new KeyCloakClientCreationService(realm, session,
          apiKey, user.getUsername());
      return service.registerClient();
  }

  private String createApiKey() {
      LOG.info("Generating new API key !!");
      String id ;
      ClientProvider clientProvider = session.clients();
      PassGenerator pg = new PassGenerator();
      do {
        id = pg.generate(RandomUtils.nextInt(8, 13));       
      } while (clientProvider.getClientById(realm,id) != null);
      LOG.info("Created new API key : "+ id );
    return id;

  }

  private ClientModel getClientAssociatedToUser(UserModel user) {
    Optional<RoleModel> rModel = user.getRoleMappingsStream().filter(
        roleModel -> CLIENT_OWNER.equals(roleModel.getName())).findFirst();
    if (rModel.isPresent() && rModel.get().isClientRole()) {
      RoleContainerModel container = rModel.get().getContainer();
      ClientModel client = (ClientModel) container;
      LOG.info("Client with id " + client.getClientId() + " is present for user " +  user.getUsername());
      return client;
    }
    LOG.info("No Client found with client_owner role for user : "+user.getUsername());
    return null;
  }

  private UserModel getUserBasedOnEmail(String email) {
    Map<String, String> filter = new HashMap<>();
    filter.put(UserModel.EMAIL,email);
    Optional<UserModel> userForEmail = userProvider.searchForUserStream(realm, filter).findFirst();
    return userForEmail.orElse(null);
  }

}
