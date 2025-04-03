package eu.europeana.keycloak.transaction;

import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.RealmModel;

public class ClientTransaction extends AbstractKeycloakTransaction {

  private final RealmModel realm;
  private final ClientProvider clientProvider;

  private final ClientModel clientModel;


  public ClientTransaction(RealmModel realm, ClientProvider clients, ClientModel client) {
    this.realm =realm;
    this.clientProvider=clients;
    this.clientModel=client;
  }

  @Override
  protected void commitImpl() {
    clientProvider.removeClient(realm,clientModel.getClientId());
  }

  @Override
  protected void rollbackImpl() {

  }
}
