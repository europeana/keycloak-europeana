package eu.europeana.api.common.zoho;


/**
 * @author Luthien Dulk
 * Created on 12 feb 2024
 */
public class ZohoAccessConfiguration {

  // the configuration is set in the build job (Jenkins), eg echo "ZOHO_USER_NAME=Pietje_Puk" >>.env



  static final String ZOHO_USER_NAME = System.getenv("ZOHO_USER_NAME");
  static final String ZOHO_CLIENT_ID = System.getenv("ZOHO_CLIENT_ID");
  static final String ZOHO_CLIENT_SECRET = System.getenv("ZOHO_CLIENT_SECRET");
  static final String ZOHO_REFRESH_TOKEN = System.getenv("ZOHO_REFRESH_TOKEN");
  static final String ZOHO_REDIRECT_URL = System.getenv("ZOHO_REDIRECT_URL");
  static final String ZOHO_EU_GRANT_CODE = System.getenv("ZOHO_EU_GRANT_CODE");



/*
  private String zohoUserName;
  private String zohoClientId;
  private String zohoClientSecret;
  private String zohoRefreshToken;
  private String zohoRedirectUrl;

  public ZohoAccessConfiguration(){
    if (null != System.getenv("ZOHO_USER_NAME")){
      this.zohoUserName = System.getenv("ZOHO_USER_NAME");
    }
    if (null != System.getenv("ZOHO_CLIENT_ID")){
      this.zohoUserName = System.getenv("ZOHO_CLIENT_ID");
    }
    if (null != System.getenv("ZOHO_CLIENT_SECRET")){
      this.zohoUserName = System.getenv("ZOHO_CLIENT_SECRET");
    }
    if (null != System.getenv("ZOHO_REFRESH_TOKEN")){
      this.zohoUserName = System.getenv("ZOHO_REFRESH_TOKEN");
    }
    if (null != System.getenv("ZOHO_REDIRECT_URL")){
      this.zohoUserName = System.getenv("ZOHO_REDIRECT_URL");
    }

  }

  public String getZohoUserName() {
    return zohoUserName;
  }

  public String getZohoClientId() {
    return zohoClientId;
  }

  public String getZohoClientSecret() {
    return zohoClientSecret;
  }

  public String getZohoRefreshToken() {
    return zohoRefreshToken;
  }

  public String getZohoRedirectUrl() {
    return zohoRedirectUrl;
  }

 */

}
