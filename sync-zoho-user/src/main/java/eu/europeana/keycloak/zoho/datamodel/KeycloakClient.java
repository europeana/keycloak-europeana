package eu.europeana.keycloak.zoho.datamodel;

import java.util.Map;

public class KeycloakClient {

  public static final String RATE_LIMITREACHED = "rateLimitReached";
  public static final String LAST_ACCESS = "lastAccess";
  String key;

  Map<String,String> attributes;

  public KeycloakClient(String key, Map<String,String> attributes) {
    this.key = key;
    this.attributes = attributes;
  }

  public String getKey() {
    return key;
  }

  public String getLastAccessDate() {
    return  attributes.get(LAST_ACCESS);
  }

  public void setLastAccessDate(String lastAccessDate) {
    this.attributes.put(LAST_ACCESS,lastAccessDate);
  }

  public String getRateLimitReached() {
   return  attributes.get(RATE_LIMITREACHED);
  }

  public void setRateLimitReached(String rateLimitReached) {
    this.attributes.put(RATE_LIMITREACHED,rateLimitReached);
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  public void addAttribute(String attributeName,String attributeValue) {
    this.attributes.put(attributeName,attributeValue);
  }
}