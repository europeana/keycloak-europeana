package eu.europeana.keycloak.zoho.datamodel;

public class Institute {
    private String accountName;
    private String europeanaOrgID;
    public Institute(String aName, String eID) {
        accountName    = aName;
        europeanaOrgID = eID;
    }
    public String getAccountName() {
        return accountName;
    }
    public String getEuropeanaOrgID() {
        return europeanaOrgID;
    }
}