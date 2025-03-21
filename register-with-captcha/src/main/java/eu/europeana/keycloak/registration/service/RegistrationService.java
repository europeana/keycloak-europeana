package eu.europeana.keycloak.registration.service;

import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.RESUBMIT_CAPTCHA;
import static eu.europeana.keycloak.registration.config.CaptachaManagerConfig.CAPTCHA_PATTERN;

import eu.europeana.keycloak.registration.datamodel.ErrorResponse;
import eu.europeana.keycloak.registration.datamodel.RegistrationInput;
import eu.europeana.keycloak.registration.exception.CaptchaException;
import eu.europeana.keycloak.registration.util.PassGenerator;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
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
import org.keycloak.services.resources.Cors;
import org.keycloak.utils.EmailValidationUtil;

public class RegistrationService {

  public static final String CLIENT_OWNER = "client_owner";
  private static final Logger LOG = Logger.getLogger(RegistrationService.class);
  public static final String ACCOUNT_NOT_FOUND_FOR_EMAIL = "An Europeana account was not found associated to the email address %s, please register for one in the Europeana website first before filling in this form";
  public static final String EMAIL_NOT_VERIFIED ="Please confirm your account by clicking on the link that was sent to you when registering for an Europeana account";
  public static final String ACCOUNT_DISABLED="Please contact Europeana customer support for further information";
  public static final String CLIENT_OWNER_ROLE_DESCRIPTION = "Ownership of this client";
  private final KeycloakSession session;
  private final RealmModel realm;
  private final UserProvider userProvider;

  private final HttpRequest request;

  private Cors cors;

  public RegistrationService(KeycloakSession session) {
    this.session =session;
    this.realm = session.getContext().getRealm();
    this.userProvider = session.users();
    this.request = session.getContext().getHttpRequest();
  }

  private void setupCors() {
    this.cors = Cors.add(this.request)
        .auth().allowedMethods("POST")
        .allowAllOrigins();
  }

  @Path("")
  @OPTIONS
  public Response registerWithCaptchaPreflight() {
    return Cors.add(this.request, Response.ok())
        .auth().allowedMethods("POST","OPTIONS")
        .auth().preflight().build();
  }

  @Path("")
  @POST
  @Consumes({MediaType.APPLICATION_JSON})
  @Produces({MediaType.APPLICATION_JSON})
  public Response registerWithCaptcha(RegistrationInput input){
    try {
      setupCors();
      verifyCaptcha();
      //input Email id validations

      if (input == null || StringUtils.isEmpty(input.getEmail())) {
        return this.cors.builder(Response.status(Status.BAD_REQUEST)
            .entity(new ErrorResponse("400-missing-email","The email address is missing","Please fill the email address with the email associated to your Europeana account"))).build();
      }
      String email = input.getEmail().toLowerCase();
      validateEmail(email);
      //Validate User
      UserModel user = userProvider.getUserByEmail(realm, email);
      if (user == null) {
        return this.cors.builder(Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("400-account-unknown","Europeana account not known",
            String.format(ACCOUNT_NOT_FOUND_FOR_EMAIL, input.getEmail())))).build();
      }
      if(!user.isEmailVerified()){
        return this.cors.builder(Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("400-account-unconfirmed","Europeana account not yet confirmed",
            EMAIL_NOT_VERIFIED))).build();
      }
      if(!user.isEnabled()){
        return this.cors.builder(Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("400-account-disabled","Europeana account has been disabled",
            ACCOUNT_DISABLED))).build();
      }

      LOG.info("Found user: "+ user.getUsername() + " for provided email : " + input.getEmail());
      updateKeyAndNotifyUser(user);

      return this.cors.builder(Response.ok().entity("")).build();
    }
    catch (CaptchaException ex){
      return this.cors.builder(Response.status(Status.BAD_REQUEST)
          .entity(new ErrorResponse("400-captcha-error","There was a problem validating reCaptcha",
              ex.getMessage()))).build();
    }
    catch (EmailException ex){
      return this.cors.builder(Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("500-Internal-Server-Error","Internal Server Error",ex.getMessage()))).build();
    }
  }

  private void validateEmail(String email) throws EmailException {
    if(!EmailValidationUtil.isValidEmail(email)){
      throw new EmailException("Invalid Email Id");
    }
  }

  private void updateKeyAndNotifyUser(UserModel user) throws EmailException {
    String apikey = getOrCreateApikeyForUser(user);
    //send mail to accepted email id i.e. email of user
   if(StringUtils.isEmpty(apikey)) {
     LOG.error("User not registered. No Apikey will be returned!!");
     return;
   }
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
      return client.getClientId();
    }
    return null;
  }

  private ClientModel getOrCreateClientForUser(UserModel user) {
    return Optional.ofNullable(getClientAssociatedToUser(user))
        .orElseGet(() -> {
          var newClient = createClientWithNewKey(user);
          LOG.info("New client : " + newClient.getClientId() + " is created for user : "+ user.getUsername());
          return newClient;
        });
  }

  private void verifyCaptcha() {
    String captchaToken = getAuthorizationHeader(session.getContext().getHttpRequest());
    CaptchaManager captchaManager = new CaptchaManager();
    if (captchaToken == null || !captchaManager.verifyCaptchaToken(captchaToken)) {
      LOG.info(RESUBMIT_CAPTCHA);
      throw new CaptchaException(RESUBMIT_CAPTCHA);
    }
  }

  private String getAuthorizationHeader(HttpRequest httpRequest) {
      String authorization = httpRequest.getHttpHeaders().getHeaderString(HttpHeaders.AUTHORIZATION);
      if (authorization != null) {
        try {
          Pattern pattern = Pattern.compile(CAPTCHA_PATTERN);
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
    RoleModel roleModel = roles.addClientRole(client, CLIENT_OWNER);
    if (roleModel != null) {
      roleModel.setDescription(CLIENT_OWNER_ROLE_DESCRIPTION);
    }
    return roleModel;
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


}
