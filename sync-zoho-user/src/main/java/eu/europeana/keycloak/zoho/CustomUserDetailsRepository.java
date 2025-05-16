package eu.europeana.keycloak.zoho;

import jakarta.persistence.EntityManager;
import java.util.List;

public class CustomUserDetailsRepository {
  private final EntityManager em;
  public CustomUserDetailsRepository(EntityManager em) {
    this.em = em;
  }

  public List<KeycloakUser> findKeycloakUsers(){
    String query ="SELECT  u.id,u.username,u.email,u.firstName ,u.lastName, GROUP_CONCAT(kr.NAME ORDER BY kr.NAME SEPARATOR ', ') AS roles FROM UserEntity u  JOIN UserRoleMappingEntity urm JOIN RoleEntity kr "
        + "WHERE u.realmId ='europeana' and u.enabled = true "
        + "GROUP BY u.id,u.username";
    return em.createQuery(query,KeycloakUser.class).getResultList();
  }
}