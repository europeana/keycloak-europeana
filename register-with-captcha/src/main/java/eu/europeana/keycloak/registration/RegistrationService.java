package eu.europeana.keycloak.registration;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
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
  public Response registerWithCaptcha(RegistrationInput input){
    LOG.info("Input :" + input+ " Headers: " + session.getContext().getHttpRequest().getHttpHeaders());
    if(input!=null && StringUtils.isNotEmpty(input.getEmail())){
      UserModel user =getUserBasedOnEmail(input.getEmail());
      if(user !=null) {
        LOG.info(" Username for the provided email is : " + user.getUsername());
        ClientModel client =  getClientAssociatedToUser(user);
        if(client == null)  {
          client = createClientWithNewKey(user);
          RoleModel newRoleForClient = createNewRoleForClient(client);
          LOG.info("Role created " + newRoleForClient.getName());
          user.grantRole(newRoleForClient);
        }
        //validate new user
        //send mail to accepted email id
        return Response.ok().entity("Created new Client, Role for user " ).build();
      }
      else {
        return Response.status(Status.BAD_REQUEST).entity(String.format("An Europeana account was not found associated to the email address %s",input.getEmail())).build();
      }
    }
    return Response.status(Status.BAD_REQUEST).entity("The email field is missing").build();
  }

  private RoleModel createNewRoleForClient(ClientModel client) {
    RoleProvider roles = session.roles();
    return  roles.addClientRole(client, "client_owner");
  }

  private ClientModel createClientWithNewKey(UserModel user) {
    String apiKey = createApiKey();
     //Create a new client with the API key as ID.
      KeyCloakClientCreationService service = new KeyCloakClientCreationService(realm, session,
          apiKey, user.getUsername());
      return service.registerClient();
  }

  private String createApiKey() {
      LOG.info("Gerating new API key !!");
      String id ;
      ClientProvider clientProvider = session.clients();
      List<String> clientIdList = clientProvider.getClientsStream(realm).map(ClientModel::getClientId).toList();
      LOG.info("Client ID List :" + clientIdList);
      PassGenerator pg = new PassGenerator();
      do {
        id = pg.generate(RandomUtils.nextInt(8, 13));
        LOG.info("Generated Key " + id );
      } while (clientIdList.contains(id));
      LOG.info("Created new API key : "+ id );
    return id;

  }

  private ClientModel getClientAssociatedToUser(UserModel user) {
    Optional<RoleModel> rModel = user.getRoleMappingsStream().filter(
        roleModel -> CLIENT_OWNER.equals(roleModel.getName())).findFirst();
    if (rModel.isPresent() && rModel.get().isClientRole()) {
      RoleContainerModel container = rModel.get().getContainer();
      LOG.info("Client present for user " + container.getId());
      return (ClientModel) container;
    }
    return null;
  }

  private UserModel getUserBasedOnEmail(String email) {
    Map<String, String> filter = new HashMap<>();
    filter.put(UserModel.EMAIL,email);
    Optional<UserModel> userForEmail = userProvider.searchForUserStream(realm, filter).findFirst();
    return userForEmail.orElse(null);

  }

}
