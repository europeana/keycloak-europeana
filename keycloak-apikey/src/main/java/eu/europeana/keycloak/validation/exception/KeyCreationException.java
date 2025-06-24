package eu.europeana.keycloak.validation.exception;

/**
 * Custom Exception to be thrown during apikey creation
 */
public class KeyCreationException extends RuntimeException {
  public KeyCreationException(String message , Throwable tx){
    super(message,tx);
  }
}