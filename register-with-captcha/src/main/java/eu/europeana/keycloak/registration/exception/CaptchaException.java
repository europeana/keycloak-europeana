package eu.europeana.keycloak.registration.exception;

public class CaptchaException extends RuntimeException{
  public CaptchaException (String message){
    super(message);
  }
}