package eu.europeana.keycloak.zoho;

public class KeycloakUser {

  private String id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private String associatedRoleType;

  public KeycloakUser(String id, String username, String email, String firstName, String lastName,
      String associatedRoleType) {
    this.id = id;
    this.username = username;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.associatedRoleType = associatedRoleType;
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

  public String getAssociatedRoleType() {
    return associatedRoleType;
  }

  public void setAssociatedRoleType(String associatedRoleType) {
    this.associatedRoleType = associatedRoleType;
  }
}