package eu.europeana.keycloak.zoho.util;

import eu.europeana.keycloak.zoho.datamodel.Contact;
import eu.europeana.keycloak.zoho.datamodel.KeycloakUser;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static eu.europeana.keycloak.zoho.repo.KeycloakZohoVocabulary.*;

public class SyncHelper {

    private static final Logger LOG = Logger.getLogger(SyncHelper.class);

    public List<String> tokenizeString(String ids, String separator) {
        final List<String> idList;
        idList = StringUtils.isNotEmpty(ids)?
                Arrays.stream(ids.split(separator)).map(String::trim).filter(StringUtils::isNotEmpty).toList()
                :List.of();
        return idList;
    }

    public boolean isDateChanged(String target, String source) {
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
     *
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

    public  Set<String> removeAPIRelatedParticipation(String contactParticipation) {
        if(StringUtils.isEmpty(contactParticipation)) {
            return Collections.emptySet();
        }
        return Arrays.stream(contactParticipation.split(";")).filter(participation ->
                        !(API_CUSTOMER.equals(participation) || API_USER.equals(participation) || ACCOUNT_HOLDER.equals(participation)))
                .collect(Collectors.toCollection(HashSet::new));
    }

    public  boolean isParticipationLevelChanged(Contact zohoContact, Set<String> participationLevel) {
        if(StringUtils.isNotEmpty(zohoContact.getContactParticipation())) {
            String[] levels = zohoContact.getContactParticipation().split(";");
            return levels.length > 0 && Arrays.stream(levels)
                    .anyMatch(p -> !participationLevel.contains(p));
        }
        return false;
    }

    public static boolean isLastNameChanged(Contact zohoContact, KeycloakUser keycloakUser) {
        return StringUtils.isNotEmpty(keycloakUser.getLastName()) &&
                !keycloakUser.getLastName().equals(zohoContact.getLastName());
    }

    public static boolean isFirstNameChanged(Contact zohoContact, KeycloakUser keycloakUser) {
        return StringUtils.isNotEmpty(keycloakUser.getFirstName()) &&
                !keycloakUser.getFirstName().equals(zohoContact.getFirstName());
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