package eu.europeana.keycloak;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;

/**
 * Utility class to define commonly used operations.
 */
public final class KeycloakUtils {
  private static final Logger LOG  = Logger.getLogger(KeycloakUtils.class);

  private KeycloakUtils() {}

  /**
   * Gets the integer value of the provided environment variable.Returns the default in case the value is not present.
   * @param envVar Name of environment variable
   * @param defaultValue default value to be returned
   * @return int value
   */
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