package eu.europeana.keycloak.repo;

import jakarta.persistence.EntityManager;

import java.util.List;

import static eu.europeana.keycloak.utils.Constants.SHARED_OWNER;
import static eu.europeana.keycloak.utils.Constants.ROLE_ATTRIBUTE_SCOPE_INTERNAL;
import static eu.europeana.keycloak.utils.Constants.ROLE_ATTRIBUTE_SCOPE;

/** Contains methods to interact directly with the Keycloak DB via JPA
 * to retrieve specific client-related information
 */
public class CustomClientRepository {
    private final EntityManager em;

    /** Constructs the {@code CustomClientRepository}  using the provided EntityManager.
     * @param em The {@code EntityManager} instance to be used for database interactions.
     */
    public CustomClientRepository(EntityManager em) {
        this.em = em;
    }

    /** Gets the count of keycloak clients having specific role.
     * @param roleName role to search
     * @return Count of keycloak clients
     **/
    public Long findKeyByRoleName(String roleName) {
        String query = "SELECT count(c.id) FROM ClientEntity c ,RoleEntity kr where kr.clientId = c.id and kr.name = :roleNameVal";
        return em.createQuery(query, Long.class)
                .setParameter("roleNameVal", roleName).getSingleResult();
    }

    /**
     * Get the client count of keycloak having specific role and attribute (name and value)
     * @param roleName role to search
     * @param attributeName attribute pair name
     * @param attributeValue attribute pair value
     * @return
     */
    public Long findKeyByRoleNameAndAttributePair(String roleName, String attributeName, String attributeValue) {
        String query = "SELECT count(c.id) FROM  ClientEntity c , RoleEntity kr, RoleAttributeEntity rae where kr.clientId = c.id and kr.name =:roleNameVal " +
                "and rae.role.name = kr.name and rae.role.id = kr.id and rae.name = :name and rae.value= :value" ;
        return em.createQuery(query, Long.class)
                .setParameter("roleNameVal", roleName)
                .setParameter("name", attributeName)
                .setParameter("value", attributeValue).getSingleResult();

    }

    /**
     * Get the Internal keys
     * @return list of internal keys
     */
    public List<String> getInternalKeys(){
        String query = "SELECT c.clientId FROM  ClientEntity c , RoleEntity kr, RoleAttributeEntity rae where kr.clientId = c.id and kr.name =:roleNameVal " +
                "and rae.role.name = kr.name and rae.role.id = kr.id and rae.name = :name and rae.value= :value" ;
        return em.createQuery(query, String.class)
                .setParameter("roleNameVal", SHARED_OWNER)
                .setParameter("name", ROLE_ATTRIBUTE_SCOPE)
                .setParameter("value", ROLE_ATTRIBUTE_SCOPE_INTERNAL).getResultList();
    }

    /**
     * Get the project keys
     * @return list of project keys
     */
    public List<String> getProjectKeys(){
        String query = "SELECT c.clientId FROM  ClientEntity c , RoleEntity kr where kr.clientId = c.id and kr.name =:roleNameVal";
        return em.createQuery(query, String.class)
                .setParameter("roleNameVal", SHARED_OWNER).getResultList();
    }
}