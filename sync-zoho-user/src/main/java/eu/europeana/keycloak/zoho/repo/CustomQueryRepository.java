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

public class CustomQueryRepository {
  private final EntityManager em;
  public CustomQueryRepository(EntityManager em) {
    this.em = em;
  }

  /**
   * Fetch the ID of the test group -'Europeana Test Users'
   * @return groupID from keycloak table 'KEYCLOAK_GROUP' or null if no record found
   */
  public  String findTestGroupId(){
    String query ="SELECT gi.id FROM  GroupEntity gi WHERE gi.name=:groupname";
    try {
      return em.createQuery(query, String.class)
          .setParameter("groupname", "Europeana Test Users").getSingleResult();
    }catch (NoResultException e){
      return null;
    }
  }

  public  List<String> findTestGroupUsers(String groupId){
    String query ="SELECT uge.user.id FROM  UserGroupMembershipEntity uge WHERE uge.groupId=:groupID";
    return em.createQuery(query,String.class).setParameter("groupID", groupId).getResultList();
  }

  public Map<String, KeycloakUser> listAllUserMails(String realmName) {

    Map<String,KeycloakUser> userDetailsMap = new HashMap<>();
    String nativeQueryString = """
                        SELECT
                            u.ID,u.USERNAME,u.EMAIL,u.FIRST_NAME ,u.LAST_NAME,
                            STRING_AGG(kr.NAME, ', ' ORDER BY kr.NAME) AS roles
                        FROM
                            {h-schema}USER_ENTITY u
                        LEFT JOIN
                            {h-schema}USER_ROLE_MAPPING urm ON u.ID = urm.USER_ID
                        LEFT JOIN
                            {h-schema}KEYCLOAK_ROLE kr ON urm.ROLE_ID = kr.ID
                        WHERE u.realm_id = %s AND u.enabled = true AND u.service_account_client_link is null
                        GROUP BY
                            u.ID,u.USERNAME,u.EMAIL,u.FIRST_NAME ,u.LAST_NAME
                        ORDER BY u.EMAIL""".formatted("'"+realmName+"'");

    Query nativeQuery = em.createNativeQuery(nativeQueryString);

    List<Object[]> rows = nativeQuery.getResultList();

    for(Object[] row : rows){

      String id = (String) row[0];
      String username = (String) row[1];
      String email = (String) row[2];
      String firstName = (String) row[3];
      String lastName = (String) row[4];
      String roles = (String) row[5];
      userDetailsMap.put(email,new KeycloakUser(id,username,email,firstName,lastName,roles));
    }
    return userDetailsMap;
  }

  /**
    Fetch the list of clients representing project keys for given realm
    @param realmName name of the realm
    @return map of zoho project ids and respective keycloak clients
   */
  public Map<String, KeycloakClient> getProjectClients(String realmName) {

    String nativeQueryString = """
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

    Query nativeQuery = em.createNativeQuery(nativeQueryString).setParameter("realmName" ,realmName);
    List<Object[]> rows = nativeQuery.getResultList();

    Map<String, KeycloakClient> clientMap = new HashMap<>();
    for (Object[] row : rows) {
      String apikey = String.valueOf(row[0]);
      String attributeName = String.valueOf(row[1]);
      String attributeValue = String.valueOf(row[2]);

      KeycloakClient client = clientMap.get(apikey);
      if (client == null) {
        Map<String,String> attributemap = new HashMap<>();
        if(StringUtils.isNotEmpty(attributeName)) {
          attributemap.put(attributeName, attributeValue);
        }
        clientMap.put(apikey, new KeycloakClient(apikey,attributemap));
      } else {
        client.addAttribute(attributeName, attributeValue);
      }
    }
    return clientMap;
  }
}