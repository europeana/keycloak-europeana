package eu.europeana.keycloak.registration.exception;
public class ClientCreationException extends RuntimeException{
 public ClientCreationException(String message , Throwable tx){
   super(message,tx);
 }

}
