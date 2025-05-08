package eu.europeana.keycloak.zoho;

import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;

public class KeycloakToZohoSyncService {

  private final KeycloakSession session;

  public KeycloakToZohoSyncService(KeycloakSession session) {
    this.session = session;
  }

  public Map<String, String> handleZohoUpdate(List<Contact> contacts, OffsetDateTime toThisTimeAgo) {

    EntityManager entityManager = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    CustomUserDetailsRepository repo = new CustomUserDetailsRepository(entityManager);
    //Get modified keycloak users -
    // for each keycloak user check if user or its keys are created after the input day
    // if yes then add it in modified user map

    //Separated zoho contacts
    Map<String,Contact> zohoContactsByPrimaryMail = new HashMap<>();
    Map<String,Contact> zohoContactsBySecondaryMail = new HashMap<>();


    for(Contact c : contacts ){
      if(StringUtils.isNotEmpty(c.getEmail())) {
        zohoContactsByPrimaryMail.put(c.getEmail(), c);
      }
      if(StringUtils.isNotEmpty(c.getSecondaryEmail())) {
        zohoContactsBySecondaryMail.put(c.getSecondaryEmail(), c);
      }
    }

    //keyCloak Users
    repo.findKeycloakUsers(toThisTimeAgo);
    repo.findModifiedKeycloakUsers(toThisTimeAgo);


    Map<String,KeycloakUser> userToHandle =  new  HashMap<>();

    //logic to update the zoho contact
    //for each keycloack user check if the user is present in zoho or not , if not present then create the user else update the details in zoho
     for(Map.Entry<String,KeycloakUser> entry: userToHandle.entrySet()) {
       if (zohoContactsByPrimaryMail.containsKey(entry.getKey())){
         //that means keycloak contact already present in zoho we need to check and update it

       }

     }


    return null;
  }
}
