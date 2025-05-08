package eu.europeana.keycloak.zoho;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CustomUserDetailsRepository {
  private final EntityManager em;
  public CustomUserDetailsRepository(EntityManager em) {
    this.em = em;
  }
  public List<String> findModifiedKeycloakUsers(OffsetDateTime toThistimeAgo){
    return null;
  }
  public List<String> findKeycloakUsers(OffsetDateTime toThistimeAgo){
    String dateString = toThistimeAgo.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    String query ="";
    return null;
  }
}
