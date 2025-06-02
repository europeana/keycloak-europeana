package eu.europeana.keycloak.registration.datamodel;

/**
 * Represents the structure for the API registration request
 */
public class RegistrationInput {
  private String email;
  public String getEmail() {
    return email;
  }
  public void setEmail(String email) {
    this.email = email;
  }
  @Override
  public String toString() {
    return "RegistrationInput{" +
        "email='" + email + '\'' +
        '}';
  }
}