package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.exception.SDKException;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.record.APIException;
import com.zoho.crm.api.record.ActionHandler;
import com.zoho.crm.api.record.ActionResponse;
import com.zoho.crm.api.record.ActionWrapper;
import com.zoho.crm.api.record.BodyWrapper;
import com.zoho.crm.api.record.Field;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.record.SuccessResponse;
import com.zoho.crm.api.util.APIResponse;
import com.zoho.crm.api.util.Model;


public class KeycloakToZohoSyncService {
  private final KeycloakSession session;
  private final RealmModel realm;
  private final UserProvider userProvider;

  private static final Logger LOG = Logger.getLogger(KeycloakToZohoSyncService.class);

  public KeycloakToZohoSyncService(KeycloakSession session){
    this.session = session;
    this.realm = session.getContext().getRealm();
    this.userProvider = session.users();
  }
  private Set<String> getParticipationLevel(UserModel usermodel) {
    Set<String>  participationLevel = new HashSet<>();
    participationLevel.add("Account holder");
    if(usermodel.getRoleMappingsStream().anyMatch(
        roleModel -> ("client_owner".equals(roleModel.getName())))){
      participationLevel.add("API User");
    }
    if(usermodel.getRoleMappingsStream().anyMatch(
        roleModel -> ("shared_owner".equals(roleModel.getName())))){
      participationLevel.add("API Customer");
    }
    return participationLevel;
  }
  /**This method is used to update a single contact of zoho with ID and print the response.
   * @param email - email for the new contact
   * @param lastName - Name for the contact.This can be keycloak username or the combination of first and last name from keycloak
   * @throws Exception
   */
  public void createNewZohoContact(String email, String lastName,Set<String> partiCipationLevel) throws SDKException, IllegalAccessException {
    String moduleAPIName = "Contacts";
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper bodyWrapper = new BodyWrapper();
    List<Record> records = new ArrayList<>();
    Record newRecord = new Record();
    newRecord.addFieldValue(Field.Contacts.LAST_NAME, lastName);
    newRecord.addKeyValue("Email",email);
    newRecord.addKeyValue("Contact_Participation",partiCipationLevel);
    records.add(newRecord);
    bodyWrapper.setData(records);
    HeaderMap headerInstance = new HeaderMap();

    LOG.info("Creation request :" + bodyWrapper);

    APIResponse<ActionHandler> response = recordOperations.createRecords(bodyWrapper,
        headerInstance);
    processResponse(response);
  }

  /**This method is used to update a single contact of zoho with ID and print the response.
   * @param recordId - The Zoho ID of the record to be updated.
   * @param userAccountID - keycloak user public id
   * @param participationLevel - Indicates the type of user e.g. API Customer,API User etc.
   * @throws Exception - SDKException, IllegalAccessException
   */
  public void updateZohoContact(long recordId,String userAccountID,Set<String> participationLevel) throws SDKException, IllegalAccessException {
    String moduleAPIName = "Contacts";
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper request = new BodyWrapper();
    List<Record> records = new ArrayList<>();
    Record recordToUpdate = new Record();
    recordToUpdate.addKeyValue("User_Account_ID",userAccountID);
    recordToUpdate.addKeyValue("Contact_Participation",participationLevel);
    records.add(recordToUpdate);
    request.setData(records);
    HeaderMap headerInstance = new HeaderMap();
    LOG.info("Update request :" + request);
    APIResponse<ActionHandler> response = recordOperations.updateRecord(recordId, request,
        headerInstance);
    processResponse(response);
  }
  private void processResponse(APIResponse<ActionHandler> response) throws IllegalAccessException {
    if (response != null) {
      if (response.isExpected()) {
        ActionHandler actionHandler = response.getObject();
        if (actionHandler instanceof ActionWrapper actionWrapper) {
          List<ActionResponse> actionResponses = actionWrapper.getData();
          for (ActionResponse actionResponse : actionResponses) {
            if (actionResponse instanceof SuccessResponse successResponse) {
              LOG.info("Message: " + successResponse.getMessage().getValue());
            }
            else if (actionResponse instanceof APIException exception) {
              LOG.error("Status: " + exception.getStatus().getValue());
              LOG.error("Code: " + exception.getCode().getValue());
              LOG.error("Details: ");
              for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
                LOG.error(entry.getKey() + ": " + entry.getValue());
              }
              LOG.error("Message: " + exception.getMessage().getValue());
            }
          }
        }
        else if (actionHandler instanceof APIException exception) {
          LOG.error("Status: " + exception.getStatus().getValue());
          LOG.error("Code: " + exception.getCode().getValue());
          LOG.error("Details: ");
          for (Map.Entry<String, Object> entry : exception.getDetails().entrySet()) {
            LOG.error(entry.getKey() + ": " + entry.getValue());
          }
          LOG.error("Message: " + exception.getMessage().getValue());
        }
      } else {
        Model responseObject = response.getModel();
        Class<? extends Model> clas = responseObject.getClass();
        java.lang.reflect.Field[] fields = clas.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
          LOG.error(field.getName() + ":" + field.get(responseObject));
        }
      }
    }
  }

  public Map<String, String> handleZohoUpdate(Contact contact) {
    try {
      String primaryMail = contact.getEmail();
      String secondaryMail= contact.getSecondaryEmail();
      //check if contact exists in keycloak , if not then disassociate it
      UserModel keycloakUser = userProvider.getUserByEmail(realm,primaryMail);
      if (keycloakUser == null) {
        //dissociate the contact i.e. set user_account_id as null
        //TODO- Uncomment for production
        //updateZohoContact(Long.parseLong(contact.getID()),null,null);

      } else {
        boolean isPartOfTestGroup = keycloakUser.getGroupsStream()
            .anyMatch(group -> "Europeana Test Users".equals(group.getName()));
        //Skip the users who belong to test groups
        if (!isPartOfTestGroup) {
          String userAccountId = keycloakUser.getId();
          Set<String> participationLevel = getParticipationLevel(keycloakUser);
          UserModel keycloakUserForSecondaryMail = userProvider.getUserByEmail(realm,
              secondaryMail);
          if (keycloakUserForSecondaryMail != null) {
            participationLevel.addAll(getParticipationLevel(keycloakUserForSecondaryMail));
          }
          //TODO - update only if the values are changed
          if (keycloakUser.getUsername().contains("snazare")) {
            updateZohoContact(Long.parseLong(contact.getID()), userAccountId, participationLevel);
          }
        }
      }
    }
    catch (Exception e){
      LOG.error("Exception occured while creating new contact. "+ e);
    }
    return null;
  }

  public int validateAndCreateZohoContact(List<Contact> zohoContacts) {
    int count = 0;
    try {
      //Separated zoho contacts
      Map<String, Contact> zohoContactsByPrimaryMail = new HashMap<>();
      Map<String, Contact> zohoContactsBySecondaryMail = new HashMap<>();

      for (Contact c : zohoContacts) {
        if (StringUtils.isNotEmpty(c.getEmail())) {
          zohoContactsByPrimaryMail.put(c.getEmail(), c);
        }
        if (StringUtils.isNotEmpty(c.getSecondaryEmail())) {
          zohoContactsBySecondaryMail.put(c.getSecondaryEmail(), c);
        }
      }
      EntityManager entityManager = session.getProvider(JpaConnectionProvider.class)
          .getEntityManager();
      CustomUserDetailsRepository repo = new CustomUserDetailsRepository(entityManager);
      //keyCloak Users
      List<KeycloakUser> keycloakUsers = repo.findKeycloakUsers();
      for (KeycloakUser user : keycloakUsers) {
        if (!zohoContactsByPrimaryMail.containsKey(user.getEmail())) {
          //The keycloak user is not part of zoho yet , create new contact in zoho
          String contactName = user.getFirstName() + " " + user.getLastName();
          if (StringUtils.isEmpty(contactName)) {
            contactName = user.getUsername();
          }
          List<String> participationLevel = List.of("Account holder");
          if (StringUtils.isNotEmpty(user.getAssociatedRoleType())) {
            participationLevel = Arrays.stream(user.getAssociatedRoleType().split(",")).toList();
          }
          if (contactName.contains("snazare")) {
            createNewZohoContact(user.getEmail(), contactName, new HashSet<>(participationLevel));
          }
          count++;
        }
      }
    }catch (SDKException | IllegalAccessException e){
      LOG.error("Error occured while creating contact : "+ e);
    }
    return count;
  }
}