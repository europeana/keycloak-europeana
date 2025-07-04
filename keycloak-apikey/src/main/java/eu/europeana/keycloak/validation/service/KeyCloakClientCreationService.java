package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.exception.KeyCreationException;
import eu.europeana.keycloak.validation.util.Constants;
import eu.europeana.keycloak.validation.util.PassGenerator;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.RandomUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.UserModel;
/**
 * Service for creating the keyclock client object along with necessary roles
 */
public class KeyCloakClientCreationService {

  private static final Logger LOG = Logger.getLogger(KeyCloakClientCreationService.class);
  private final KeycloakSession session;
   private final RealmModel realm;
   private final String clientID;
  private final String clientName;

  /**
   * Constructs KeyCloakClientCreationService by initializing
   * @param session KeycloakSession
   * @param clientID ID of client
   * @param clientName name of client
   */
   public KeyCloakClientCreationService(KeycloakSession session,String clientID,String clientName) {
     this.session = session;
     this.realm =session.getContext().getRealm();
     this.clientID = clientID;
     this.clientName = clientName;
   }

   public Apikey registerKey(UserModel user,String keyType,String keyDescription) {
     String apikey = (this.clientID == null)? generateApikeyName():clientID;
     ClientModel client = createNewKey(apikey,keyDescription);
     Date creationDate = new Date();
     RoleModel clientRole = createClientRole(keyType, client, creationDate);
     if (clientRole != null) {
         user.grantRole(clientRole);
         return new Apikey(client.getId(), client.getClientId(),
             keyType, creationDate, client.getName(), client.getDescription(), null);
        }
     return null;
   }

  private RoleModel createClientRole(String keyType, ClientModel client, Date creationDate) {
    if(Constants.PERSONAL_KEY.equals(keyType)) {
        return createOwnerRole(client, creationDate, keyType,Constants.CLIENT_OWNER_ROLE_DESCRIPTION);
    } else if (Constants.PROJECT_KEY.equals(keyType)){
      return createOwnerRole(client, creationDate, keyType,Constants.SHARED_OWNER_ROLE_DESCRIPTION);
    }
    return null;
  }

  private ClientModel createNewKey(String clientID,String keyDescription) {
     try {
       ClientProvider clientProvider = session.clients();
       ClientModel client = clientProvider.addClient(realm, clientID);
       client.setName(clientName);
       client.setDescription(keyDescription);
       client.setProtocol("openid-connect");
       client.setPublicClient(true);
       client.setStandardFlowEnabled(false);
       client.setImplicitFlowEnabled(false);
       client.setDirectAccessGrantsEnabled(false);
       client.setServiceAccountsEnabled(false);
       client.setEnabled(true);
       LOG.info("Created new client : "+ client.getName() + " "+client.getClientId());
       return client;
     }catch (Exception ex){
       LOG.error("Unable to create new Client !! " + ex.getMessage());
       throw new KeyCreationException(ex.getMessage(),ex);
     }
  }

  private RoleModel createOwnerRole(ClientModel client, Date roleCreationDate,String roleName,String roleDescription) {
    try {
      RoleProvider roles = session.roles();
      RoleModel roleModel = roles.addClientRole(client, roleName);
      roleModel.setDescription(roleDescription);
      roleModel.setAttribute(Constants.ROLE_ATTRIBUTE_CREATION_DATE,List.of(getDateString(roleCreationDate)));
      return roleModel;
    }catch (Exception e){
      LOG.error("Unable to create new role for client !! " +client.getClientId()+ e.getMessage());
      return null;
    }
  }

  private String getDateString(Date  now) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.CREATION_DATE_PATTERN).withZone(ZoneOffset.UTC);
    return formatter.format(now.toInstant());
  }

  private String generateApikeyName() {
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
}