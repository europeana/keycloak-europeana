package eu.europeana.keycloak.zoho.repo;

import eu.europeana.keycloak.zoho.datamodel.KeycloakClient;
import eu.europeana.keycloak.zoho.datamodel.KeycloakUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * Repository for executing specialized JPA and Native SQL queries against the Keycloak database.
 * @author Shweta Nazare
 */
public class CustomQueryRepository {
  private static final String FIND_USER_IDS_BY_GROUP_ID = "SELECT uge.user.id FROM  UserGroupMembershipEntity uge WHERE uge.groupId=:groupID";
  private static final String FIND_GROUP_ID_BY_NAME = "SELECT gi.id FROM  GroupEntity gi WHERE gi.name=:groupname";
  private static final String FIND_USER_DETAILS_WITH_ROLES = """
          SELECT
              u.ID,u.USERNAME,u.EMAIL,u.FIRST_NAME ,u.LAST_NAME,
              STRING_AGG(kr.NAME, ', ' ORDER BY kr.NAME) AS roles
          FROM
              {h-schema}USER_ENTITY u
          LEFT JOIN
              {h-schema}USER_ROLE_MAPPING urm ON u.ID = urm.USER_ID
          LEFT JOIN
              {h-schema}KEYCLOAK_ROLE kr ON urm.ROLE_ID = kr.ID
          WHERE u.realm_id = :realmName AND u.enabled = true AND u.service_account_client_link is null
          GROUP BY
              u.ID,u.USERNAME,u.EMAIL,u.FIRST_NAME ,u.LAST_NAME
          ORDER BY u.EMAIL""";

  public static final String FIND_USER_PRIVATE_CLIENT_ATTR = """
          SELECT u.email,c.client_id as apikey,ra.name as attribute_name,ra.value as attribute_value
          FROM
          {h-schema}USER_ENTITY u
          INNER JOIN
          {h-schema}USER_ROLE_MAPPING urm ON u.ID = urm.USER_ID
          INNER JOIN
          {h-schema}KEYCLOAK_ROLE kr ON urm.ROLE_ID = kr.ID
          INNER JOIN 
          {h-schema}CLIENT c ON kr.client = c.id
          INNER JOIN
          {h-schema}ROLE_ATTRIBUTE ra ON kr.id=ra.role_id                        
          WHERE u.realm_id = :realmName AND u.enabled = true AND u.service_account_client_link IS NULL
          AND kr.name = 'client_owner'
          AND c.realm_id = :realmName 
          AND c.enabled = true
          AND ra.name IN('rateLimitReached','lastAccess')
          """;
  private static final String FIND_PROJECT_CLIENT_ATTR = """
          SELECT c.client_id as apikey,ra.name as attribute_name,ra.value as attribute_value
          FROM
          {h-schema}CLIENT c
          JOIN
          {h-schema}KEYCLOAK_ROLE kr ON kr.client = c.id
          JOIN
          {h-schema}ROLE_ATTRIBUTE ra ON kr.id=ra.role_id
          WHERE kr.name in ('shared_owner')
          AND  c.realm_id = :realmName 
          """;


  private final EntityManager em;
  public CustomQueryRepository(EntityManager em) {
    this.em = em;
  }

  /**
   * Fetch the ID of the test group -'Europeana Test Users'
   * @return groupID from keycloak table 'KEYCLOAK_GROUP' or null if no record found
   */
  public String findTestGroupId() {
    try {
      return em.createQuery(FIND_GROUP_ID_BY_NAME, String.class).setParameter("groupname", "Europeana Test Users").getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public  List<String> findTestGroupUsers(String groupId){
    return em.createQuery(FIND_USER_IDS_BY_GROUP_ID,String.class).setParameter("groupID", groupId).getResultList();
  }

  public Map<String, KeycloakUser> listAllUserMails(String realmName) {

    Map<String,KeycloakUser> userDetailsMap = new HashMap<>();
    Query nativeQuery = em.createNativeQuery(FIND_USER_DETAILS_WITH_ROLES).setParameter("realmName" ,realmName);

    List<Object[]> rows = nativeQuery.getResultList();

    Map<String, KeycloakClient> privateKeys = getUserPrivateClient(realmName);

    for(Object[] row : rows){

      String id = (String) row[0];
      String username = (String) row[1];
      String email = (String) row[2];
      String firstName = (String) row[3];
      String lastName = (String) row[4];
      String roles = (String) row[5];

      KeycloakClient privateKey = privateKeys.get(email);
      String lastAccess = privateKey!=null ? privateKey.getLastAccessDate():null;
      String rateLimitReached = privateKey!=null ? privateKey.getRateLimitReached():null;

      userDetailsMap.put(email.toLowerCase(),
              new KeycloakUser(id,username,email,firstName,lastName,roles,lastAccess,rateLimitReached));
    }
    return userDetailsMap;
  }

  /**
   * Generate map of user email and 'private' client details.Expectation is there is only one private client for each individual user.
   * The client details contains the apikey and its attributes with value.
   */
  private Map<String, KeycloakClient> getUserPrivateClient(String realmName) {
     Query nativeQuery = em.createNativeQuery(FIND_USER_PRIVATE_CLIENT_ATTR).setParameter("realmName" ,realmName);
    List<Object[]> rows = nativeQuery.getResultList();

    Map<String, KeycloakClient> clientMap = new HashMap<>();
    for (Object[] row : rows) {
      String email = String.valueOf(row[0]);
      String apikey = String.valueOf(row[1]);
      String attributeName = String.valueOf(row[2]);
      String attributeValue = String.valueOf(row[3]);
      //The details are stored against User Email
      populateClientInfoMap(clientMap, email, apikey, attributeName, attributeValue);
    }
    return clientMap;
  }

  private void populateClientInfoMap(Map<String, KeycloakClient> clientMap, String mapKey, String apikey, String attributeName, String attributeValue) {
    KeycloakClient client = clientMap.get(mapKey);

    if (client == null) {
      Map<String,String> attributemap = new HashMap<>();
      if(StringUtils.isNotEmpty(attributeName)) {
        attributemap.put(attributeName, attributeValue);
      }
      clientMap.put(mapKey, new KeycloakClient(apikey,attributemap));
    } else {
      client.addAttribute(attributeName, attributeValue);
    }
  }

  /**
    Fetch the list of clients representing project keys for given realm
    @param realmName name of the realm
    @return map of zoho project ids and respective keycloak clients
   */
  public Map<String, KeycloakClient> getProjectClients(String realmName) {

    Query nativeQuery = em.createNativeQuery(FIND_PROJECT_CLIENT_ATTR).setParameter("realmName" ,realmName);
    List<Object[]> rows = nativeQuery.getResultList();

    Map<String, KeycloakClient> clientMap = new HashMap<>();
    for (Object[] row : rows) {
      String apikey = String.valueOf(row[0]);
      String attributeName = String.valueOf(row[1]);
      String attributeValue = String.valueOf(row[2]);

      populateClientInfoMap(clientMap, apikey, apikey, attributeName, attributeValue);
    }
    return clientMap;
  }
}