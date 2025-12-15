package eu.europeana.keycloak.zoho.sync;

import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.Field;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.util.Choice;
import eu.europeana.keycloak.zoho.datamodel.Account;
import eu.europeana.keycloak.zoho.datamodel.Contact;
import eu.europeana.keycloak.zoho.datamodel.Institute;
import eu.europeana.keycloak.zoho.datamodel.KeycloakUser;
import eu.europeana.keycloak.zoho.repo.CustomQueryRepository;
import eu.europeana.keycloak.zoho.util.SyncHelper;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import java.time.OffsetDateTime;
import java.util.*;

import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.ENABLE_CONTACT_SYNC;
import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.TEST_CONTACT_IDS_TO_UPDATE;

/**
 * Service to check and sync specific fields for the zoho contact or create new contact in zoho
 */

public class ZohoContactSyncHandler extends AbstractSyncHandler {
    private static final Logger LOG = Logger.getLogger(ZohoContactSyncHandler.class);
    public static final String CONTACTS_SYNC_MSG = "%s accounts in Zoho where compared against %s accounts in KeyCloak where:  %s accounts are shared and %s contacts were added to Zoho. The affiliation for %s accounts was changed or established.";
    private final UserProvider userProvider;
    private final SyncHelper helper;
    private final ZohoUpdater updater;
    private final List<String> preconfiguredContactIdsToSync;
    private final List<String> updatedContacts = new ArrayList<>();
    HashMap<String, Institute> instituteMap = new HashMap<>();
    HashMap<String, String> modifiedUserMap = new HashMap<>();

    /**
     * Initialize KeycloakToZohoSyncService
     *
     * @param realm keycloak realm name
     * @param repo  Query Repository
     */
    public ZohoContactSyncHandler(RealmModel realm, CustomQueryRepository repo, UserProvider userProvider) {
        super(realm, repo);
        this.userProvider = userProvider;
        this.updater = new ZohoUpdater();
        this.helper = new SyncHelper();
        preconfiguredContactIdsToSync = helper.tokenizeString(System.getenv(TEST_CONTACT_IDS_TO_UPDATE), ",");
    }

    public String syncContacts(int days, List<Account> institutions, List<Contact> contacts) {
        try {
            int nrUpdatedUsers = 0;
            int nrOfNewlyAddedContactsInZoho = 0;
            if (institutions != null && !institutions.isEmpty() && contacts != null && !contacts.isEmpty()) {
                updateContactsInZoho(days, institutions, contacts);
                nrOfNewlyAddedContactsInZoho = validateAndCreateZohoContact(contacts);
                nrUpdatedUsers = updateUsersInKeyCloak();
            }
            return generateStatusReportForContactSync(contacts.size(),
                    nrUpdatedUsers, nrOfNewlyAddedContactsInZoho);
        } catch (Exception e) {
            LOG.info("Message: " + e.getMessage() + "; cause: " + e);
            return "Contacts from last " + days + " days are not synchronized successfully ! ";
        }
    }

    private int updateUsersInKeyCloak() {
        int updated = 0;
        LOG.info("Checking if updated contacts exist in Keycloak ...");
        for (Map.Entry<String, String> affiliatedUser : modifiedUserMap.entrySet()) {
            UserModel user = userProvider.getUserByEmail(realm, affiliatedUser.getKey());
            if (user != null) {
                String zohoOrgId = affiliatedUser.getValue();
                //In case zoho orgID does not match with existing affiliation in keycloak then update the keycloak affiliation
                String affiliationValue = user.getFirstAttribute("affiliation");
                boolean isToUpdateAffiliation = (StringUtils.isNotBlank(zohoOrgId) ? !zohoOrgId.equals(
                        affiliationValue) : StringUtils.isNotBlank(affiliationValue));
                if (isToUpdateAffiliation) {
                    user.setSingleAttribute("affiliation", zohoOrgId);
                    updated++;
                    LOG.info(affiliatedUser.getKey() + " affiliation updated from : " + affiliationValue + " to " +
                            zohoOrgId + " in keycloak");
                } else {
                    LOG.info(affiliatedUser.getKey() + " affiliation will not be updated in keycloak");
                }
            }
        }
        LOG.info(updated + " users were found in Keycloak and had their affiliation updated.");
        return updated;
    }

    private void updateContactsInZoho(int days, List<Account> institutions, List<Contact> contacts) {
        //Create the map of Institutes
        for (Account zohoInstitute : institutions) {
            instituteMap.put(zohoInstitute.getId(), new Institute(zohoInstitute.getAccountName(), zohoInstitute.getEuropeanaOrgID()));
        }
        loadKeycloakUsersFromDB();
        for (Contact zohoContactObj : contacts) {
            //Fetch the contacts which have been updated in zoho but does not reflect details in keycloak
            calculateUsersModifiedInzoho(zohoContactObj, days);
            //handle contacts which have been updated in Keycloak but does not reflect details in zoho.
            handleZohoUpdate(zohoContactObj);
        }
        LOG.info(modifiedUserMap.size() + " contacts records were updated in Zoho in the past " + days + " days.");
        LOG.info("Zoho Contacts Updated in this sync: " + updatedContacts);
    }

    /**
     * Generate map of user Email and the OrgID for contacts who were modified after given date.     *
     *
     * @param zohoContactObj
     * @param days
     */
    private void calculateUsersModifiedInzoho(Contact zohoContactObj, int days) {
        OffsetDateTime toThisTimeAgo = OffsetDateTime.now().minusDays(days);
        if (zohoContactObj.getModifiedTime().isAfter(toThisTimeAgo)) {
            String accountID = zohoContactObj.getAccountID();
            String email = zohoContactObj.getEmail();
            if (StringUtils.isNotBlank(accountID) && instituteMap.get(accountID) != null) {
                modifiedUserMap.put(email, instituteMap.get(accountID).getEuropeanaOrgID());
            } else if (StringUtils.isBlank(accountID)) {
                modifiedUserMap.put(email, null);
            }
        }
    }

    private String generateStatusReportForContactSync(int totalContacts, int nrUpdatedUsers, int nrOfNewlyAddedContactsInZoho) {
        return String.format(CONTACTS_SYNC_MSG,
                totalContacts,
                userProvider.getUsersCount(realm),
                modifiedUserMap.size(),
                nrOfNewlyAddedContactsInZoho,
                nrUpdatedUsers);
    }

    private void handleZohoUpdate(Contact contact) {
        try {
            KeycloakUser keycloakUser = userDetails.get(contact.getEmail().toLowerCase());
            if (keycloakUser == null) {
                // if zoho contact not part of keycloak , then disassociate it in zoho
                handleUserDissociation(contact);
            } else {
                //update contact association in zoho if required
                handleZohoContactUpdate(contact, keycloakUser);
            }
        } catch (SDKException e) {
            LOG.error("Exception occurred while updating  contact. " + e);
        }
    }


    /**
     * Removes any details which link the zohoContact to keycloak user.
     * i.e. change the user_account_id  to null and remove API related participation levels,
     * If the sync flag is ON or the contact is preconfigured for force sync.
     *
     * @param contact represents zoho contact
     * @throws SDKException
     */
    private void handleUserDissociation(Contact contact) throws SDKException {

        if ((ENABLE_CONTACT_SYNC || isPreconfiguredForForceUpdate(contact))
                && StringUtils.isNotEmpty(contact.getUserAccountId())) {

            Record recordToUpdate = requestToDissociateContact(contact);
            if (updater.callZohoUpdate(Long.parseLong(contact.getId()),recordToUpdate)) {
                updatedContacts.add(contact.getId() + ":" + contact.getEmail());
            }
        }
    }

    private Record requestToDissociateContact(Contact contact) {
        Record recordToUpdate = new Record();
        recordToUpdate.addKeyValue("User_Account_ID",null);
        recordToUpdate.addKeyValue("Contact_Participation",getParticipationChoice(
                helper.removeAPIRelatedParticipation(contact.getContactParticipation())));
        return recordToUpdate;
    }

    /**
     * Checks if any relevant contact detail is to be updated in zoho and call zoho if required.
     * <p>Update is skipped for users belonging to test group or if the global sync flag  is OFF.</p>
     * <p>Update is always performed for the zohoContact ids (preconfigured separately) even if global sync switch is OFF.</p>
     */
    private void handleZohoContactUpdate(Contact contact, KeycloakUser keycloakUser) throws SDKException {
        if (!isPartOfTestGroup(keycloakUser)) {
            Record recordToUpdate = getRecordForUpdate(contact, keycloakUser);
            if (!recordToUpdate.getKeyValues().isEmpty()) {
                if (ENABLE_CONTACT_SYNC || isPreconfiguredForForceUpdate(contact)) {
                    if (updater.callZohoUpdate(Long.parseLong(contact.getId()),recordToUpdate)) {
                        //List of contacts which are now updated in zoho.
                        updatedContacts.add(contact.getId() + ":" + contact.getEmail());
                    }
                }
            }
        }
    }

    /**
     * Gets participation level for zho Contact based on roles of respective keycloak users.
     *
     * <p>Both primary and secondary emails of a zoho contact can have keycloak Users associated to them.</p>
     * <p>Method considers  roles of both of the users for calculation.</p>
     */
    private Set<String> calculateParticipationLevel(Contact contact, KeycloakUser keycloakUser) {
        Set<String> participationLevel = helper.getParticipationLevel(keycloakUser.getAssociatedRoleList(), contact.getContactParticipation());

        //If Secondary mail is present then consider the private/project keys of that user as well
        String secondaryEmail = contact.getSecondaryEmail();
        if (StringUtils.isNotEmpty(secondaryEmail)) {
            KeycloakUser userForSecondaryMail = userDetails.get(secondaryEmail.toLowerCase());
            if (userForSecondaryMail != null) {
                participationLevel.addAll(helper.getParticipationLevel(userForSecondaryMail.getAssociatedRoleList(), null));
            }
        }
        return participationLevel;
    }

    private boolean isPreconfiguredForForceUpdate(Contact contact) {
        if (preconfiguredContactIdsToSync.contains(contact.getId())) {
            LOG.info("Contact " + contact.getEmail() + " - " + contact.getId() + " will be updated in zoho !");
            return true;
        }
        return false;
    }

    /**
     * Create request for update with only changed values.
     * @param zohoContact
     * @param keycloakUser
     * @return updated record object
     */
    private Record getRecordForUpdate(Contact zohoContact, KeycloakUser keycloakUser) {

        Record recordToUpdate  = new Record();
        //check if  user Account id is changed
        if (!keycloakUser.getId().equals(zohoContact.getUserAccountId())) {
            recordToUpdate.addKeyValue("User_Account_ID",keycloakUser.getId());
        }
        //Check if LastAccessDate is Changed in keycloak
        if (helper.isDateChanged(zohoContact.getLastAccess(), keycloakUser.getLastAccess())) {
            recordToUpdate.addKeyValue("Last_access", OffsetDateTime.parse(keycloakUser.getLastAccess()));
        }
        //Check if rateLimitReachedDate is changed in keycloak
        if (helper.isDateChanged(zohoContact.getRateLimitReached(), keycloakUser.getRateLimitReached())) {
            recordToUpdate.addKeyValue("Rate_limit_reached", OffsetDateTime.parse(keycloakUser.getLastAccess()));
        }
        //Check if participation level changed
        Set<String> participationLevel = calculateParticipationLevel(zohoContact, keycloakUser);
        if(helper.isParticipationLevelChanged(zohoContact, participationLevel)){
           recordToUpdate.addKeyValue("Contact_Participation",getParticipationChoice(participationLevel));
        }

       return recordToUpdate;
    }


    /**
     * Validates keycloak user and creates the associated contact in zoho
     *
     * @param zohoContacts list of contacts in zoho
     * @return count of newly created contacts in zoho
     */
    private int validateAndCreateZohoContact(List<Contact> zohoContacts) {
        List<String> newContacts = new ArrayList<>();
        try {
            Map<String, Contact> zohoContactsByPrimaryMail = getEmailToContactMap(zohoContacts);
            //Iterate over keyCloak Users
            for (Map.Entry<String, KeycloakUser> userEntry : userDetails.entrySet()) {
                KeycloakUser user = userEntry.getValue();
                if (!zohoContactsByPrimaryMail.containsKey(user.getEmail().toLowerCase())
                        && !testUserIds.contains(user.getId())) {
                    //The keycloak user is not part of zoho yet , create new contact in zoho
                    createContactInZoho(user, newContacts);
                }
            }
        } catch (SDKException e) {
            LOG.error("Error occurred while creating contact : " + e.getMessage());
        }
        LOG.info("New contacts :" + newContacts);
        return newContacts.size();
    }

    private void createContactInZoho(KeycloakUser user, List<String> newContacts) throws SDKException {
        String lastName = helper.getLastNameForContact(user);
        Set<String> participationLevel = helper.getParticipationLevel(user.getAssociatedRoleList(), null);
        if (ENABLE_CONTACT_SYNC) {
            LOG.info("Creating zoho contact " + user.getEmail());
            if (updater.createNewZohoContact(
                    newRecordRequest(user.getId(), user.getEmail(), user.getFirstName(), lastName, participationLevel))) {
                newContacts.add(user.getEmail());
            } else {
                LOG.error("Zoho Contact creation failed for " + user.getEmail());
            }
        }
    }

    /**
     * Create New record Request
     * @param userAccountID - keycloak public id of user
     * @param email - email for the new contact
     * @param firstName - firstName of user
     * @param lastName - Name for the contact.This can be keycloak username if user does not have first and last name populated in keycloak
     * @param participationLevel - contacts participation level
     * @return newRecord object
     */

    private Record newRecordRequest(String userAccountID, String email, String firstName, String lastName, Set<String> participationLevel) {
        Record newRecord = new Record();
        newRecord.addFieldValue(Field.Contacts.FIRST_NAME, firstName);
        newRecord.addFieldValue(Field.Contacts.LAST_NAME, lastName);
        newRecord.addKeyValue("Email", email);

        List<Choice<String>> participationLevelList = getParticipationChoice(participationLevel);

        newRecord.addKeyValue("Contact_Participation",new ArrayList<>(participationLevelList));
        newRecord.addKeyValue("Lead_Source",new Choice<>("Europeana account sign-up form"));
        newRecord.addKeyValue("User_Account_ID", userAccountID);
        return newRecord;
    }


    private List<Choice<String>> getParticipationChoice(Set<String> participationLevel) {
        List<Choice<String>> participationLevelList = new ArrayList<>();
        if(participationLevel !=null)
            participationLevel.forEach(p-> participationLevelList.add(new Choice<>(p)));
        return participationLevelList;
    }

    private  Map<String, Contact> getEmailToContactMap(List<Contact> zohoContacts) {
        Map<String, Contact> zohoContactsByPrimaryMail = new HashMap<>();
        for (Contact c : zohoContacts) {
            if (StringUtils.isNotEmpty(c.getEmail())) {
                zohoContactsByPrimaryMail.put(c.getEmail().toLowerCase(), c);
            }
        }
        return zohoContactsByPrimaryMail;
    }
}