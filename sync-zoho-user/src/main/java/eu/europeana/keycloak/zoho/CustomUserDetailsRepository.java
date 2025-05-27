package eu.europeana.keycloak.zoho;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.keycloak.models.jpa.entities.UserEntity;

public class CustomUserDetailsRepository {
  private final EntityManager em;
  public CustomUserDetailsRepository(EntityManager em) {
    this.em = em;
  }

  public List<UserEntity> findKeycloakUsers(){
    String query ="SELECT u FROM UserEntity u WHERE u.realmId ='europeana' and u.enabled = true and u.serviceAccountClientLink is null";
    return em.createQuery(query, UserEntity.class).getResultList();
  }

  public List<String> findUserRoles(UserEntity userID){
    String query ="SELECT re.name FROM UserRoleMappingEntity urm  JOIN RoleEntity re ON re.id = urm.roleId WHERE urm.user=:userID";
    return  em.createQuery(query, String.class).setParameter("userID", userID).getResultList();
  }

  public  String findTestGroupId(){
    String query ="SELECT gi.id FROM  GroupEntity gi WHERE gi.name=:groupname";
    return em.createQuery(query,String.class).setParameter("groupname", "Europeana Test Users").getSingleResult();
  }

  public  List<String> findTestGroupUsers(String groupId){
    String query ="SELECT uge.id FROM  UserGroupMembershipEntity uge WHERE uge.groupId=:groupID";
    return em.createQuery(query,String.class).setParameter("groupID", groupId).getResultList();
  }

}