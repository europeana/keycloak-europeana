package eu.europeana.keycloak.metrics;

import jakarta.persistence.EntityManager;
import org.keycloak.models.jpa.entities.RoleAttributeEntity;

import javax.management.relation.Role;
import java.util.stream.Stream;

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

    public Long findKeyByRoleName1(String roleName, String attributeName, String attributeValue) {
        RoleAttributeEntity roleAttribute = new RoleAttributeEntity();
        roleAttribute.setName(attributeName);
        roleAttribute.setValue(attributeValue);
        String query =
                "SELECT count(c.id) FROM ClientEntity c, RoleEntity kr, RoleAttributeEntity rae where kr.clientId = c.id and kr.name = :roleNameVal " +
                        "and rae.id = kr.id and rae.role = :roleNameVal " +
                        "and kr.attributes in :attribute" ;
        return em.createQuery(query, Long.class)
                .setParameter("roleNameVal", roleName)
                .setParameter("attribute", roleAttribute).getSingleResult();

    }
}