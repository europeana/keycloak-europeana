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
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.query.sql.internal.NativeQueryImpl;
import org.jboss.logging.Logger;

public class CustomQueryRepository {

  private static final Logger LOG = Logger.getLogger(CustomQueryRepository.class);
  private final EntityManager em;
  public CustomQueryRepository(EntityManager em) {
    this.em = em;
  }

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

    if(((NativeQueryImpl<?>) nativeQuery).getSession() instanceof SessionImpl session){
      if(session.getSessionFactory() instanceof SessionFactoryImpl factory){
        LOG.info(" DB Schema name : "+factory.getProperties().get("hibernate.default_schema"));
      }
    }

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

  public Map<String, KeycloakClient> getAllClients(String realmName) {

    String nativeQueryString = """
        SELECT c.client_id as apikey,kr.name as role_name,ra.name as attribute_name,ra.value as attribute_value
        FROM
        {h-schema}CLIENT c
        JOIN
        {h-schema}KEYCLOAK_ROLE kr ON kr.client = c.id
        JOIN
        {h-schema}ROLE_ATTRIBUTE ra ON kr.id=ra.role_id
        WHERE kr.name in ('client_owner','shared_owner')
        AND  c.realm_id = %s      
        """.formatted("'" + realmName + "'");

    Query nativeQuery = em.createNativeQuery(nativeQueryString);
    List<Object[]> rows = nativeQuery.getResultList();

    Map<String, KeycloakClient> clientMap = new HashMap<>();
    for (Object[] row : rows) {
      String apikey = (String) row[0];
      String role_name = (String) row[1];
      String attribute_name = (String) row[2];
      String attribute_value = (String) row[3];

      KeycloakClient client = clientMap.get(apikey);
      if (client == null) {
        Map<String,String> attributemap = new HashMap<>();
        if(StringUtils.isNotEmpty(attribute_name)) {
          attributemap.put(attribute_name, attribute_value);
        }
        clientMap.put(apikey, new KeycloakClient(apikey, role_name,attributemap));
      } else {
        client.addAttribute(attribute_name, attribute_value);
      }
    }
    return clientMap;
  }
}