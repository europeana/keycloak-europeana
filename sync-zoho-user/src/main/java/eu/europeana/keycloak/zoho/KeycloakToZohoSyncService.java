package eu.europeana.keycloak.zoho;

import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.util.Choice;
import eu.europeana.keycloak.zoho.batch.ZohoBatchUpdater;
import eu.europeana.keycloak.zoho.datamodel.APIProject;
import eu.europeana.keycloak.zoho.datamodel.Contact;
import eu.europeana.keycloak.zoho.datamodel.KeycloakClient;
import eu.europeana.keycloak.zoho.datamodel.KeycloakUser;
import eu.europeana.keycloak.zoho.repo.CustomUserDetailsRepository;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
import com.zoho.crm.api.HeaderMap;
import com.zoho.crm.api.record.ActionHandler;
import com.zoho.crm.api.record.BodyWrapper;
import com.zoho.crm.api.record.Field;
import com.zoho.crm.api.record.RecordOperations;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.util.APIResponse;

/**
 * Service to check and sync specific fields for the zoho contact or create new contact in zoho
 */

public class KeycloakToZohoSyncService {

  private static final Logger LOG = Logger.getLogger(KeycloakToZohoSyncService.class);
  public static final String CLIENT_OWNER = "client_owner";
  public static final String SHARED_OWNER = "shared_owner";
  public static final String ACCOUNT_HOLDER = "Account holder";
  public static final String API_USER = "API User";
  public static final String API_CUSTOMER = "API Customer";
  private final  EntityManager entityManager;
  private final CustomUserDetailsRepository repo;
  private final RealmModel realm;
  private final List<String> updatedContacts = new ArrayList<>();
  private Map<String, KeycloakUser> userdetails = new HashMap<>();
  private Map<String, KeycloakClient> clientdetails = new HashMap<>();
  private List<String> testUserIds = new ArrayList<>();
  private  ZohoBatchUpdater updater ;

  /**
   * Initialize KeycloakToZohoSyncService
   * @param session Keycloak session
   */
  public KeycloakToZohoSyncService(KeycloakSession session){
    this.realm = session.getContext().getRealm();
    this.entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    this.repo = new CustomUserDetailsRepository(entityManager);
    this.updater = new ZohoBatchUpdater();
  }

  public List<String> getUpdatedContactList(){
    return updatedContacts;
  }

  /**
   * Initialize userdetails map and the userids belonging to test group
   */
  public void loadKeycloakUsersAndGroups(){
    userdetails =  repo.listAllUserMails(realm.getName());
    //calculate users belonging to test Group
    String testGroupId   =  repo.findTestGroupId();
    if(testGroupId!=null){
      testUserIds = repo.findTestGroupUsers(testGroupId);
    }
  }


  /**This method is used to create a single contact of zoho with ID and print the response.
   * @param userAccountID - keycloak public id of user
   * @param email - email for the new contact
   * @param firstName - firstName of user
   * @param lastName - Name for the contact.This can be keycloak username if user does not have first and last name populated in keycloak
   * @param participationLevel - contacts participation level
   * @return boolean true if contact created successfully
   * @throws SDKException -
   * */
  public boolean createZohoContact(String userAccountID ,String email,String firstName,String lastName,Set<String> participationLevel) throws SDKException {
    String moduleAPIName = "Contacts";
    RecordOperations recordOperations = new RecordOperations(moduleAPIName);
    BodyWrapper bodyWrapper = new BodyWrapper();

    Record newRecord = new Record();
    newRecord.addFieldValue(Field.Contacts.FIRST_NAME, firstName);
    newRecord.addFieldValue(Field.Contacts.LAST_NAME, lastName);
    newRecord.addKeyValue("Email",email);

    List<Choice<String>> participationLevelList = getParticipationChoice(
        participationLevel);

    newRecord.addKeyValue("Contact_Participation",new ArrayList<>(participationLevelList));
    newRecord.addKeyValue("Lead_Source",new Choice<>("Europeana account sign-up form"));
    newRecord.addKeyValue("User_Account_ID",userAccountID);

    List<Record> records = new ArrayList<>();
    records.add(newRecord);
    bodyWrapper.setData(records);

    HeaderMap headerInstance = new HeaderMap();
    APIResponse<ActionHandler> response = recordOperations.createRecords(bodyWrapper,
        headerInstance);
    return updater.processResponse(response);
  }

  private static List<Choice<String>> getParticipationChoice(Set<String> participationLevel) {
    List<Choice<String>> participationLevelList = new ArrayList<>();
    if(participationLevel !=null)
      participationLevel.forEach(p-> participationLevelList.add(new Choice<>(p)));
    return participationLevelList;
  }

  /**This method is used to update a single contact of zoho with ID and print the response.
   * @param recordId - The Zoho ID of the record to be updated.
   * @param userAccountID - keycloak user public id
   * @param participationLevel - Indicates the type of user e.g. API Customer,API User etc.
   * @return boolean true if contact updated successfully
   * @throws SDKException -
   */
  public boolean updateZohoContact(long recordId,String userAccountID,Set<String> participationLevel) throws SDKException {

    List<Record> records = new ArrayList<>();
    Record recordToUpdate = new Record();
    recordToUpdate.addKeyValue("User_Account_ID",userAccountID);
    recordToUpdate.addKeyValue("Contact_Participation",getParticipationChoice(participationLevel));
    records.add(recordToUpdate);

    RecordOperations recordOperations = new RecordOperations("Contacts");
    BodyWrapper request = new BodyWrapper();
    request.setData(records);
    LOG.info("Updating  zoho contact id :" + recordId);
    APIResponse<ActionHandler> response = recordOperations.updateRecord(recordId, request,new HeaderMap());
    return  updater.processResponse(response);
  }

  /**
   * Update the contact in zoho
   * @param contact object
   */
  public  void handleZohoUpdate(Contact contact) {
    try {
      KeycloakUser keycloakUser = userdetails.get(contact.getEmail());
      if (keycloakUser == null) {
        // if zoho contact not part of keycloak , then disassociate it in zoho
        handleUserDissociation(contact);
      } else {
        //update contact association in zoho if required
        handleZohoContactUpdate(contact, keycloakUser);
      }
    }catch (SDKException e){
      LOG.error("Exception occurred while updating  contact. "+ e);
    }
  }

  private void handleUserDissociation(Contact contact) throws SDKException {
    //dissociate the associated contact i.e. change the user_account_id  to null and remove API related participation levels
    if(isSyncEnabled() && StringUtils.isNotEmpty(contact.getUserAccountId())){
      if(updateZohoContact(Long.parseLong(contact.getId()),null, removeAPIRelatedParticipation(contact.getContactParticipation()))){
        updatedContacts.add(contact.getId() + ":" + contact.getEmail());
      }
    }
  }

  private void handleZohoContactUpdate(Contact contact, KeycloakUser keycloakUser) throws SDKException {
    boolean isPartOfTestGroup = isPartOfTestGroup(keycloakUser);
    //Skip the users who belong to test groups
    if (!isPartOfTestGroup) {
      Set<String> participationLevel = calculateParticipationLevel(keycloakUser.getAssociatedRoleList(),contact.getContactParticipation());
      //If Secondary mail is present then consider the private/project keys of that user
      updateParticipationBasedOnsecondaryMail(contact.getSecondaryEmail(), participationLevel);
      if (isSyncEnabled() && isToUpdateContact(contact, keycloakUser,participationLevel)
        && updateZohoContact(Long.parseLong(contact.getId()), keycloakUser.getId(), participationLevel)
      ) {
        updatedContacts.add(contact.getId() + ":" + contact.getEmail());
       }
    }
  }

  private boolean isPartOfTestGroup(KeycloakUser keycloakUser) {
    return testUserIds.contains(keycloakUser.getId());
  }

  /**
   * Control if actual updates in ZOHO to be made by the sync job.   *
   * @return boolean
   */
    public boolean isSyncEnabled() {
    String enableKeycloakToZohoSync = System.getenv("ENABLE_KEYCLOAK_TO_ZOHO_SYNC");
    return StringUtils.isNotEmpty(enableKeycloakToZohoSync) && "true".equals(enableKeycloakToZohoSync);
  }

  private static Set<String> removeAPIRelatedParticipation(String contactParticipation) {
    if(StringUtils.isEmpty(contactParticipation)) {
         return Collections.emptySet();
    }
    return Arrays.stream(contactParticipation.split(";")).filter(participation ->
            !(API_CUSTOMER.equals(participation) || API_USER.equals(participation) || ACCOUNT_HOLDER.equals(participation)))
        .collect(Collectors.toCollection(HashSet::new));
  }

  private boolean isToUpdateContact(Contact zohoContact, KeycloakUser keycloakUser,
      Set<String> participationLevel) {
    //check if  user Account id is changed
    if(!keycloakUser.getId().equals(zohoContact.getUserAccountId())) {
      return true;
    }
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

  private static boolean isContactNameChanged(Contact zohoContact, KeycloakUser keycloakUser) {
    if(StringUtils.isNotEmpty(keycloakUser.getFirstName()) &&
        !keycloakUser.getFirstName().equals(zohoContact.getFirstName())){
      return true;
    }
    return StringUtils.isNotEmpty(keycloakUser.getLastName()) &&
        !keycloakUser.getLastName().equals(zohoContact.getLastName());
  }

  private void updateParticipationBasedOnsecondaryMail(String secondaryMail, Set<String> participationLevel) {
    if(StringUtils.isNotEmpty(secondaryMail)) {
       KeycloakUser userForSecondaryMail = userdetails.get(secondaryMail);
      if (userForSecondaryMail != null) {
        participationLevel.addAll(calculateParticipationLevel(userForSecondaryMail.getAssociatedRoleList(),null));
      }
    }
  }

  /**
   * Validates keycloak user and creates the associated contact in zoho
   * @param zohoContacts list of contacts in zoho
   * @return count of newly created contacts in zoho
   */
  public int validateAndCreateZohoContact(List<Contact> zohoContacts) {
    int count = 0;
    List<String> newContacts= new ArrayList<>();
    try {
      Map<String, Contact> zohoContactsByPrimaryMail = getContactsMap(zohoContacts);
      //Iterate over keyCloak Users
      for (Map.Entry<String,KeycloakUser> userEntry : userdetails.entrySet()) {
        KeycloakUser user = userEntry.getValue();
        if (!zohoContactsByPrimaryMail.containsKey(user.getEmail()) && !testUserIds.contains(user.getId())) {
          //The keycloak user is not part of zoho yet , create new contact in zoho
          String firstName = user.getFirstName();
          String lastName = populateLastNameForContact(user, firstName);
          Set<String> participationLevel = calculateParticipationLevel(user.getAssociatedRoleList(),null);
          if(isSyncEnabled()){
            LOG.info("Creating zoho contact " + user.getEmail());
           if(createZohoContact(user.getId(),user.getEmail(), firstName, lastName,participationLevel)){
             newContacts.add(user.getEmail());
             count++;
           }else {
             LOG.error("Zoho Contact creation failed for "+user.getEmail());
           }
         }
        }
      }
    }catch (SDKException e){
      LOG.error("Error occurred while creating contact : "+ e.getMessage());
    }
    LOG.info("New contacts :" + newContacts);
    return count;
  }

  private static String populateLastNameForContact(KeycloakUser user, String firstName) {
    String lastName = user.getLastName();
    if (StringUtils.isEmpty(firstName) && StringUtils.isEmpty(lastName)) {
      lastName = user.getUsername();
    }
    if (StringUtils.isNotEmpty(firstName) && StringUtils.isEmpty(lastName)) {
      lastName="-";
    }
    return lastName;
  }

  private static Map<String, Contact> getContactsMap(List<Contact> zohoContacts) {
    Map<String, Contact> zohoContactsByPrimaryMail = new HashMap<>();
    for (Contact c : zohoContacts) {
      if (StringUtils.isNotEmpty(c.getEmail())) {
        zohoContactsByPrimaryMail.put(c.getEmail(), c);
      }
    }
    return zohoContactsByPrimaryMail;
  }

  private Set<String> calculateParticipationLevel(List<String> userRoles,String existingParticipation) {
    Set<String> participationLevels = new HashSet<>();

    if(StringUtils.isNotEmpty(existingParticipation)) {
      List<String> existingParticipationLevelList = Arrays.stream(existingParticipation.split(";")).toList();
      participationLevels.addAll(existingParticipationLevelList);
    }

    participationLevels.add(ACCOUNT_HOLDER);
    if(userRoles.contains(SHARED_OWNER)){
      participationLevels.add(API_CUSTOMER);
    }
    if(userRoles.contains(CLIENT_OWNER)){
      participationLevels.add(API_USER);
    }

    return participationLevels;
  }

  public void updateAPIProjects(List<APIProject> apiProjects) {
    try {
      clientdetails = repo.getAllClients(realm.getName());
      Map<Long,KeycloakClient> apiProjectsToUpdate = new HashMap<>();
      for (APIProject project : apiProjects) {
        String projectKey = project.getKey();
        //fetch the lastAccess Date attribute from client
        KeycloakClient client = clientdetails.get(projectKey);
        if (client !=null &&
            StringUtils.isNotEmpty(client.getLastAccessDate())
         && isDateChanged(project.getLastAccess(), client.getLastAccessDate())) {
          // if the last access date value is changed , then consider it for updating in zoho
          LOG.info("Last Access date for client : " + client.getLastAccessDate());
          apiProjectsToUpdate.put(Long.parseLong(project.getId()),client);
        }
      }
      if(isSyncEnabled()) {
        updater.updateInBatches(getListOfRecordsToUpdate(apiProjectsToUpdate),"API_projects");
      }
    } catch (SDKException e) {
      LOG.error("Error occurred while updating API_Project: " + e.getMessage());
    }
  }

  private static boolean isDateChanged(String date1, String date2) {
    try {
      return !OffsetDateTime.parse(date2).isEqual(OffsetDateTime.parse(date1));
    } catch (DateTimeParseException e) {
      LOG.error("Unable to parse input date to OffsetDateTime - " + date1 + " and " + date2);
      return false;
    }
  }

  private List<Record> getListOfRecordsToUpdate(Map<Long, KeycloakClient> apiProjectsToUpdate) {
    List<Record> records = new ArrayList<>();
    for(Map.Entry<Long,KeycloakClient> entry : apiProjectsToUpdate.entrySet()){
      //prepare record to update and add it in list
      Record recordToUpdate = new Record();
      recordToUpdate.setId(entry.getKey());
      recordToUpdate.addKeyValue("Last_access", OffsetDateTime.parse(entry.getValue().getLastAccessDate()));
      records.add(recordToUpdate);
    }
    return records;
  }
}