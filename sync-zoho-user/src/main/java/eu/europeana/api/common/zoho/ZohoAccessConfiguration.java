package eu.europeana.api.common.zoho;


/**
 * @author Luthien Dulk
 * Created on 12 feb 2024
 */
public class ZohoAccessConfiguration {

  private ZohoAccessConfiguration(){}

  static final String ZOHO_USER_NAME = System.getenv("ZOHO_USER_NAME");
  static final String ZOHO_CLIENT_ID = System.getenv("ZOHO_CLIENT_ID");
  static final String ZOHO_CLIENT_SECRET = System.getenv("ZOHO_CLIENT_SECRET");
  static final String ZOHO_REFRESH_TOKEN = System.getenv("ZOHO_REFRESH_TOKEN");
  static final String ZOHO_REDIRECT_URL = System.getenv("ZOHO_REDIRECT_URL");
}