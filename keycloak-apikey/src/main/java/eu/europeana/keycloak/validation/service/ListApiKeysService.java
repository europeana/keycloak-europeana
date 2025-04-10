package eu.europeana.keycloak.validation.service;

import eu.europeana.keycloak.validation.datamodel.Apikey;
import eu.europeana.keycloak.validation.util.Constants;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

public class ListApiKeysService {
  private static final Logger LOG  = Logger.getLogger(ListApiKeysService.class);

  public List<Apikey> getPrivateAndProjectkeys(UserModel userModel) {
    List<RoleModel> rolesAssociatedToUser = userModel.getRoleMappingsStream().filter(
        roleModel -> (Constants.CLIENT_OWNER.equals(roleModel.getName()) || Constants.SHARED_OWNER.equals(
            roleModel.getName()))).toList();
    return gatherSortedApiKeyInfo(rolesAssociatedToUser);
  }

  /** Method fetches personal and project keys and then sorts them based on creation date.   *
   * The resultant list has 2 parts-> first is sorted personal keys and then sorted project keys.
   * If the creation date is not provided the such keys are appended at the end of respective list.
   * @param roleModelList roles Associated To User
   * @return Api key List
   */
  private static List<Apikey> gatherSortedApiKeyInfo(List<RoleModel> roleModelList) {
    List<Apikey> personalKeys = new ArrayList<>();
    List<Apikey> projectKeys = new ArrayList<>();

    gatherPersonalAndProjectKeys(roleModelList, personalKeys, projectKeys);

    personalKeys.sort(Comparator.comparing(Apikey::getCreated,Comparator.nullsLast(Comparator.reverseOrder())).reversed());
    projectKeys.sort(Comparator.comparing(Apikey::getCreated,Comparator.nullsLast(Comparator.reverseOrder())).reversed());

    personalKeys.addAll(projectKeys);

    return personalKeys;
  }

  private static void gatherPersonalAndProjectKeys(List<RoleModel> rolesAssociatedToUser, List<Apikey> personalKeys,
      List<Apikey> projectKeys) {
    for (RoleModel rolemodel : rolesAssociatedToUser) {
      if (rolemodel.isClientRole()) {
        Apikey apikey = getApikey(rolemodel);
        if(apikey !=null) {
          if(Constants.PERSONAL_KEY.equals(apikey.getType())){
            personalKeys.add(apikey);
          }
          if(Constants.PROJECT_KEY.equals(apikey.getType())){
            projectKeys.add(apikey);
          }
        }
      }
    }
  }

  /**Based on the role associated to use , get the client(i.e. apikey) and
   * generate the Apikey object for only personal & project keys associated to user.
   * The creation date of key is fetched based on attribute associated to user
   * @param rolemodel role associated to user
   * @return Apikey objec or else null
   */
  private static Apikey getApikey(RoleModel rolemodel) {
    ClientModel client = (ClientModel) rolemodel.getContainer();
    Date creationDate = getRoleCreationDate(rolemodel.getFirstAttribute(Constants.ROLE_ATTRIBUTE_CREATION_DATE));
    String clientState = client.isEnabled()?null: Constants.CLIENT_STATE_DISABLED;
    if (Constants.CLIENT_OWNER.equals(rolemodel.getName())) {
      return new Apikey(client.getId(), client.getClientId(), Constants.PERSONAL_KEY, creationDate, null,
          null, clientState);
    }
    if (Constants.SHARED_OWNER.equals(rolemodel.getName())) {
      return new Apikey(client.getId(), client.getClientId(), Constants.PROJECT_KEY, creationDate,
          client.getName(),
          client.getDescription(), clientState);
    }
    return null;
  }

  private static Date getRoleCreationDate(String creationDate) {
    if(StringUtils.isEmpty(creationDate)) return null;
    try {
      ZonedDateTime zonedDateTime = ZonedDateTime.parse(creationDate, DateTimeFormatter.ISO_INSTANT.withZone(
          ZoneOffset.UTC));
      return Date.from(zonedDateTime.toInstant());
    }
    catch (DateTimeParseException ex) {
      LOG.error("Exception occurred while parsing date : "+creationDate+" returning null date");
      return null;
    }
  }
}
