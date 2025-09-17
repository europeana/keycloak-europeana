package eu.europeana.keycloak;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

public class KeycloakUtils {
  private static final Logger LOG  = Logger.getLogger(KeycloakUtils.class);
  public static int getEnvInt(String envVar, int defaultValue) {
    String evVarValue = System.getenv(envVar);
    if(StringUtils.isBlank(evVarValue)){
      LOG.error("Environment variable "+envVar+ " is not configured. Using default value "+ defaultValue);
      return defaultValue;
    }
    try{
      return Integer.parseInt(evVarValue);
    }catch (NumberFormatException ex){
      LOG.error("Invalid number format for environment variable "+envVar+"="+evVarValue+" Using default value "+ defaultValue);
      return defaultValue;
    }
  }

}