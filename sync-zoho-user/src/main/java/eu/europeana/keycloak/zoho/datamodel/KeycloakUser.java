package eu.europeana.keycloak.zoho.datamodel;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KeycloakUser {

  private String id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private String associatedRoles;
  private String lastAccess;
  private String rateLimitReached;

  private String personalKey;

  public KeycloakUser(String id, String username, String email, String firstName, String lastName,
      String associatedRoles,String lastAccess,String rateLimitReached,String personalKey) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.associatedRoles = associatedRoles;
    this.lastAccess = lastAccess;
    this.rateLimitReached =rateLimitReached;
    this.personalKey = personalKey;
  }

  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getAssociatedRoles() {
    return associatedRoles;
  }

  public void setAssociatedRoles(String associatedRoles) {
    this.associatedRoles = associatedRoles;
  }

  public  List<String> getAssociatedRoleList(){
    if(this.associatedRoles !=null){
     return  Arrays.asList(getAssociatedRoles().split(","));
    }
    return Collections.emptyList();
  }

  public String getLastAccess() {
    return lastAccess;
  }

  public void setLastAccess(String lastAccess) {
    this.lastAccess = lastAccess;
  }

  public String getRateLimitReached() {
    return rateLimitReached;
  }

  public void setRateLimitReached(String rateLimitReached) {
    this.rateLimitReached = rateLimitReached;
  }

  public String getPersonalKey() {
    return personalKey;
  }

  public void setPersonalKey(String personalKey) {
    this.personalKey = personalKey;
  }
}