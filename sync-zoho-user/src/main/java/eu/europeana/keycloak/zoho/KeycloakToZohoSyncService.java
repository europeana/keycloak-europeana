package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.Field.Contacts;
import com.zoho.crm.api.util.Choice;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.keycloak.models.jpa.entities.UserEntity;


public class KeycloakToZohoSyncService {

  public static final String CLIENT_OWNER = "client_owner";
  public static final String SHARED_OWNER = "shared_owner";
  public static final String ACCOUNT_HOLDER = "Account holder";
  public static final String API_USER = "API User";
  public static final String API_CUSTOMER = "API Customer";
  private final KeycloakSession session;
  private final RealmModel realm;
  private final UserProvider userProvider;
  public  List<String> updatedContacts = new ArrayList<>();
  private static final Logger LOG = Logger.getLogger(KeycloakToZohoSyncService.class);

  public KeycloakToZohoSyncService(KeycloakSession session){
    this.session = session;
    this.realm = session.getContext().getRealm();
    this.userProvider = session.users();
  }
  private Set<String> getParticipationLevel(UserModel usermodel) {
    Set<String>  participationLevel = new HashSet<>();
    participationLevel.add(ACCOUNT_HOLDER);
    if(usermodel.getRoleMappingsStream().anyMatch(
        roleModel -> (CLIENT_OWNER.equals(roleModel.getName())))){
      participationLevel.add(API_USER);
    }
    if(usermodel.getRoleMappingsStream().anyMatch(
        roleModel -> (SHARED_OWNER.equals(roleModel.getName())))){
      participationLevel.add(API_CUSTOMER);
    }

    return participationLevel;
  }
  /**This method is used to create a single contact of zoho with ID and print the response.
   * @param userAccountID - keycloak public id of user
   * @param email - email for the new contact
   * @param firstName - firstName of user
   * @param lastName - Name for the contact.This can be keycloak username if user does not have first and last name populated in keycloak
   */
  public boolean createNewZohoContact(String userAccountID ,String email,String firstName,String lastName,Set<String> participationLevel) throws SDKException, IllegalAccessException {
    String moduleAPIName = "Contacts";
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper bodyWrapper = new BodyWrapper();

    Record newRecord = new Record();
    newRecord.addFieldValue(Contacts.FIRST_NAME, firstName);
    newRecord.addFieldValue(Field.Contacts.LAST_NAME, lastName);
    newRecord.addKeyValue("Email",email);

    List<Choice<String>> participationLevelList = new ArrayList<>();
    if(participationLevel!=null)
      participationLevel.forEach(p-> participationLevelList.add(new Choice<>(p)));

    newRecord.addKeyValue("Contact_Participation",new ArrayList<>(participationLevelList));
    newRecord.addKeyValue("Lead_Source",new Choice<>("Europeana account sign-up form"));
    newRecord.addKeyValue("User_Account_ID",userAccountID);

    List<Record> records = new ArrayList<>();
    records.add(newRecord);
    bodyWrapper.setData(records);

    HeaderMap headerInstance = new HeaderMap();
    APIResponse<ActionHandler> response = recordOperations.createRecords(bodyWrapper,
        headerInstance);
    return processResponse(response);
  }

  /**This method is used to update a single contact of zoho with ID and print the response.
   * @param recordId - The Zoho ID of the record to be updated.
   * @param userAccountID - keycloak user public id
   * @param participationLevel - Indicates the type of user e.g. API Customer,API User etc.
   */
  public boolean updateZohoContact(long recordId,String userAccountID,Set<String> participationLevel) throws SDKException {
    String moduleAPIName = "Contacts";
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper request = new BodyWrapper();
    List<Record> records = new ArrayList<>();
    Record recordToUpdate = new Record();

    recordToUpdate.addKeyValue("User_Account_ID",userAccountID);
    List<Choice<String>> participationLevelList = new ArrayList<>();
    if(participationLevel!=null)
       participationLevel.forEach(p-> participationLevelList.add(new Choice<>(p)));
    recordToUpdate.addKeyValue("Contact_Participation",participationLevelList);
    records.add(recordToUpdate);

    request.setData(records);
    HeaderMap headerInstance = new HeaderMap();
    LOG.info("Updating  zoho contact id :" + recordId);
    APIResponse<ActionHandler> response = recordOperations.updateRecord(recordId, request,
        headerInstance);
   return  processResponse(response);
  }

  private boolean processResponse(APIResponse<ActionHandler> response){
    if (response != null && response.isExpected()) {
      if (response.getObject() instanceof ActionWrapper actionWrapper) {
        for (ActionResponse actionResponse : actionWrapper.getData()) {
          if (actionResponse instanceof SuccessResponse) {
            return true;
          } else if (actionResponse instanceof APIException exception) {
            LOG.error("Status: " + exception.getStatus().getValue() + " Code:" + exception.getCode().getValue()+" Message: "+ exception.getMessage().getValue());
            return false;
          }
        }
      } else if (response.getObject() instanceof APIException exception) {
        LOG.error(" Status: " + exception.getStatus().getValue() + " Code:" + exception.getCode().getValue()+" Message: "+ exception.getMessage().getValue());
        return false;
      }
    }
    return false;
  }
  public  void handleZohoUpdate(Contact contact) {
    try {
      String primaryMail = contact.getEmail();
      //check if contact exists in keycloak , if not then disassociate it
      UserModel keycloakUser = userProvider.getUserByEmail(realm,primaryMail);
      if (keycloakUser == null) {
        //dissociate the contact i.e. set the user_account_id  as null and remove API related participation levels
        if(isSyncEnabled() && StringUtils.isNotEmpty(contact.getUserAccountId())){
          updateZohoContact(Long.parseLong(contact.getId()),null, getUpdatedParticipationLevels(contact));
        }
      } else {
        boolean isPartOfTestGroup = keycloakUser.getGroupsStream()
            .anyMatch(group -> "Europeana Test Users".equals(group.getName()));
        //Skip the users who belong to test groups
        if (!isPartOfTestGroup) {
          Set<String> participationLevel = getParticipationLevel(keycloakUser);
          //If Secondary mail is present then consider the private/project keys of that user
          updateParticipationBasedOnsecondaryMail(contact.getSecondaryEmail(), participationLevel);
          if (isSyncEnabled() && isToUpdateContact(contact,keycloakUser,participationLevel)) {
           if(updateZohoContact(Long.parseLong(contact.getId()), keycloakUser.getId(), participationLevel)) {
             updatedContacts.add(contact.getId() + ":" + contact.getEmail());
           }
          }
        }
      }
    }
    catch (Exception e){
      LOG.error("Exception occured while updating  contact. "+ e);
    }
  }

  /**
   * Control if actual updates in ZOHO to be made by the sync job.   *
   * @return boolean
   */
  private boolean isSyncEnabled() {
    String enableKeycloakToZohoSync = System.getenv("ENABLE_KEYCLOAK_TO_ZOHO_SYNC");
    return StringUtils.isNotEmpty(enableKeycloakToZohoSync) && "true".equals(enableKeycloakToZohoSync);
  }

  private static Set<String> getUpdatedParticipationLevels(Contact contact) {
    if(StringUtils.isEmpty(contact.getContactParticipation())) {
         return Collections.emptySet();
    }
    return Arrays.stream(contact.getContactParticipation().split(";")).filter(p ->
            API_CUSTOMER.equals(p) || API_USER.equals(p) || ACCOUNT_HOLDER.equals(p))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private boolean isToUpdateContact(Contact zohoContact, UserModel keycloakUser,
      Set<String> participationLevel) {
    //check if  user Account id is changed
    if(!keycloakUser.getId().equals(zohoContact.getUserAccountId()))
      return true;
    //check if the name is changed
    if (isContactNameChanged(zohoContact, keycloakUser)) {
      return true;
    }
    //Check if participation level changed
    return isparticipationLevelChanged(zohoContact, participationLevel);
  }

  private static boolean isparticipationLevelChanged(Contact zohoContact, Set<String> participationLevel) {
    if(StringUtils.isNotEmpty(zohoContact.getContactParticipation())) {
      String[] levels = zohoContact.getContactParticipation().split(";");
      return levels.length > 0 && Arrays.stream(levels)
          .anyMatch(p -> !participationLevel.contains(p));
    }
    return false;
  }

  private static boolean isContactNameChanged(Contact zohoContact, UserModel keycloakUser) {
    if(StringUtils.isNotEmpty(keycloakUser.getFirstName()) &&
        !keycloakUser.getFirstName().equals(zohoContact.getFirstName())){
      return true;
    }
    return StringUtils.isNotEmpty(keycloakUser.getLastName()) &&
        !keycloakUser.getLastName().equals(zohoContact.getLastName());
  }

  private void updateParticipationBasedOnsecondaryMail(String secondaryMail, Set<String> participationLevel) {
    if(StringUtils.isNotEmpty(secondaryMail)) {
      UserModel keycloakUserForSecondaryMail = userProvider.getUserByEmail(realm,
          secondaryMail);
      if (keycloakUserForSecondaryMail != null) {
        participationLevel.addAll(getParticipationLevel(keycloakUserForSecondaryMail));
      }
    }
  }

  public int validateAndCreateZohoContact(List<Contact> zohoContacts) {
    int count = 0;
    List<String> newContacts= new ArrayList<>();
    try {
      //Separated zoho contacts
      Map<String, Contact> zohoContactsByPrimaryMail = new HashMap<>();
      for (Contact c : zohoContacts) {
        if (StringUtils.isNotEmpty(c.getEmail())) {
          zohoContactsByPrimaryMail.put(c.getEmail(), c);
        }
      }
      EntityManager entityManager = session.getProvider(JpaConnectionProvider.class)
          .getEntityManager();
      CustomUserDetailsRepository repo = new CustomUserDetailsRepository(entityManager);
      //Iterate over keyCloak Users
      List<UserEntity> keycloakUsers = repo.findKeycloakUsers();
      for (UserEntity user : keycloakUsers) {
        if (!zohoContactsByPrimaryMail.containsKey(user.getEmail())) {
          //The keycloak user is not part of zoho yet , create new contact in zoho
          String firstName = user.getFirstName();
          String lastName = user.getLastName();
          if (StringUtils.isEmpty(firstName) && StringUtils.isEmpty(lastName)) {
            lastName = user.getUsername();
          }
          Set<String> participationLevel = calculateParticipationLevel(user,repo);
         if(isSyncEnabled() && createNewZohoContact(user.getId(),user.getEmail(), firstName, lastName,(participationLevel))) {
          newContacts.add(user.getLastName());
          count++;
         }
        }
      }
    }catch (SDKException | IllegalAccessException e){
      LOG.error("Error occured while creating contact : "+ e);
    }
    LOG.info("New contacts :" + newContacts);
    return count;
  }

  private Set<String> calculateParticipationLevel(UserEntity id, CustomUserDetailsRepository repo) {
    List<String> userRoles = repo.findUserRoles(id);
    Set<String> participationLevels = new HashSet<>();
    participationLevels.add(ACCOUNT_HOLDER);
    if(userRoles.contains(SHARED_OWNER)){participationLevels.add(API_CUSTOMER);}
    if(userRoles.contains(CLIENT_OWNER)){participationLevels.add(API_USER);}
    return participationLevels;
  }
}