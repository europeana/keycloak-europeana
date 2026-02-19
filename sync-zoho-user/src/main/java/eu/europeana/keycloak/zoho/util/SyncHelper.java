package eu.europeana.keycloak.zoho.util;

import com.zoho.crm.api.record.Field;
import com.zoho.crm.api.record.Record;
import com.zoho.crm.api.util.Choice;
import eu.europeana.keycloak.zoho.datamodel.Contact;
import eu.europeana.keycloak.zoho.datamodel.KeycloakUser;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.*;

/**
 * Mapping methods for generating the request elements for zoho calls
 */
public class SyncHelper {

    private static final Logger LOG = Logger.getLogger(SyncHelper.class);
    public static final String CONTACT_PARTICIPATION = "Contact_Participation";
    public static final String USER_ACCOUNT_ID = "User_Account_ID";
    public static final String LAST_ACCESS = "Last_access";
    public static final String RATE_LIMIT_REACHED = "Rate_limit_reached";
    public static final String LEAD_SOURCE = "Lead_Source";
    public static final String EMAIL = "Email";
    public static final String PERSONAL_KEY = "Personal_key";
    public static final String USERNAME = "Username";
    public static final String EUROPEANA_ACCOUNT_SIGN_UP_FORM = "Europeana account sign-up form";
    public static final String SEMI_COLON = ";";

    /**
     * Converts string into String List.
     * @param input string with delimiters
     * @param separator e.g. ',' ,';'
     * @return  list of strings  or empty list
     */
    public List<String> tokenizeString(String input, String separator) {
        final List<String> idList;
        idList = StringUtils.isNotEmpty(input)?
                Arrays.stream(input.split(separator)).map(String::trim).filter(StringUtils::isNotEmpty).toList()
                :List.of();
        return idList;
    }

    /**
     * Create request for update with only changed values.
     *
     * @param zohoContact
     * @param keycloakUser
     * @param userDetails
     * @return updated record object
     */
    public Record getRecordForUpdate(Contact zohoContact, KeycloakUser keycloakUser, Map<String, KeycloakUser> userDetails) {

        Record recordToUpdate  = new Record();
        //check if  user Account id is changed
        if (!keycloakUser.getId().equals(zohoContact.getUserAccountId())) {
            recordToUpdate.addKeyValue(USER_ACCOUNT_ID,keycloakUser.getId());
        }
        //Check if LastAccessDate is Changed in keycloak
        if (isDateChangedInSource(zohoContact.getLastAccess(), keycloakUser.getLastAccess())) {
            recordToUpdate.addKeyValue(LAST_ACCESS, OffsetDateTime.parse(keycloakUser.getLastAccess()));
        }
        //Check if rateLimitReachedDate is changed in keycloak
        if (isDateChangedInSource(zohoContact.getRateLimitReached(), keycloakUser.getRateLimitReached())) {
            recordToUpdate.addKeyValue(RATE_LIMIT_REACHED, OffsetDateTime.parse(keycloakUser.getRateLimitReached()));
        }
        //Check if participation level changed
        Set<String> participationLevel = calculateParticipationLevel(zohoContact, keycloakUser,userDetails);
        if(isParticipationLevelChanged(zohoContact, participationLevel)){
            recordToUpdate.addKeyValue(CONTACT_PARTICIPATION,getParticipationChoice(participationLevel));
        }
        //check if personalKey is not aligned in keycloak and zoho
        if(!StringUtils.equals(keycloakUser.getPersonalKey(), zohoContact.getPersonalKey())){
            recordToUpdate.addKeyValue(PERSONAL_KEY,keycloakUser.getPersonalKey());
        }
        //check if Username to be updated
        if(!StringUtils.equals(keycloakUser.getUsername(),zohoContact.getUsername())){
            recordToUpdate.addKeyValue(USERNAME,keycloakUser.getUsername());
        }
        return recordToUpdate;
    }

    /**
     * Gets participation level for zho Contact based on roles of respective keycloak users.
     *
     * <p>Both primary and secondary emails of a zoho contact can have keycloak Users associated to them.</p>
     * <p>Method considers  roles of both of the users for calculation.</p>
     */
    private Set<String> calculateParticipationLevel(Contact contact, KeycloakUser keycloakUser, Map<String, KeycloakUser> userDetails) {
        Set<String> participationLevel = getParticipationLevel(keycloakUser.getAssociatedRoleList(), contact.getContactParticipation());

        //If Secondary mail is present then consider the private/project keys of that user as well
        String secondaryEmail = contact.getSecondaryEmail();
        if (StringUtils.isNotEmpty(secondaryEmail)) {
            KeycloakUser userForSecondaryMail = userDetails.get(secondaryEmail.toLowerCase(Locale.ENGLISH));
            if (userForSecondaryMail != null) {
                participationLevel.addAll(getParticipationLevel(userForSecondaryMail.getAssociatedRoleList(), null));
            }
        }
        return participationLevel;
    }

    /**
     * Create New record for creating  contact in zoho.
     * @param user - keycloak user
     * @return newRecord object
     */

    public Record createContactRequest(KeycloakUser user) {

        Set<String> participationLevel = getParticipationLevel(user.getAssociatedRoleList(), null);
        Record newRecord = new Record();
        newRecord.addFieldValue(Field.Contacts.FIRST_NAME, user.getFirstName());
        newRecord.addFieldValue(Field.Contacts.LAST_NAME, getLastNameForContact(user));
        newRecord.addKeyValue(EMAIL, user.getEmail());
        newRecord.addKeyValue(PERSONAL_KEY, user.getPersonalKey());
        newRecord.addKeyValue(USERNAME, user.getUsername());

        List<Choice<String>> participationLevelList = getParticipationChoice(participationLevel);

        newRecord.addKeyValue(CONTACT_PARTICIPATION,new ArrayList<>(participationLevelList));
        newRecord.addKeyValue(LEAD_SOURCE,new Choice<>(EUROPEANA_ACCOUNT_SIGN_UP_FORM));
        newRecord.addKeyValue(USER_ACCOUNT_ID, user.getId());
        return newRecord;
    }

    /**
     * Generates the request element for dissociating contact.
     * @param contact representing zoho contact
     * @return Record object
     */
    public Record requestToDissociateContact(Contact contact) {
        Record recordToUpdate = new Record();
        recordToUpdate.addKeyValue(USER_ACCOUNT_ID,null);
        recordToUpdate.addKeyValue(CONTACT_PARTICIPATION,getParticipationChoice(
                removeAPIRelatedParticipation(contact.getContactParticipation())));
        return recordToUpdate;
    }

    /**
     * Check if source and target dates are different  and if target date need to be updated.
     * @param target  date to compare from target system
     * @param source  date to compare from source
     * @return boolean
     */
    public boolean isDateChangedInSource(String target, String source) {
        if (StringUtils.isEmpty(source) || !isValidDate(source)) {
            //sourceDate is not present (earlier date) or is invalid then not required to update
            return false;
        }
        if (StringUtils.isEmpty(target) || !isValidDate(target)) {
            //targetDate is not present or have some invalid value , need to update with sourceDate
            return true;
        }
        //If source date and target dates are different and need to be synced.
        return !OffsetDateTime.parse(source).isEqual(OffsetDateTime.parse(target));
    }

    /**
     * Checks if string represents valid date-time and is parsed using
     * {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     * e.g. of valid date- 2007-12-03T10:15:30+01:00
     * @param date String
     * @return boolean true if valid date else false.
     */
    public  boolean isValidDate(String date) {
        try {
            OffsetDateTime.parse(date);
            return true;
        } catch (DateTimeParseException e) {
            LOG.error("Invalid Date : " + date);
            return false;
        }
    }


    /**
     * Fetch all participation levels of a user based on roles along with existing ones.
     *
     * <p>If the keycloak user has respective 'contact' in zoho , then  it is an
     * 'Account holder'.</p>
     * <p>If user has Project key associated , then it is also an 'API Customer'.</p>
     * <p>If user has Private key , then it is also a 'API User'.</p>
     *
     * @param userRoles keycloak roles of user
     * @param existingParticipation  existing participation values( ';' separated)  if any
     * @return  Set of Strings containing all participation levels.
     */
    public Set<String> getParticipationLevel(List<String> userRoles, String existingParticipation) {
        Set<String> participationLevels = new HashSet<>();
        if(StringUtils.isNotEmpty(existingParticipation)) {
            List<String> existingParticipationLevelList = Arrays.stream(existingParticipation.split(SEMI_COLON)).toList();
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

    private  Set<String> removeAPIRelatedParticipation(String contactParticipation) {
        if(StringUtils.isEmpty(contactParticipation)) {
            return Collections.emptySet();
        }
        return Arrays.stream(contactParticipation.split(SEMI_COLON)).filter(participation ->
                        !(API_CUSTOMER.equals(participation) || API_USER.equals(participation) || ACCOUNT_HOLDER.equals(participation)))
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     *  Compare the existing participation levels against the calculated ones.
     *  If any of the calculated participation level is not  part of existing levels in zoho ,
     *  then consider the participation levels needs to be updated in zoho.
     */
    private boolean isParticipationLevelChanged(Contact zohoContact, Set<String> calculatedParticipationLevels) {
        String zohoParticipation = zohoContact.getContactParticipation();
        List<String> zohoParticipationLevels = StringUtils.isNotEmpty(zohoParticipation) ?
                Arrays.stream(zohoParticipation.split(SEMI_COLON)).toList() : Collections.emptyList();
        return !zohoParticipationLevels.containsAll(calculatedParticipationLevels);
    }

    private List<Choice<String>> getParticipationChoice(Set<String> participationLevel) {
        List<Choice<String>> participationLevelList = new ArrayList<>();
        if(participationLevel !=null)
            participationLevel.forEach(p-> participationLevelList.add(new Choice<>(p)));
        return participationLevelList;
    }

    /**
     * Get the last Name from the Keycloak user.
     * If user does not have first or last name  , use the username as the last name.
     * populate '-' as last name for users who have only the first name
     * @param user  KeycloakUser
     * @return lastName for the user
     */
    public  String getLastNameForContact(KeycloakUser user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (StringUtils.isEmpty(firstName) && StringUtils.isEmpty(lastName)) {
            lastName = user.getUsername();
        }
        if (StringUtils.isNotEmpty(firstName) && StringUtils.isEmpty(lastName)) {
            lastName = "-";
        }
        return lastName;
    }
}