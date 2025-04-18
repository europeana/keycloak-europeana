package eu.europeana.keycloak.validation.exception;

public class KeyCreationException extends RuntimeException {
  public KeyCreationException(String message , Throwable tx){
    super(message,tx);
  }
}
