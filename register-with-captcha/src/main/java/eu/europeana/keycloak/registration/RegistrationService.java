package eu.europeana.keycloak.registration;

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
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

public class RegistrationService {
  private static final Logger LOG = Logger.getLogger(RegistrationService.class);

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
    LOG.info("Input :" + input + " Headers: " + session.getContext().getHttpRequest().getHttpHeaders());
    if(input!=null && StringUtils.isNotEmpty(input.getEmail())){
      UserModel user =getUserBasedOnEmail(input.getEmail());
      if(user !=null) {
        LOG.info(" Username for the provided email is : " + user.getUsername());
        return Response.ok().build();
      }
      else {
        return Response.status(Status.BAD_REQUEST).entity(String.format("An Europeana account was not found associated to the email address %s",input.getEmail())).build();
      }
    }
    return Response.status(Status.BAD_REQUEST).entity("The email field is missing").build();
  }

  private UserModel getUserBasedOnEmail(String email) {
    Map<String, String> filter = new HashMap<>();
    filter.put(UserModel.EMAIL,email);
    Optional<UserModel> userForEmail = userProvider.searchForUserStream(realm, filter).findFirst();
    return userForEmail.isPresent()? userForEmail.get():null;
  }

}
