package eu.europeana.keycloak.metrics;

import jakarta.persistence.EntityManager;

public class CustomClientRepository {
    private final EntityManager em;

    public CustomClientRepository(EntityManager em) {
        this.em = em;
    }

    public Long findKeyByRoleName(String roleName) {
        String query = "SELECT count(c.id) FROM ClientEntity c ,RoleEntity kr where kr.clientId = c.id and kr.name = :roleNameVal";
        return em.createQuery(query, Long.class)
                .setParameter("roleNameVal", roleName).getSingleResult();
    }
}
