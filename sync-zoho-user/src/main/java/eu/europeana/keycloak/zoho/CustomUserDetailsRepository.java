package eu.europeana.keycloak.zoho;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomUserDetailsRepository {
  private final EntityManager em;
  public CustomUserDetailsRepository(EntityManager em) {
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

  public Map<String,KeycloakUser> listAllUserMails(String realmName) {

    Map<String,KeycloakUser> userDetailsMap = new HashMap<>();
    String nativeQueryString = """
                        SELECT
                            u.ID,u.USERNAME,u.EMAIL,u.FIRST_NAME ,u.LAST_NAME,
                            STRING_AGG(kr.NAME, ', ' ORDER BY kr.NAME) AS roles
                        FROM
                            USER_ENTITY u
                        LEFT JOIN
                            USER_ROLE_MAPPING urm ON u.ID = urm.USER_ID
                        LEFT JOIN
                            KEYCLOAK_ROLE kr ON urm.ROLE_ID = kr.ID
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
}