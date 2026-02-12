package eu.europeana.keycloak.zoho.sync;

import com.zoho.crm.api.exception.SDKException;
import com.zoho.crm.api.record.Record;
import eu.europeana.keycloak.zoho.datamodel.Account;
import eu.europeana.keycloak.zoho.datamodel.Contact;
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

import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.*;

/**
 * Service to check and sync specific fields for the zoho contact or create new contact in zoho
 */

public class ZohoContactSyncHandler extends AbstractSyncHandler {
    private static final Logger LOG = Logger.getLogger(ZohoContactSyncHandler.class);
    public static final String CONTACTS_SYNC_MSG = "%s Zoho contacts were compared against " +
            "%s Keycloak Users where " +
            "\\n%s Contacts were modified in zoho since last time." +
            "\\nThe affiliation for %s Keycloak Users is changed or established."+
            "\\n%s Contacts are added to Zoho. ";
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
     * @param repo  Query Repository for triggering custom queries
     * @param userProvider keycloak userProvider
     */
    public ZohoContactSyncHandler(RealmModel realm, CustomQueryRepository repo, UserProvider userProvider) {
        super(realm, repo);
        this.userProvider = userProvider;
        this.updater = new ZohoUpdater();
        this.helper = new SyncHelper();
        preconfiguredContactIdsToSync = helper.tokenizeString(System.getenv(TEST_CONTACT_IDS_TO_UPDATE), ",");
    }

    /**
     * Update or create contacts in zoho.
     * Find contacts modified in zoho in last 'n' days and update the keycloak db.
     *
     * @param days to get modified contacts from today to this many number of days ago.
     * @param institutions list of objects representing zoho Institutions
     * @param contacts List of objects representing zoho contacts
     * @return status
     */

    public String syncContacts(int days, List<Account> institutions, List<Contact> contacts) {
        try {
            int nrUpdatedUsers;
            int nrOfNewlyAddedContactsInZoho;
            if (institutions != null && !institutions.isEmpty() && contacts != null && !contacts.isEmpty()) {
                updateContactsInZoho(days, institutions, contacts);
                nrOfNewlyAddedContactsInZoho = validateAndCreateZohoContact(contacts);
                nrUpdatedUsers = updateUsersInKeyCloak();
                return generateStatusReportForContactSync(contacts.size(),
                        nrUpdatedUsers, nrOfNewlyAddedContactsInZoho);
            }
            LOG.error("List of Institutions and Contacts of zoho is Empty!");
        } catch (Exception e) {
            LOG.error("Error while synchronizing contacts from last" + days + " days.  " + e.getMessage() + "; cause: " + e);
        }
        return null;
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
            //Fetch the contacts which have been updated in zoho in last 'n' days but does not reflect details in keycloak
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
                nrUpdatedUsers,
                nrOfNewlyAddedContactsInZoho);
    }

    private void handleZohoUpdate(Contact contact) {
        try {
            KeycloakUser keycloakUser = userDetails.get(contact.getEmail().toLowerCase(Locale.ENGLISH));
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
        if (isEligibleBasedOnSyncScope(contact)
                && StringUtils.isNotEmpty(contact.getUserAccountId())) {

            Record recordToUpdate = helper.requestToDissociateContact(contact);
            if (updater.callZohoUpdate(Long.parseLong(contact.getId()),recordToUpdate)) {
                updatedContacts.add(contact.getId() + ":" + contact.getEmail());
            }
        }
    }

    /**
     * Checks if any relevant contact detail is to be updated in zoho and call zoho if required.
     * <p>Update is skipped for users belonging to test group or if the global sync flag is OFF.</p>
     * <p>Update is always performed for the 'preconfigured zohoContact ids' even if global sync switch is OFF.</p>
     */
    private void handleZohoContactUpdate(Contact contact, KeycloakUser keycloakUser) throws SDKException {
        if (isPartOfTestGroup(keycloakUser)) {
            return;
        }

        Record recordToUpdate = helper.getRecordForUpdate(contact, keycloakUser, userDetails);
        if (recordToUpdate.getKeyValues().isEmpty()) {
            return;
        }

        if (isEligibleBasedOnSyncScope(contact)) {
            if (updater.callZohoUpdate(Long.parseLong(contact.getId()), recordToUpdate)) {
                //List of contacts which are now updated in zoho.
                updatedContacts.add(contact.getId() + ":" + contact.getEmail());
            }
        }
    }

    public boolean isEligibleBasedOnSyncScope(Contact contact) {
        return SYNC_SCOPE == SyncScope.ALL ||
                (SYNC_SCOPE == SyncScope.TEST_ONLY && isPreconfiguredForUpdate(contact));
    }

    private boolean isPreconfiguredForUpdate(Contact contact) {
        if (preconfiguredContactIdsToSync.contains(contact.getId())) {
            LOG.info("Contact " + contact.getEmail() + " - " + contact.getId() + " will be updated in zoho !");
            return true;
        }
        return false;
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
                if (!zohoContactsByPrimaryMail.containsKey(user.getEmail().toLowerCase(Locale.ENGLISH))
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
        if (SYNC_SCOPE == SyncScope.ALL) {
            LOG.info("Creating zoho contact " + user.getEmail());
            Record contactRequest = helper.createContactRequest(user);
            if (updater.createNewZohoContact(contactRequest)) {
                newContacts.add(user.getEmail());
            } else {
                LOG.error("Zoho Contact creation failed for " + user.getEmail());
            }
        }
    }


    private  Map<String, Contact> getEmailToContactMap(List<Contact> zohoContacts) {
        Map<String, Contact> zohoContactsByPrimaryMail = new HashMap<>();
        for (Contact c : zohoContacts) {
            if (StringUtils.isNotEmpty(c.getEmail())) {
                zohoContactsByPrimaryMail.put(c.getEmail().toLowerCase(Locale.ENGLISH), c);
            }
        }
        return zohoContactsByPrimaryMail;
    }

    public class Institute {
        private String accountName;
        private String europeanaOrgID;

        public Institute(String aName, String eID) {
            accountName    = aName;
            europeanaOrgID = eID;
        }
        public String getAccountName() {
            return accountName;
        }
        public String getEuropeanaOrgID() {
            return europeanaOrgID;
        }
    }
}